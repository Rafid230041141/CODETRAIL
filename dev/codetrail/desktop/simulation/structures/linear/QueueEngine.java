package dev.codetrail.desktop.simulation.structures.linear;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.ArrayState;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Bounded circular queue trace with explicit front/rear wraparound. */
public final class QueueEngine implements SimulationEngine {
    public static final String TYPE = "QUEUE";
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 10;
    public static final int MAX_QUEUE_CAPACITY = MAX_CAPACITY;
    public static final int MAX_ABS_VALUE = LinearStructureSupport.MAX_ABS_VALUE;
    public static final int MAX_OPERATIONS = LinearStructureSupport.MAX_OPERATIONS;
    public static final int MAX_TRACE_STEPS = LinearStructureSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_KIND = 2;
    private static final int LINE_FULL = 3;
    private static final int LINE_WRITE = 4;
    private static final int LINE_ADVANCE = 5;
    private static final int LINE_EMPTY = 6;
    private static final int LINE_READ = 7;
    private static final int LINE_CLEAR = 8;
    private static final int LINE_ADVANCE_FRONT = 9;
    private static final int LINE_RETURN = 10;

    private static final int DEFAULT_CAPACITY = 3;
    private static final int[] DEFAULT_VALUES = {4, 8, 1, 9};
    private static final List<String> PSEUDOCODE = List.of(
            "processQueue(capacity, operations):",
            "    if operation.kind == enqueue:",
            "        if size == capacity: report FULL",
            "        else slots[rear] = value; size++",
            "            rear = (rear + 1) mod capacity",
            "    else if size == 0: report EMPTY",
            "    else value = slots[front]",
            "        slots[front] = EMPTY; size--",
            "        front = (front + 1) mod capacity",
            "        return value");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        Trace trace = new Trace(request.capacity());
        trace.add(
                -1,
                0,
                "Initialize empty queue with capacity " + request.capacity(),
                StepEventType.INITIALIZE,
                facts("initialize", null, "none", trace, SnapshotStatus.ACTIVE));

        for (int operationIndex = 0; operationIndex < request.operations().size(); operationIndex++) {
            Operation operation = request.operations().get(operationIndex);
            trace.beginOperation();
            trace.add(
                    trace.front,
                    LINE_METHOD,
                    "Start operation " + (operationIndex + 1) + ": " + operation.describe(),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), operation.value(), "start", trace, SnapshotStatus.ACTIVE));
            trace.add(
                    trace.front,
                    LINE_KIND,
                    "Dispatch " + operation.kind(),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), operation.value(), "dispatch", trace, SnapshotStatus.ACTIVE));

            if (operation.isEnqueue()) {
                int slot = trace.rear();
                if (trace.size == trace.capacity) {
                    trace.status(slot, SnapshotStatus.REJECTED);
                    trace.add(
                            slot,
                            LINE_FULL,
                            "Queue is full; ignore enqueue " + operation.value(),
                            StepEventType.EXECUTE_LINE,
                            facts("enqueue", operation.value(), "FULL", trace, SnapshotStatus.REJECTED));
                } else {
                    trace.slots[slot] = operation.value();
                    trace.size++;
                    trace.status(slot, SnapshotStatus.ACTIVE);
                    trace.add(
                            slot,
                            LINE_WRITE,
                            "Write " + operation.value() + " at slot " + slot,
                            StepEventType.EXECUTE_LINE,
                            facts("enqueue", operation.value(), "write", trace, SnapshotStatus.ACTIVE));
                    trace.rear = (slot + 1) % trace.capacity;
                    trace.pointerMove = "rear = (" + slot + " + 1) mod " + trace.capacity + " = " + trace.rear;
                    trace.status(slot, SnapshotStatus.DONE);
                    trace.add(
                            trace.rear,
                            LINE_ADVANCE,
                            trace.pointerMove + "; rear marks the next write slot",
                            StepEventType.EXECUTE_LINE,
                            facts("enqueue", operation.value(), "enqueued", trace, SnapshotStatus.DONE));
                }
            } else if (trace.size == 0) {
                trace.status(trace.front, SnapshotStatus.REJECTED);
                trace.add(
                        trace.front,
                        LINE_EMPTY,
                        "Queue is empty; dequeue returns EMPTY",
                        StepEventType.EXECUTE_LINE,
                        facts("dequeue", null, "EMPTY", trace, SnapshotStatus.REJECTED));
            } else {
                int slot = trace.front;
                int value = trace.slots[slot];
                trace.status(slot, SnapshotStatus.ACTIVE);
                trace.add(
                        slot,
                        LINE_READ,
                        "Read front value " + value + " from slot " + slot,
                        StepEventType.EXECUTE_LINE,
                        facts("dequeue", value, "read", trace, SnapshotStatus.ACTIVE));
                trace.slots[slot] = null;
                trace.size--;
                trace.status(slot, SnapshotStatus.DONE);
                trace.add(
                        slot,
                        LINE_CLEAR,
                        "Clear slot " + slot + " and decrease size to " + trace.size,
                        StepEventType.EXECUTE_LINE,
                        facts("dequeue", value, "cleared", trace, SnapshotStatus.DONE));
                trace.front = (slot + 1) % trace.capacity;
                trace.pointerMove = "front = (" + slot + " + 1) mod " + trace.capacity + " = " + trace.front;
                trace.add(
                        trace.front,
                        LINE_ADVANCE_FRONT,
                        trace.pointerMove + "; front marks the next read slot",
                        StepEventType.EXECUTE_LINE,
                        facts("dequeue", value, "dequeued", trace, SnapshotStatus.DONE));
                trace.pointerMove = "none";
                trace.add(
                        trace.front,
                        LINE_RETURN,
                        "Return " + value,
                        StepEventType.EXECUTE_LINE,
                        facts("dequeue", value, "returned", trace, SnapshotStatus.DONE));
            }
        }

        trace.pointerMove = "none";
        trace.statuses(SnapshotStatus.DONE);
        trace.add(
                trace.size == 0 ? -1 : trace.front,
                0,
                "Complete: queue = " + trace.queueText(),
                StepEventType.COMPLETE,
                facts("complete", null, "complete", trace, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static List<Fact> facts(
            String operation,
            Integer value,
            String outcome,
            Trace trace,
            SnapshotStatus status) {
        return List.of(
                LinearStructureSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("capacity", Integer.toString(trace.capacity), SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("front", Integer.toString(trace.front), status),
                LinearStructureSupport.fact("rear", Integer.toString(trace.rear()), status),
                LinearStructureSupport.fact("size", Integer.toString(trace.size), status),
                LinearStructureSupport.fact("queue", trace.queueText(), status),
                LinearStructureSupport.fact("slots", trace.slotsText(), status),
                LinearStructureSupport.fact("value", LinearStructureSupport.integerText(value), status),
                LinearStructureSupport.fact("outcome", outcome, status),
                LinearStructureSupport.fact("pointer-move", trace.pointerMove, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("capacity", DEFAULT_CAPACITY);
        ArrayNode operations = defaultInput.putArray("operations");
        addEnqueue(operations, DEFAULT_VALUES[0]);
        addEnqueue(operations, DEFAULT_VALUES[1]);
        addDequeue(operations);
        addEnqueue(operations, DEFAULT_VALUES[2]);
        addEnqueue(operations, DEFAULT_VALUES[3]);
        return new SimulationMetadata(
                TYPE,
                "Queue",
                "O(1) enqueue/dequeue",
                "O(capacity)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"capacity\":3,\"operations\":[{\"kind\":\"enqueue\",\"value\":4},{\"kind\":\"dequeue\"}]}; capacity must be "
                        + MIN_CAPACITY + ".." + MAX_CAPACITY + ", use at most " + MAX_OPERATIONS
                        + " operations, enqueue values must have absolute value at most " + MAX_ABS_VALUE
                        + ", full enqueue reports FULL, and empty dequeue reports EMPTY.",
                PSEUDOCODE);
    }

    private static void addEnqueue(ArrayNode operations, int value) {
        ObjectNode operation = operations.addObject();
        operation.put("kind", "enqueue");
        operation.put("value", value);
    }

    private static void addDequeue(ArrayNode operations) {
        operations.addObject().put("kind", "dequeue");
    }

    private record Operation(String kind, Integer value) {
        boolean isEnqueue() { return kind.equals("enqueue"); }

        String describe() {
            return isEnqueue() ? "enqueue(" + value + ")" : "dequeue()";
        }
    }

    private record Request(int capacity, List<Operation> operations) {
        static Request parse(JsonNode input) {
            ObjectNode object = LinearStructureSupport.objectInput(input, TYPE);
            LinearStructureSupport.exactFields(object, Set.of("capacity", "operations"), TYPE);
            int capacity = LinearStructureSupport.integer(object.get("capacity"), TYPE + " capacity");
            LinearStructureSupport.requireIndex(capacity, MIN_CAPACITY, MAX_CAPACITY, TYPE + " capacity");
            JsonNode operationsNode = object.get("operations");
            if (operationsNode == null || !operationsNode.isArray()) {
                throw new IllegalArgumentException(TYPE + " operations must be a JSON array");
            }
            if (operationsNode.size() > MAX_OPERATIONS) {
                throw new IllegalArgumentException(TYPE + " operations must contain at most " + MAX_OPERATIONS + " entries");
            }
            List<Operation> operations = new ArrayList<>(operationsNode.size());
            for (int index = 0; index < operationsNode.size(); index++) {
                JsonNode operationNode = operationsNode.get(index);
                String context = TYPE + " operation " + index;
                LinearStructureSupport.exactFields(operationNode, Set.of("kind", "value"), context);
                ObjectNode operation = (ObjectNode) operationNode;
                String kind = LinearStructureSupport.text(operation.get("kind"), context + " kind");
                if (kind.equals("enqueue")) {
                    if (!operation.has("value")) {
                        throw new IllegalArgumentException(context + " enqueue requires value");
                    }
                    operations.add(new Operation(kind,
                            LinearStructureSupport.boundedValue(operation.get("value"), context + " value")));
                } else if (kind.equals("dequeue")) {
                    if (operation.has("value")) {
                        throw new IllegalArgumentException(context + " dequeue does not accept value");
                    }
                    operations.add(new Operation(kind, null));
                } else {
                    throw new IllegalArgumentException(context + " kind must be exactly enqueue or dequeue");
                }
            }
            return new Request(capacity, List.copyOf(operations));
        }
    }

    private static final class Trace {
        private final int capacity;
        private final Integer[] slots;
        private final SnapshotStatus[] statuses;
        private final List<SimulationStep> steps = new ArrayList<>();
        private int front;
        private int rear;
        private int size;
        private String pointerMove = "none";

        private Trace(int capacity) {
            this.capacity = capacity;
            this.slots = new Integer[capacity];
            this.statuses = new SnapshotStatus[capacity];
            statuses(SnapshotStatus.DEFAULT);
        }

        private int rear() {
            return rear;
        }

        private void beginOperation() {
            pointerMove = "none";
            statuses(SnapshotStatus.DEFAULT);
        }

        private void status(int index, SnapshotStatus status) {
            statuses[index] = status;
        }

        private void statuses(SnapshotStatus status) {
            java.util.Arrays.fill(statuses, status);
        }

        private String queueText() {
            List<Integer> values = new ArrayList<>(size);
            for (int offset = 0; offset < capacity; offset++) {
                Integer value = slots[(front + offset) % capacity];
                if (value != null) values.add(value);
            }
            return LinearStructureSupport.formatValues(values);
        }

        private String slotsText() {
            return LinearStructureSupport.formatPhysicalSlots(slots);
        }

        private void add(
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("linear structure trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            List<TypedCell> cells = new ArrayList<>(capacity);
            for (int index = 0; index < capacity; index++) {
                cells.add(new TypedCell("slot " + index,
                        LinearStructureSupport.integerText(slots[index]), statuses[index]));
            }
            ArrayState state = new ArrayState(cells, focusIndex, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }
}
