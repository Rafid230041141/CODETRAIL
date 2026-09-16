package dev.codetrail.desktop.simulation.structures;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TreeState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Max-heap construction, insertion, and extraction with a complete tree trace. */
public final class HeapEngine implements SimulationEngine {
    public static final String TYPE = "HEAP";
    public static final int MIN_VALUES = 0;
    public static final int MAX_VALUES = 15;
    public static final int MAX_ARRAY_LENGTH = MAX_VALUES;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_TRACE_STEPS = 1024;

    private static final int LINE_BUILD = 1;
    private static final int LINE_BUILD_LOOP = 2;
    private static final int LINE_BUILD_SIFT = 3;
    private static final int LINE_INSERT = 4;
    private static final int LINE_INSERT_APPEND = 5;
    private static final int LINE_INSERT_SIFT = 6;
    private static final int LINE_EXTRACT = 7;
    private static final int LINE_EXTRACT_EMPTY = 8;
    private static final int LINE_EXTRACT_VALUE = 9;
    private static final int LINE_EXTRACT_SWAP = 10;
    private static final int LINE_EXTRACT_SHRINK = 11;
    private static final int LINE_EXTRACT_SIFT = 12;
    private static final int LINE_SIFT_UP = 13;
    private static final int LINE_SIFT_UP_CHECK = 14;
    private static final int LINE_SIFT_UP_SWAP = 15;
    private static final int LINE_SIFT_UP_ADVANCE = 16;
    private static final int LINE_SIFT_DOWN = 17;
    private static final int LINE_SIFT_DOWN_CHECK = 18;
    private static final int LINE_SIFT_DOWN_CHILD = 19;
    private static final int LINE_SIFT_DOWN_ORDER = 20;
    private static final int LINE_SIFT_DOWN_SWAP = 21;
    private static final int LINE_SIFT_DOWN_ADVANCE = 22;
    private static final int LINE_RETURN = 23;

    private static final int[] DEFAULT_VALUES = {9, 4, 7, 1, 6};
    private static final List<String> PSEUDOCODE = List.of(
            "buildHeap(values):",
            "    for i = floor(n / 2) - 1 down to 0:",
            "        siftDown(i, n)",
            "if operation == insert:",
            "    append value at index n",
            "    siftUp(n)",
            "else if operation == extract:",
            "    if n == 0: return EMPTY",
            "    extracted = heap[0]",
            "    swap heap[0] and heap[n - 1]",
            "    n = n - 1",
            "    siftDown(0, n)",
            "siftUp(i):",
            "    while i > 0 and heap[parent(i)] < heap[i]:",
            "        swap heap[i] and heap[parent(i)]",
            "        i = parent(i)",
            "siftDown(i, n):",
            "    while left(i) < n:",
            "        child = larger of left(i), right(i)",
            "        if heap[i] >= heap[child]: break",
            "        swap heap[i] and heap[child]",
            "        i = child",
            "return heap and extracted value");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Model model = new Model(request.values());
        Trace trace = new Trace(model);
        OperationContext context = new OperationContext(request.operation());

        trace.add(
                model.size,
                0,
                "Initialize a max heap from " + model.size + " value(s)",
                StepEventType.INITIALIZE,
                facts(model, model.size, context.operation, context.extractedValue, "initialize", SnapshotStatus.ACTIVE));
        trace.add(
                model.size,
                LINE_BUILD,
                "Build the heap before the requested operation",
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue, "build", SnapshotStatus.ACTIVE));
        buildHeap(model, trace, context);

        if (request.operation().equals("insert")) {
            executeInsert(model, trace, context, request.value());
        } else {
            executeExtract(model, trace, context);
        }

        trace.markAllDone();
        trace.add(
                model.size,
                LINE_RETURN,
                "Return the max heap and extracted value " + context.extractedValue,
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue, "return", SnapshotStatus.DONE));
        trace.add(
                model.size,
                0,
                "Complete: max heap contains " + model.size + " value(s)",
                StepEventType.COMPLETE,
                facts(model, model.size, context.operation, context.extractedValue, "complete", SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void buildHeap(Model model, Trace trace, OperationContext context) {
        for (int root = model.size / 2 - 1; root >= 0; root--) {
            trace.beginPhase();
            trace.activate(root);
            trace.add(
                    model.size,
                    LINE_BUILD_LOOP,
                    "Heapify subtree rooted at logical index " + root,
                    StepEventType.EXECUTE_LINE,
                    facts(model, model.size, context.operation, context.extractedValue,
                            "bottom-up build", SnapshotStatus.ACTIVE));
            trace.add(
                    model.size,
                    LINE_BUILD_SIFT,
                    "Sift down build root " + root + " within the full heap",
                    StepEventType.EXECUTE_LINE,
                    facts(model, model.size, context.operation, context.extractedValue,
                            "bottom-up build", SnapshotStatus.ACTIVE));
            siftDown(model, trace, context, root, model.size, "bottom-up build");
            trace.finishPhase();
        }
    }

    private static void executeInsert(Model model, Trace trace, OperationContext context, int value) {
        trace.beginPhase();
        int insertionIndex = model.size;
        model.append(value);
        trace.activate(insertionIndex);
        trace.add(
                model.size,
                LINE_INSERT,
                "Insert value " + value + " at the next logical index " + insertionIndex,
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "insert; appended", SnapshotStatus.ACTIVE));
        trace.add(
                model.size,
                LINE_INSERT_APPEND,
                "Append " + value + " at heap index " + insertionIndex,
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "insert; appended", SnapshotStatus.ACTIVE));
        trace.add(
                model.size,
                LINE_INSERT_SIFT,
                "Sift the inserted value up toward the root",
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "insert; sift up", SnapshotStatus.ACTIVE));
        siftUp(model, trace, context, insertionIndex);
        trace.finishPhase();
    }

    private static void executeExtract(Model model, Trace trace, OperationContext context) {
        trace.beginPhase();
        trace.add(
                model.size,
                LINE_EXTRACT,
                "Extract the maximum from logical heap index 0",
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "extract", SnapshotStatus.ACTIVE));
        if (model.size == 0) {
            context.extractedValue = "EMPTY";
            trace.add(
                    0,
                    LINE_EXTRACT_EMPTY,
                    "Heap is empty; extraction returns EMPTY",
                    StepEventType.EXECUTE_LINE,
                    facts(model, 0, context.operation, context.extractedValue,
                            "extract; empty", SnapshotStatus.DONE));
            trace.finishPhase();
            return;
        }

        context.extractedValue = Integer.toString(model.values[0]);
        trace.activate(0);
        trace.add(
                model.size,
                LINE_EXTRACT_VALUE,
                "Save maximum value " + context.extractedValue + " from the root",
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "extract; root saved", SnapshotStatus.ACTIVE));

        if (model.size == 1) {
            model.removeLast();
            trace.markDone(0);
            trace.add(
                    0,
                    LINE_EXTRACT_SHRINK,
                    "Remove the only heap value; the heap is now empty",
                    StepEventType.EXECUTE_LINE,
                    facts(model, 0, context.operation, context.extractedValue,
                            "extract; removed root", SnapshotStatus.DONE));
            trace.finishPhase();
            return;
        }

        int last = model.size - 1;
        trace.activate(last);
        trace.add(
                model.size,
                LINE_EXTRACT_SWAP,
                "Swap root index 0 with final heap index " + last,
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "extract; before root swap", SnapshotStatus.ACTIVE));
        model.swap(0, last);
        trace.markDone(last);
        trace.activate(0);
        trace.add(
                last,
                LINE_EXTRACT_SWAP,
                "After the actual root/final swap, keep extracted value " + context.extractedValue
                        + " outside the active heap boundary",
                StepEventType.EXECUTE_LINE,
                facts(model, last, context.operation, context.extractedValue,
                        "extract; root swap complete", SnapshotStatus.ACTIVE));
        model.removeLast();
        trace.add(
                model.size,
                LINE_EXTRACT_SHRINK,
                "Shrink the active heap boundary to " + model.size,
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "extract; boundary shrunk", SnapshotStatus.ACTIVE));
        trace.add(
                model.size,
                LINE_EXTRACT_SIFT,
                "Sift the replacement root down to restore max-heap order",
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "extract; sift down", SnapshotStatus.ACTIVE));
        siftDown(model, trace, context, 0, model.size, "extract; sift down");
        trace.finishPhase();
    }

    private static void siftUp(Model model, Trace trace, OperationContext context, int start) {
        int index = start;
        trace.activate(index);
        while (index > 0) {
            int parent = (index - 1) / 2;
            trace.activate(parent);
            trace.add(
                    model.size,
                    LINE_SIFT_UP,
                    "Inspect inserted value at index " + index + " against parent " + parent,
                    StepEventType.EXECUTE_LINE,
                    facts(model, model.size, context.operation, context.extractedValue,
                            "insert; sift up", SnapshotStatus.ACTIVE));
            trace.add(
                    model.size,
                    LINE_SIFT_UP_CHECK,
                    "Check heap[parent=" + parent + "] = " + model.values[parent]
                            + " against heap[index=" + index + "] = " + model.values[index],
                    StepEventType.EXECUTE_LINE,
                    facts(model, model.size, context.operation, context.extractedValue,
                            "insert; compare parent", SnapshotStatus.ACTIVE));
            if (model.values[parent] >= model.values[index]) {
                trace.add(
                        model.size,
                        LINE_SIFT_UP_CHECK,
                        "Parent " + parent + " is already at least as large; stop sift up",
                        StepEventType.EXECUTE_LINE,
                        facts(model, model.size, context.operation, context.extractedValue,
                                "insert; heap order holds", SnapshotStatus.DONE));
                return;
            }
            model.swap(index, parent);
            trace.activate(index);
            trace.activate(parent);
            trace.add(
                    model.size,
                    LINE_SIFT_UP_SWAP,
                    "Swap heap indices " + index + " and " + parent,
                    StepEventType.EXECUTE_LINE,
                    facts(model, model.size, context.operation, context.extractedValue,
                            "insert; sift-up swap", SnapshotStatus.ACTIVE));
            index = parent;
            trace.activate(index);
            trace.add(
                    model.size,
                    LINE_SIFT_UP_ADVANCE,
                    "Continue sift up from index " + index,
                    StepEventType.EXECUTE_LINE,
                    facts(model, model.size, context.operation, context.extractedValue,
                            "insert; sift up", SnapshotStatus.ACTIVE));
        }
        trace.add(
                model.size,
                LINE_SIFT_UP_CHECK,
                "Inserted value reached the root; sift up is complete",
                StepEventType.EXECUTE_LINE,
                facts(model, model.size, context.operation, context.extractedValue,
                        "insert; root reached", SnapshotStatus.DONE));
    }

    private static void siftDown(
            Model model,
            Trace trace,
            OperationContext context,
            int start,
            int boundary,
            String phase) {
        if (boundary == 0) {
            return;
        }
        int index = start;
        trace.activate(index);
        while (true) {
            trace.activate(index);
            trace.add(
                    boundary,
                    LINE_SIFT_DOWN,
                    "Sift down from logical index " + index + " within heap size " + boundary,
                    StepEventType.EXECUTE_LINE,
                    facts(model, boundary, context.operation, context.extractedValue, phase, SnapshotStatus.ACTIVE));
            int left = 2 * index + 1;
            trace.add(
                    boundary,
                    LINE_SIFT_DOWN_CHECK,
                    "Check whether left child index " + left + " is inside the heap",
                    StepEventType.EXECUTE_LINE,
                    facts(model, boundary, context.operation, context.extractedValue, phase, SnapshotStatus.ACTIVE));
            if (left >= boundary) {
                trace.add(
                        boundary,
                        LINE_SIFT_DOWN_CHECK,
                        "No child remains at index " + index + "; sift down is complete",
                        StepEventType.EXECUTE_LINE,
                        facts(model, boundary, context.operation, context.extractedValue,
                                phase + "; leaf", SnapshotStatus.DONE));
                return;
            }

            int child = left;
            trace.activate(left);
            trace.add(
                    boundary,
                    LINE_SIFT_DOWN_CHILD,
                    "Choose left child index " + left,
                    StepEventType.EXECUTE_LINE,
                    facts(model, boundary, context.operation, context.extractedValue,
                            phase + "; choose child", SnapshotStatus.ACTIVE));
            int right = left + 1;
            if (right < boundary) {
                trace.activate(right);
                trace.add(
                        boundary,
                        LINE_SIFT_DOWN_CHILD,
                        "Compare children " + left + " and " + right + " and keep the larger one",
                        StepEventType.EXECUTE_LINE,
                        facts(model, boundary, context.operation, context.extractedValue,
                                phase + "; choose child", SnapshotStatus.ACTIVE));
                if (model.values[right] > model.values[left]) {
                    child = right;
                    trace.activate(child);
                    trace.add(
                            boundary,
                            LINE_SIFT_DOWN_CHILD,
                            "Select right child index " + right + " as the larger child",
                            StepEventType.EXECUTE_LINE,
                            facts(model, boundary, context.operation, context.extractedValue,
                                    phase + "; right child selected", SnapshotStatus.ACTIVE));
                }
            }

            trace.activate(child);
            trace.add(
                    boundary,
                    LINE_SIFT_DOWN_ORDER,
                    "Compare parent value " + model.values[index] + " with child value " + model.values[child],
                    StepEventType.EXECUTE_LINE,
                    facts(model, boundary, context.operation, context.extractedValue,
                            phase + "; parent check", SnapshotStatus.ACTIVE));
            if (model.values[index] >= model.values[child]) {
                trace.add(
                        boundary,
                        LINE_SIFT_DOWN_ORDER,
                        "Parent is at least as large as its larger child; stop sift down",
                        StepEventType.EXECUTE_LINE,
                        facts(model, boundary, context.operation, context.extractedValue,
                                phase + "; heap order holds", SnapshotStatus.DONE));
                return;
            }

            model.swap(index, child);
            trace.activate(index);
            trace.activate(child);
            trace.add(
                    boundary,
                    LINE_SIFT_DOWN_SWAP,
                    "Swap heap indices " + index + " and " + child,
                    StepEventType.EXECUTE_LINE,
                    facts(model, boundary, context.operation, context.extractedValue,
                            phase + "; sift-down swap", SnapshotStatus.ACTIVE));
            index = child;
            trace.activate(index);
            trace.add(
                    boundary,
                    LINE_SIFT_DOWN_ADVANCE,
                    "Continue sift down from index " + index,
                    StepEventType.EXECUTE_LINE,
                    facts(model, boundary, context.operation, context.extractedValue,
                            phase + "; continue", SnapshotStatus.ACTIVE));
        }
    }

    private static List<Fact> facts(
            Model model,
            int boundary,
            String operation,
            String extractedValue,
            String phase,
            SnapshotStatus status) {
        String currentArray = model.format(boundary);
        String heapSize = Integer.toString(boundary);
        return List.of(
                new Fact("operation", operation, SnapshotStatus.DEFAULT),
                new Fact("current-array", currentArray, status),
                new Fact("currentarray", currentArray, status),
                new Fact("extracted-value", extractedValue, status),
                new Fact("answer", extractedValue, status),
                new Fact("extractedvalue", extractedValue, status),
                new Fact("heap-size", heapSize, status),
                new Fact("phase", phase, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode values = defaultInput.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        defaultInput.put("operation", "insert");
        defaultInput.put("value", 8);
        return new SimulationMetadata(
                TYPE,
                "Heap",
                "O(log n) insert/extract after O(n) build",
                "O(n)",
                RendererFamily.TREE,
                defaultInput,
                "Enter JSON as {\"values\":[9,4,7,1,6],\"operation\":\"insert\",\"value\":8}; "
                        + "values length must be " + MIN_VALUES + ".." + MAX_VALUES
                        + ", every integer must have absolute value at most " + MAX_ABS_VALUE
                        + ", and operation must be insert with value or extract.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final int[] values;
        private int size;

        private Model(int[] initialValues) {
            values = new int[MAX_VALUES];
            System.arraycopy(initialValues, 0, values, 0, initialValues.length);
            size = initialValues.length;
        }

        private void append(int value) {
            if (size >= MAX_VALUES) {
                throw new IllegalStateException("heap cannot exceed " + MAX_VALUES + " values");
            }
            values[size++] = value;
        }

        private void removeLast() {
            if (size == 0) {
                throw new IllegalStateException("cannot remove from an empty heap");
            }
            size--;
        }

        private void swap(int first, int second) {
            int temporary = values[first];
            values[first] = values[second];
            values[second] = temporary;
        }

        private String format(int boundary) {
            if (boundary < 0 || boundary > size) {
                throw new IllegalArgumentException("heap fact boundary is outside the active heap");
            }
            StringBuilder formatted = new StringBuilder("[");
            for (int index = 0; index < boundary; index++) {
                if (index > 0) {
                    formatted.append(", ");
                }
                formatted.append(values[index]);
            }
            return formatted.append(']').toString();
        }
    }

    private static final class OperationContext {
        private final String operation;
        private String extractedValue = "none";

        private OperationContext(String operation) {
            this.operation = operation;
        }
    }

    private static final class Trace {
        private final Model model;
        private final SnapshotStatus[] statuses;
        private final Set<Integer> activeVertices = new LinkedHashSet<>();
        private final List<SimulationStep> steps = new ArrayList<>();

        private Trace(Model model) {
            this.model = Objects.requireNonNull(model, "model");
            statuses = new SnapshotStatus[MAX_VALUES];
            Arrays.fill(statuses, SnapshotStatus.DEFAULT);
        }

        private void beginPhase() {
            for (int index : activeVertices) {
                statuses[index] = SnapshotStatus.DEFAULT;
            }
            activeVertices.clear();
        }

        private void finishPhase() {
            for (int index : List.copyOf(activeVertices)) {
                statuses[index] = SnapshotStatus.DONE;
            }
            activeVertices.clear();
        }

        private void activate(int index) {
            if (index < 0 || index >= statuses.length) {
                throw new IllegalArgumentException("heap trace index is outside the bound: " + index);
            }
            statuses[index] = SnapshotStatus.ACTIVE;
            activeVertices.add(index);
        }

        private void markDone(int index) {
            if (index < 0 || index >= statuses.length) {
                throw new IllegalArgumentException("heap trace index is outside the bound: " + index);
            }
            statuses[index] = SnapshotStatus.DONE;
            activeVertices.remove(index);
        }

        private void markAllDone() {
            Arrays.fill(statuses, SnapshotStatus.DONE);
            activeVertices.clear();
        }

        private void add(
                int boundary,
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (boundary < 0 || boundary > model.size) {
                throw new IllegalArgumentException("heap trace boundary is outside the active heap");
            }
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("heap trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");

            List<Node> nodes = new ArrayList<>(model.size);
            for (int index = 0; index < model.size; index++) {
                SnapshotStatus status = index >= boundary ? SnapshotStatus.DONE : statuses[index];
                nodes.add(new Node(Integer.toString(index), index + " = " + model.values[index], status));
            }
            List<Edge> edges = new ArrayList<>(Math.max(0, model.size - 1));
            Set<String> activeEdges = new LinkedHashSet<>();
            for (int child = 1; child < model.size; child++) {
                int parent = (child - 1) / 2;
                String id = edgeId(parent, child);
                SnapshotStatus status = child >= boundary ? SnapshotStatus.DONE : statuses[child];
                edges.add(new Edge(id, Integer.toString(parent), Integer.toString(child), status));
                if (status == SnapshotStatus.ACTIVE) {
                    activeEdges.add(id);
                }
            }
            Set<String> activeNodes = new LinkedHashSet<>();
            for (int index : activeVertices) {
                if (index < boundary && index < model.size) {
                    activeNodes.add(Integer.toString(index));
                }
            }
            TreeState state = new TreeState(nodes, edges, model.size == 0 ? null : "0", facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.copyOf(activeNodes), Set.copyOf(activeEdges)),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        private static String edgeId(int parent, int child) {
            return "edge-" + parent + "-" + child;
        }
    }

    private record Request(int[] values, String operation, int value) {
        private Request {
            values = values.clone();
        }

        private static Request parse(JsonNode input) {
            if (input == null || !input.isObject()) {
                throw new IllegalArgumentException(TYPE + " input must be a JSON object");
            }
            JsonNode valuesNode = input.get("values");
            if (valuesNode == null || !valuesNode.isArray()) {
                throw new IllegalArgumentException("HEAP values must be a JSON array");
            }
            if (valuesNode.size() < MIN_VALUES || valuesNode.size() > MAX_VALUES) {
                throw new IllegalArgumentException(
                        "HEAP values length must be in the inclusive range " + MIN_VALUES + ".." + MAX_VALUES);
            }
            int[] values = new int[valuesNode.size()];
            for (int index = 0; index < valuesNode.size(); index++) {
                JsonNode valueNode = valuesNode.get(index);
                if (valueNode == null || !valueNode.isIntegralNumber() || !valueNode.canConvertToInt()) {
                    throw new IllegalArgumentException("HEAP values must contain bounded integers");
                }
                int value = valueNode.intValue();
                if (Math.abs((long) value) > MAX_ABS_VALUE) {
                    throw new IllegalArgumentException(
                            "HEAP values must have absolute value at most " + MAX_ABS_VALUE);
                }
                values[index] = value;
            }

            JsonNode operationNode = input.get("operation");
            if (operationNode == null || !operationNode.isTextual()) {
                throw new IllegalArgumentException("HEAP operation must be text");
            }
            String operation = operationNode.textValue();
            if (operation.equals("insert")) {
                if (values.length >= MAX_VALUES) {
                    throw new IllegalArgumentException(
                            "HEAP insert would exceed the maximum heap length " + MAX_VALUES);
                }
                requireExactFields(input, Set.of("values", "operation", "value"));
                JsonNode insertValue = input.get("value");
                if (insertValue == null || !insertValue.isIntegralNumber() || !insertValue.canConvertToInt()) {
                    throw new IllegalArgumentException("HEAP insert value must be an integer");
                }
                int value = insertValue.intValue();
                if (Math.abs((long) value) > MAX_ABS_VALUE) {
                    throw new IllegalArgumentException(
                            "HEAP insert value must have absolute value at most " + MAX_ABS_VALUE);
                }
                return new Request(values, operation, value);
            }
            if (operation.equals("extract")) {
                requireExactFields(input, Set.of("values", "operation"));
                return new Request(values, operation, 0);
            }
            throw new IllegalArgumentException("HEAP operation must be exactly insert or extract");
        }

        private static void requireExactFields(JsonNode input, Set<String> allowed) {
            Iterator<String> fields = input.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!allowed.contains(field)) {
                    throw new IllegalArgumentException("HEAP input has unsupported field: " + field);
                }
            }
        }
    }
}
