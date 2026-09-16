package dev.codetrail.desktop.simulation.range;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.List;

/** Segment tree with real range-add lazy tags and push-down query/update traces. */
public final class LazySegmentTreeEngine implements SimulationEngine {
    public static final String TYPE = "SEGMENT_TREE_LAZY";
    public static final int MIN_VALUES = RangeInput.MIN_VALUES;
    public static final int MAX_VALUES = RangeInput.MAX_VALUES;
    public static final int MAX_OPERATIONS = RangeInput.MAX_OPERATIONS;
    public static final int MAX_ABS_VALUE = RangeInput.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = RangeTreeSupport.MAX_TRACE_STEPS;

    private static final int LINE_BUILD = 1;
    private static final int LINE_BUILD_LEAF = 2;
    private static final int LINE_BUILD_MIDDLE = 3;
    private static final int LINE_BUILD_CHILDREN = 4;
    private static final int LINE_BUILD_COMBINE = 5;
    private static final int LINE_ADD = 6;
    private static final int LINE_ADD_APPLY = 7;
    private static final int LINE_ADD_PUSH = 8;
    private static final int LINE_ADD_CHILDREN = 9;
    private static final int LINE_ADD_COMBINE = 10;
    private static final int LINE_SUM = 11;
    private static final int LINE_SUM_COVERED = 12;
    private static final int LINE_SUM_PUSH = 13;
    private static final int LINE_SUM_CHILDREN = 14;
    private static final int LINE_SUM_COMBINE = 15;
    private static final int LINE_SUM_RETURN = 16;
    private static final int LINE_OPERATION_RETURN = 17;

    private static final int[] DEFAULT_VALUES = {1, 2, 3, 4, 5};
    private static final List<String> PSEUDOCODE = List.of(
            "build(node, left, right):",
            "    if left == right: tree[node] = values[left]",
            "    mid = floor((left + right) / 2)",
            "    build(left child); build(right child)",
            "    tree[node] = tree[left child] + tree[right child]",
            "rangeAdd(node, left, right, updateLeft, updateRight, delta):",
            "    if covered: tree[node] += delta * (right-left+1); lazy[node] += delta",
            "    push(node): propagate lazy tag to children",
            "    recurse into overlapping children",
            "    tree[node] = tree[left child] + tree[right child]",
            "rangeSum(node, left, right, queryLeft, queryRight):",
            "    if query covers this node: return tree[node]",
            "    push(node): propagate lazy tag before descending",
            "    recurse into overlapping children",
            "    result = leftResult + rightResult",
            "    return result",
            "return each operation answer");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        RangeInput.LazyRequest request = RangeInput.parseLazy(input);
        int[] initialValues = request.values();
        RangeTreeSupport.SegmentTreeModel model = new RangeTreeSupport.SegmentTreeModel(initialValues, false);
        RangeTreeSupport.TreeTraceBuilder trace = new RangeTreeSupport.TreeTraceBuilder(model);
        long[] logicalValues = new long[initialValues.length];
        for (int index = 0; index < initialValues.length; index++) {
            logicalValues[index] = initialValues[index];
        }

        trace.add(
                0,
                "Initialize a lazy segment tree over " + initialValues.length + " value(s)",
                StepEventType.INITIALIZE,
                facts(model, logicalValues, -1, "build", "root", "pending", "pending", SnapshotStatus.ACTIVE));
        build(model, model.root(), logicalValues, trace);

        String lastResult = "pending";
        for (int operationIndex = 0; operationIndex < request.operations().size(); operationIndex++) {
            RangeInput.LazyOperation operation = request.operations().get(operationIndex);
            trace.resetStatuses();
            if (operation.kind().equals("range-add")) {
                UpdateContext context = new UpdateContext(operationIndex, operation.left(), operation.right(), operation.delta());
                rangeAdd(model, model.root(), operation, context, logicalValues, trace);
                lastResult = "updated [" + operation.left() + "," + operation.right() + "] by " + operation.delta();
                markAllDone(model, trace);
                trace.add(
                        LINE_OPERATION_RETURN,
                        "Return after range-add operation " + operationIndex + ": " + lastResult,
                        StepEventType.EXECUTE_LINE,
                        facts(model, logicalValues, operationIndex, operation.kind(), context.currentInterval,
                                context.pathText(), lastResult, SnapshotStatus.DONE));
            } else {
                QueryContext context = new QueryContext(operationIndex, operation.left(), operation.right());
                long answer = rangeSum(model, model.root(), operation, context, logicalValues, trace);
                lastResult = Long.toString(answer);
                markAllDone(model, trace);
                trace.add(
                        LINE_OPERATION_RETURN,
                        "Return range-sum answer " + lastResult + " for operation " + operationIndex,
                        StepEventType.EXECUTE_LINE,
                        facts(model, logicalValues, operationIndex, operation.kind(), context.currentInterval,
                                context.partialText(), lastResult, SnapshotStatus.DONE));
            }
        }
        trace.add(
                0,
                "Complete: lazy segment operations finished; last result = " + lastResult,
                StepEventType.COMPLETE,
                facts(model, logicalValues, request.operations().size() - 1,
                        request.operations().get(request.operations().size() - 1).kind(),
                        "root", lastResult, lastResult, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void build(
            RangeTreeSupport.SegmentTreeModel model,
            int node,
            long[] logicalValues,
            RangeTreeSupport.TreeTraceBuilder trace) {
        trace.activate(node);
        trace.add(
                LINE_BUILD,
                "Build node " + model.nodeId(node) + " for interval [" + model.left(node) + "," + model.right(node) + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, -1, "build", interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        if (model.isLeaf(node)) {
            model.buildLeaf(node);
            trace.add(
                    LINE_BUILD_LEAF,
                    "Store values[" + model.left(node) + "] = " + logicalValues[model.left(node)]
                            + " in leaf " + model.nodeId(node),
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, -1, "build", interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
            trace.done(node);
            return;
        }
        int middle = model.left(node) + (model.right(node) - model.left(node)) / 2;
        trace.add(
                LINE_BUILD_MIDDLE,
                "Split interval [" + model.left(node) + "," + model.right(node) + "] at mid " + middle,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, -1, "build", interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        trace.add(
                LINE_BUILD_CHILDREN,
                "Build child intervals [" + model.left(node) + "," + middle + "] and [" + (middle + 1) + ","
                        + model.right(node) + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, -1, "build", interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        build(model, model.leftChild(node), logicalValues, trace);
        build(model, model.rightChild(node), logicalValues, trace);
        model.recompute(node);
        trace.add(
                LINE_BUILD_COMBINE,
                "Combine child sums into node " + model.nodeId(node) + " = " + model.value(node),
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, -1, "build", interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        trace.done(node);
    }

    private static void rangeAdd(
            RangeTreeSupport.SegmentTreeModel model,
            int node,
            RangeInput.LazyOperation operation,
            UpdateContext context,
            long[] logicalValues,
            RangeTreeSupport.TreeTraceBuilder trace) {
        context.currentInterval = interval(model, node);
        trace.activate(node);
        trace.add(
                LINE_ADD,
                "Visit node " + model.nodeId(node) + " for range-add [" + operation.left() + "," + operation.right()
                        + "] by " + operation.delta(),
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.pathText(), "pending", SnapshotStatus.ACTIVE, operation, node, "pending"));
        if (operation.left() <= model.left(node) && model.right(node) <= operation.right()) {
            model.applyLazy(node, operation.delta());
            for (int index = model.left(node); index <= model.right(node); index++) {
                logicalValues[index] += operation.delta();
            }
            context.path.add(model.nodeId(node));
            trace.add(
                    LINE_ADD_APPLY,
                    "Add " + operation.delta() + " × " + (model.right(node) - model.left(node) + 1)
                            + " to this interval sum; keep lazy=" + model.lazyTag(node) + " for its children",
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.pathText(), "pending", SnapshotStatus.ACTIVE, operation, node, "sum = " + (model.value(node) - operation.delta() * (model.right(node) - model.left(node) + 1L))
                                + " + " + operation.delta() + " × " + (model.right(node) - model.left(node) + 1) + " = " + model.value(node)));
            trace.done(node);
            return;
        }
        long pushed = model.pushLazy(node);
        trace.add(
                LINE_ADD_PUSH,
                pushed == 0L
                        ? "No pending lazy tag at node " + model.nodeId(node)
                        : "Push lazy tag " + pushed + " from node " + model.nodeId(node) + " to its children",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.pathText(), "pending", SnapshotStatus.ACTIVE, operation, node, pushed == 0 ? "No pending tag to push"
                                : "Push " + pushed + " to both children; parent lazy = 0"));
        int middle = model.left(node) + (model.right(node) - model.left(node)) / 2;
        trace.add(
                LINE_ADD_CHILDREN,
                "Descend to children overlapping update range around mid " + middle,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.pathText(), "pending", SnapshotStatus.ACTIVE, operation, node, "pending"));
        if (operation.left() <= middle) {
            rangeAdd(model, model.leftChild(node), operation, context, logicalValues, trace);
        }
        if (operation.right() > middle) {
            rangeAdd(model, model.rightChild(node), operation, context, logicalValues, trace);
        }
        model.recompute(node);
        context.currentInterval = interval(model, node);
        context.path.add(model.nodeId(node));
        trace.add(
                LINE_ADD_COMBINE,
                "Recompute node " + model.nodeId(node) + " after child updates = " + model.value(node),
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.pathText(), "pending", SnapshotStatus.ACTIVE, operation, node, "pending"));
        trace.done(node);
    }

    private static long rangeSum(
            RangeTreeSupport.SegmentTreeModel model,
            int node,
            RangeInput.LazyOperation operation,
            QueryContext context,
            long[] logicalValues,
            RangeTreeSupport.TreeTraceBuilder trace) {
        context.currentInterval = interval(model, node);
        trace.activate(node);
        trace.add(
                LINE_SUM,
                "Visit node " + model.nodeId(node) + " for range-sum [" + operation.left() + "," + operation.right() + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.partialText(), "pending", SnapshotStatus.ACTIVE, operation, node, "pending"));
        if (operation.left() <= model.left(node) && model.right(node) <= operation.right()) {
            long result = model.value(node);
            context.record(model.nodeId(node), result);
            trace.add(
                    LINE_SUM_COVERED,
                    "Query fully covers [" + model.left(node) + "," + model.right(node) + "]; use sum " + result,
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                            context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, operation, node, "pending"));
            trace.add(
                    LINE_SUM_RETURN,
                    "Return covered sum " + result + " from node " + model.nodeId(node),
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                            context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, operation, node, "pending"));
            trace.done(node);
            return result;
        }

        long pushed = model.pushLazy(node);
        trace.add(
                LINE_SUM_PUSH,
                pushed == 0L
                        ? "No pending lazy tag at node " + model.nodeId(node)
                        : "Push lazy tag " + pushed + " before descending from node " + model.nodeId(node),
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.partialText(), "pending", SnapshotStatus.ACTIVE, operation, node, pushed == 0 ? "No pending tag to push"
                                : "Push " + pushed + " to both children; parent lazy = 0"));
        int middle = model.left(node) + (model.right(node) - model.left(node)) / 2;
        trace.add(
                LINE_SUM_CHILDREN,
                "Descend to children overlapping query around mid " + middle,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.partialText(), "pending", SnapshotStatus.ACTIVE, operation, node, "pending"));
        long leftResult = 0L;
        long rightResult = 0L;
        if (operation.left() <= middle) {
            leftResult = rangeSum(model, model.leftChild(node), operation, context, logicalValues, trace);
        }
        if (operation.right() > middle) {
            rightResult = rangeSum(model, model.rightChild(node), operation, context, logicalValues, trace);
        }
        long result = leftResult + rightResult;
        context.currentInterval = interval(model, node);
        trace.add(
                LINE_SUM_COMBINE,
                "Combine child sums " + leftResult + " and " + rightResult + " into " + result,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, operation, node, "pending"));
        trace.add(
                LINE_SUM_RETURN,
                "Return combined sum " + result + " from interval " + context.currentInterval,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operationIndex, operation.kind(), context.currentInterval,
                        context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, operation, node, "pending"));
        trace.done(node);
        return result;
    }

    private static void markAllDone(
            RangeTreeSupport.SegmentTreeModel model,
            RangeTreeSupport.TreeTraceBuilder trace) {
        for (int node : model.nodeOrder()) {
            trace.done(node);
        }
    }

    private static String interval(RangeTreeSupport.SegmentTreeModel model, int node) {
        return "[" + model.left(node) + "," + model.right(node) + "]";
    }

    private static List<Fact> facts(
            RangeTreeSupport.SegmentTreeModel model, long[] values, int operationIndex,
            String operation, String nodeInterval, String partialAnswer, String answer,
            SnapshotStatus status, RangeInput.LazyOperation request, int node, String equation) {
        List<Fact> result = new ArrayList<>(facts(model, values, operationIndex, operation,
                nodeInterval, partialAnswer, answer, status));
        result.add(RangeTreeSupport.fact("query-range", "[" + request.left() + "," + request.right() + "]", status));
        result.add(RangeTreeSupport.fact("teaching-equation", equation, status));
        return List.copyOf(result);
    }

    private static List<Fact> facts(
            RangeTreeSupport.SegmentTreeModel model,
            long[] values,
            int operationIndex,
            String operation,
            String nodeInterval,
            String partialAnswer,
            String answer,
            SnapshotStatus status) {
        int activeNode = model.nodeOrder().stream().filter(n -> interval(model, n).equals(nodeInterval)).findFirst().orElse(model.root());
        return List.of(
                RangeTreeSupport.fact("renderer", "lazy-segment", SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("active-node", model.nodeId(activeNode), status),
                RangeTreeSupport.fact("node-left", Integer.toString(model.left(activeNode)), status),
                RangeTreeSupport.fact("node-right", Integer.toString(model.right(activeNode)), status),
                RangeTreeSupport.fact("node-sum", Long.toString(model.value(activeNode)), status),
                RangeTreeSupport.fact("node-lazy", Long.toString(model.lazyTag(activeNode)), status),
                RangeTreeSupport.fact("operation-index", Integer.toString(operationIndex), SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("values", RangeTreeSupport.formatValues(values), status),
                RangeTreeSupport.fact("node-interval", nodeInterval, status),
                RangeTreeSupport.fact("partial-answer", partialAnswer, status),
                RangeTreeSupport.fact("answer", answer, status),
                RangeTreeSupport.fact("result", answer, status),
                RangeTreeSupport.fact("lazy-tags", RangeTreeSupport.formatLazyTags(model), status),
                RangeTreeSupport.fact("lazy", RangeTreeSupport.formatLazyTags(model), status),
                RangeTreeSupport.fact("tree", RangeTreeSupport.formatTree(model), status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        ArrayNode operations = input.putArray("operations");
        ObjectNode add = operations.addObject();
        add.put("kind", "range-add");
        add.put("left", 0);
        add.put("right", 4);
        add.put("delta", 2);
        ObjectNode sum = operations.addObject();
        sum.put("kind", "range-sum");
        sum.put("left", 1);
        sum.put("right", 3);
        return new SimulationMetadata(
                TYPE,
                "Segment Tree with Lazy Propagation",
                "O(log n) amortized range update/query after O(n) build",
                "O(n)",
                RendererFamily.TREE,
                input,
                "Enter JSON as {\"values\":[1,2,3,4,5],\"operations\":[{\"kind\":\"range-add\",\"left\":0,\"right\":4,\"delta\":2},{\"kind\":\"range-sum\",\"left\":1,\"right\":3}]}; "
                        + "values length must be " + MIN_VALUES + ".." + MAX_VALUES
                        + ", operations are inclusive zero-based range-add or range-sum entries (at most "
                        + MAX_OPERATIONS + "), and values/deltas have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }

    private static final class UpdateContext {
        private final int operationIndex;
        private final int left;
        private final int right;
        private final int delta;
        private final List<String> path = new ArrayList<>();
        private String currentInterval = "pending";

        private UpdateContext(int operationIndex, int left, int right, int delta) {
            this.operationIndex = operationIndex;
            this.left = left;
            this.right = right;
            this.delta = delta;
        }

        private String pathText() {
            return path.isEmpty() ? "pending" : String.join(" -> ", path);
        }
    }

    private static final class QueryContext {
        private final int operationIndex;
        private final int left;
        private final int right;
        private final List<String> partial = new ArrayList<>();
        private String currentInterval = "pending";
        private long answer;
        private boolean hasAnswer;

        private QueryContext(int operationIndex, int left, int right) {
            this.operationIndex = operationIndex;
            this.left = left;
            this.right = right;
        }

        private void record(String nodeId, long value) {
            partial.add(nodeId + "=" + value);
            answer += value;
            hasAnswer = true;
        }

        private String partialText() {
            return partial.isEmpty() ? "pending" : String.join(", ", partial);
        }
    }
}
