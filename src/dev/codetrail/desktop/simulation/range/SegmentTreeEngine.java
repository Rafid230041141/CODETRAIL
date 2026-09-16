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

/** Segment tree build, inclusive range query, and point-set trace producer. */
public final class SegmentTreeEngine implements SimulationEngine {
    public static final String TYPE = "SEGMENT_TREE";
    public static final int MIN_VALUES = RangeInput.MIN_VALUES;
    public static final int MAX_VALUES = RangeInput.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RangeInput.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = RangeTreeSupport.MAX_TRACE_STEPS;

    private static final int LINE_BUILD = 1;
    private static final int LINE_BUILD_LEAF = 2;
    private static final int LINE_BUILD_MIDDLE = 3;
    private static final int LINE_BUILD_CHILDREN = 4;
    private static final int LINE_BUILD_COMBINE = 5;
    private static final int LINE_QUERY = 6;
    private static final int LINE_QUERY_COVERED = 7;
    private static final int LINE_QUERY_CHILDREN = 8;
    private static final int LINE_QUERY_COMBINE = 9;
    private static final int LINE_QUERY_RETURN = 10;
    private static final int LINE_POINT_SET = 11;
    private static final int LINE_POINT_SET_LEAF = 12;
    private static final int LINE_POINT_SET_CHILD = 13;
    private static final int LINE_POINT_SET_COMBINE = 14;
    private static final int LINE_RETURN = 15;

    private static final int[] DEFAULT_VALUES = {2, 1, 3, 5, 4};
    private static final List<String> PSEUDOCODE = List.of(
            "build(node, left, right):",
            "    if left == right: tree[node] = values[left]",
            "    mid = floor((left + right) / 2)",
            "    build(left child); build(right child)",
            "    tree[node] = combine(tree[left child], tree[right child])",
            "rangeQuery(node, left, right, queryLeft, queryRight):",
            "    if query covers this node: return tree[node]",
            "    mid = floor((left + right) / 2); visit overlapping children",
            "    result = combine(leftResult, rightResult)",
            "    return result",
            "pointSet(node, left, right, index, value):",
            "    if left == right: tree[node] = value",
            "    recurse into the child containing index",
            "    tree[node] = combine(tree[left child], tree[right child])",
            "return the answer or updated tree");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        RangeInput.SegmentRequest request = RangeInput.parseSegment(input);
        int[] initialValues = request.values();
        boolean minimum = request.operation().equals("range-min");
        RangeTreeSupport.SegmentTreeModel model = new RangeTreeSupport.SegmentTreeModel(initialValues, minimum);
        RangeTreeSupport.TreeTraceBuilder trace = new RangeTreeSupport.TreeTraceBuilder(model);
        long[] logicalValues = new long[initialValues.length];
        for (int index = 0; index < initialValues.length; index++) {
            logicalValues[index] = initialValues[index];
        }

        trace.add(
                0,
                "Initialize a segment tree for " + request.operation() + " over " + initialValues.length + " value(s)",
                StepEventType.INITIALIZE,
                facts(model, logicalValues, request.operation(), "root", "pending", "pending", SnapshotStatus.ACTIVE));
        build(model, model.root(), logicalValues, request.operation(), trace);

        trace.resetStatuses();
        String result;
        if (request.isQuery()) {
            QueryContext context = new QueryContext(request.operation(), request.left(), request.right());
            query(
                    model,
                    model.root(),
                    request.left(),
                    request.right(),
                    context,
                    logicalValues,
                    trace);
            result = Long.toString(context.answer);
            markAllDone(model, trace);
            trace.add(
                    LINE_RETURN,
                    "Return " + request.operation() + " answer " + result + " for ["
                            + request.left() + "," + request.right() + "]",
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, request.operation(),
                            context.currentInterval, context.partialText(), result, SnapshotStatus.DONE));
        } else {
            PointSetContext context = new PointSetContext(request.index(), request.value());
            pointSet(model, model.root(), context, logicalValues, trace);
            result = Long.toString(request.value());
            markAllDone(model, trace);
            trace.add(
                    LINE_RETURN,
                    "Return updated tree after setting index " + request.index() + " to " + result,
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, request.operation(),
                            context.currentInterval, context.pathText(), result, SnapshotStatus.DONE));
        }
        trace.add(
                0,
                "Complete: segment tree " + request.operation() + " result = " + result,
                StepEventType.COMPLETE,
                facts(model, logicalValues, request.operation(), "root", result, result, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void build(
            RangeTreeSupport.SegmentTreeModel model,
            int node,
            long[] logicalValues,
            String operation,
            RangeTreeSupport.TreeTraceBuilder trace) {
        trace.activate(node);
        trace.add(
                LINE_BUILD,
                "Build node " + model.nodeId(node) + " for interval [" + model.left(node) + "," + model.right(node) + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, operation, interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        if (model.isLeaf(node)) {
            model.buildLeaf(node);
            trace.add(
                    LINE_BUILD_LEAF,
                    "Store values[" + model.left(node) + "] = " + logicalValues[model.left(node)]
                            + " in leaf " + model.nodeId(node),
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, operation, interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
            trace.done(node);
            return;
        }

        int middle = model.left(node) + (model.right(node) - model.left(node)) / 2;
        trace.add(
                LINE_BUILD_MIDDLE,
                "Split interval [" + model.left(node) + "," + model.right(node) + "] at mid " + middle,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, operation, interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        trace.add(
                LINE_BUILD_CHILDREN,
                "Build child intervals [" + model.left(node) + "," + middle + "] and [" + (middle + 1) + ","
                        + model.right(node) + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, operation, interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        build(model, model.leftChild(node), logicalValues, operation, trace);
        build(model, model.rightChild(node), logicalValues, operation, trace);
        model.recompute(node);
        trace.add(
                LINE_BUILD_COMBINE,
                "Combine child aggregates into node " + model.nodeId(node) + " = " + model.value(node),
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, operation, interval(model, node), "pending", "pending", SnapshotStatus.ACTIVE));
        trace.done(node);
    }

    private static long query(
            RangeTreeSupport.SegmentTreeModel model,
            int node,
            int queryLeft,
            int queryRight,
            QueryContext context,
            long[] logicalValues,
            RangeTreeSupport.TreeTraceBuilder trace) {
        context.currentInterval = interval(model, node);
        trace.activate(node);
        trace.add(
                LINE_QUERY,
                "Visit node " + model.nodeId(node) + " for query [" + queryLeft + "," + queryRight + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operation, context.currentInterval,
                        context.partialText(), "pending", SnapshotStatus.ACTIVE, queryLeft, queryRight, "pending"));
        if (queryLeft <= model.left(node) && model.right(node) <= queryRight) {
            trace.add(
                    LINE_QUERY_COVERED,
                    "Query fully covers [" + model.left(node) + "," + model.right(node) + "]; use aggregate "
                            + model.value(node),
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, context.operation, context.currentInterval,
                            context.partialText(), "pending", SnapshotStatus.ACTIVE, queryLeft, queryRight, "pending"));
            long result = model.value(node);
            context.recordCovered(model.nodeId(node), result, model.minimum());
            trace.add(
                    LINE_QUERY_RETURN,
                    "Return aggregate " + result + " from node " + model.nodeId(node),
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, context.operation, context.currentInterval,
                            context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, queryLeft, queryRight, "pending"));
            trace.done(node);
            return result;
        }

        int middle = model.left(node) + (model.right(node) - model.left(node)) / 2;
        trace.add(
                LINE_QUERY_CHILDREN,
                "Descend to overlapping children around mid " + middle,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operation, context.currentInterval,
                        context.partialText(), "pending", SnapshotStatus.ACTIVE, queryLeft, queryRight, "pending"));
        long leftResult = model.minimum() ? Long.MAX_VALUE : 0L;
        long rightResult = model.minimum() ? Long.MAX_VALUE : 0L;
        boolean hasLeft = queryLeft <= middle;
        boolean hasRight = queryRight > middle;
        if (hasLeft) {
            leftResult = query(model, model.leftChild(node), queryLeft, queryRight, context, logicalValues, trace);
        }
        if (hasRight) {
            rightResult = query(model, model.rightChild(node), queryLeft, queryRight, context, logicalValues, trace);
        }
        long result = model.minimum()
                ? Math.min(leftResult, rightResult)
                : leftResult + rightResult;
        context.currentInterval = interval(model, node);
        trace.add(
                LINE_QUERY_COMBINE,
                "Combine child answers " + displayAggregate(leftResult) + " and " + displayAggregate(rightResult) + " into " + result,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operation, context.currentInterval,
                        context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, queryLeft, queryRight, model.minimum()
                                ? "min(" + displayAggregate(leftResult) + ", " + displayAggregate(rightResult) + ") = " + result
                                : leftResult + " + " + rightResult + " = " + result));
        trace.add(
                LINE_QUERY_RETURN,
                "Return combined answer " + result + " from [" + model.left(node) + "," + model.right(node) + "]",
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, context.operation, context.currentInterval,
                        context.partialText(), Long.toString(context.answer), SnapshotStatus.ACTIVE, queryLeft, queryRight, "pending"));
        trace.done(node);
        return result;
    }

    private static void pointSet(
            RangeTreeSupport.SegmentTreeModel model,
            int node,
            PointSetContext context,
            long[] logicalValues,
            RangeTreeSupport.TreeTraceBuilder trace) {
        context.currentInterval = interval(model, node);
        trace.activate(node);
        trace.add(
                LINE_POINT_SET,
                "Visit node " + model.nodeId(node) + " while setting index " + context.index,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, "point-set", context.currentInterval,
                        context.pathText(), Long.toString(context.value), SnapshotStatus.ACTIVE));
        if (model.isLeaf(node)) {
            model.setLeaf(node, context.value);
            logicalValues[context.index] = context.value;
            context.path.add(model.nodeId(node));
            trace.add(
                    LINE_POINT_SET_LEAF,
                    "Write value " + context.value + " into leaf index " + context.index,
                    StepEventType.EXECUTE_LINE,
                    facts(model, logicalValues, "point-set", context.currentInterval,
                            context.pathText(), Long.toString(context.value), SnapshotStatus.ACTIVE));
            trace.done(node);
            return;
        }
        int middle = model.left(node) + (model.right(node) - model.left(node)) / 2;
        int child = context.index <= middle ? model.leftChild(node) : model.rightChild(node);
        trace.add(
                LINE_POINT_SET_CHILD,
                "Recurse into child " + model.nodeId(child) + " containing index " + context.index,
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, "point-set", context.currentInterval,
                        context.pathText(), Long.toString(context.value), SnapshotStatus.ACTIVE));
        pointSet(model, child, context, logicalValues, trace);
        model.recompute(node);
        context.path.add(model.nodeId(node));
        trace.add(
                LINE_POINT_SET_COMBINE,
                "Recompute node " + model.nodeId(node) + " = " + model.value(node),
                StepEventType.EXECUTE_LINE,
                facts(model, logicalValues, "point-set", context.currentInterval,
                        context.pathText(), Long.toString(context.value), SnapshotStatus.ACTIVE));
        trace.done(node);
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

    private static String displayAggregate(long value) {
        return value == Long.MAX_VALUE ? "∞" : Long.toString(value);
    }

    private static List<Fact> facts(
            RangeTreeSupport.SegmentTreeModel model, long[] values, String operation,
            String nodeInterval, String partialAnswer, String answer, SnapshotStatus status,
            int queryLeft, int queryRight, String equation) {
        List<Fact> result = new ArrayList<>(facts(model, values, operation, nodeInterval, partialAnswer, answer, status));
        result.add(RangeTreeSupport.fact("query-range", "[" + queryLeft + "," + queryRight + "]", status));
        result.add(RangeTreeSupport.fact("teaching-equation", equation, status));
        return List.copyOf(result);
    }

    private static List<Fact> facts(
            RangeTreeSupport.SegmentTreeModel model,
            long[] values,
            String operation,
            String nodeInterval,
            String partialAnswer,
            String answer,
            SnapshotStatus status) {
        return List.of(
                RangeTreeSupport.fact("renderer", "segment", SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("values", RangeTreeSupport.formatValues(values), status),
                RangeTreeSupport.fact("node-interval", nodeInterval, status),
                RangeTreeSupport.fact("partial-answer", partialAnswer, status),
                RangeTreeSupport.fact("answer", answer, status),
                RangeTreeSupport.fact("tree", RangeTreeSupport.formatTree(model), status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        input.put("operation", "range-sum");
        input.put("left", 1);
        input.put("right", 3);
        return new SimulationMetadata(
                TYPE,
                "Segment Tree",
                "O(log n) query/update after O(n) build",
                "O(n)",
                RendererFamily.TREE,
                input,
                "Enter JSON as {\"values\":[2,1,3,5,4],\"operation\":\"range-sum\",\"left\":1,\"right\":3}; "
                        + "values length must be " + MIN_VALUES + ".." + MAX_VALUES
                        + ", values must have absolute value at most " + MAX_ABS_VALUE
                        + ", ranges are inclusive zero-based, and operation may be range-sum, range-min, or point-set.",
                PSEUDOCODE);
    }

    private static final class QueryContext {
        private final String operation;
        private final int left;
        private final int right;
        private final List<String> partial = new ArrayList<>();
        private String currentInterval;
        private long answer;
        private boolean hasAnswer;

        private QueryContext(String operation, int left, int right) {
            this.operation = operation;
            this.left = left;
            this.right = right;
            this.currentInterval = "[" + left + "," + right + "]";
        }

        private void recordCovered(String nodeId, long value, boolean minimum) {
            partial.add(nodeId + "=" + value);
            if (!hasAnswer) {
                answer = value;
                hasAnswer = true;
            } else {
                answer = minimum ? Math.min(answer, value) : answer + value;
            }
        }

        private String partialText() {
            return partial.isEmpty() ? "pending" : String.join(", ", partial);
        }
    }

    private static final class PointSetContext {
        private final int index;
        private final long value;
        private final List<String> path = new ArrayList<>();
        private String currentInterval = "pending";

        private PointSetContext(int index, long value) {
            this.index = index;
            this.value = value;
        }

        private String pathText() {
            return path.isEmpty() ? "pending" : String.join(" -> ", path);
        }
    }
}
