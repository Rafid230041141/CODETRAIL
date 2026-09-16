package dev.codetrail.desktop.simulation.structures.linear;

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
import java.util.Objects;
import java.util.Set;

/** Bounded array insertion/deletion with every physical shift exposed. */
public final class ArrayEngine implements SimulationEngine {
    public static final String TYPE = "ARRAY";
    public static final int MAX_VALUES = 16;
    public static final int MAX_ARRAY_LENGTH = MAX_VALUES;
    public static final int MAX_ABS_VALUE = LinearStructureSupport.MAX_ABS_VALUE;
    public static final int MAX_OPERATIONS = LinearStructureSupport.MAX_OPERATIONS;
    public static final int MAX_TRACE_STEPS = LinearStructureSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_KIND = 2;
    private static final int LINE_SHIFT_RIGHT = 3;
    private static final int LINE_COPY_RIGHT = 4;
    private static final int LINE_STORE = 5;
    private static final int LINE_DELETE_KIND = 6;
    private static final int LINE_SHIFT_LEFT = 7;
    private static final int LINE_COPY_LEFT = 8;
    private static final int LINE_SHRINK = 9;

    private static final int DEFAULT_INDEX = 2;
    private static final int DEFAULT_VALUE = 9;
    private static final int[] DEFAULT_VALUES = {8, 3, 6, 1};
    private static final List<String> PSEUDOCODE = List.of(
            "editArray(values, operation):",
            "    if operation is insert:",
            "        for position = length down to index + 1:",
            "            values[position] = values[position - 1]",
            "        values[index] = value",
            "    else if operation is delete:",
            "        for position = index to length - 2:",
            "            values[position] = values[position + 1]",
            "        length--");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        Request request = Request.parse(input);
        LinearStructureSupport.ArrayTrace trace = new LinearStructureSupport.ArrayTrace(request.values());
        int logicalLength = request.values().length;
        trace.add(
                -1,
                0,
                "Initialize array with " + logicalLength + " value(s)",
                StepEventType.INITIALIZE,
                facts("initialize", -1, null, "none", trace, logicalLength, SnapshotStatus.ACTIVE));

        for (int operationIndex = 0; operationIndex < request.operations().size(); operationIndex++) {
            Operation operation = request.operations().get(operationIndex);
            trace.beginOperation();
            trace.add(
                    -1,
                    LINE_METHOD,
                    "Start edit " + (operationIndex + 1) + ": " + operation.describe(),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), operation.index(), operation.value(), "start", trace,
                            logicalLength, SnapshotStatus.ACTIVE));
            if (operation.isInsert()) {
                trace.add(
                        trace.safeFocus(operation.index()),
                        LINE_KIND,
                        "Choose insertion at index " + operation.index(),
                        StepEventType.EXECUTE_LINE,
                        facts(operation.kind(), operation.index(), operation.value(), "insert", trace,
                                logicalLength, SnapshotStatus.ACTIVE));
                trace.appendEmpty();
                trace.status(logicalLength, SnapshotStatus.ACTIVE);
                trace.add(
                        logicalLength,
                        LINE_SHIFT_RIGHT,
                        "Open the new last slot before shifting",
                        StepEventType.EXECUTE_LINE,
                        facts(operation.kind(), operation.index(), operation.value(),
                                "append slot", trace, logicalLength, SnapshotStatus.ACTIVE));
                trace.beginOperation();
                for (int source = logicalLength - 1; source >= operation.index(); source--) {
                    trace.cells.set(source + 1, trace.cells.get(source));
                    trace.status(source, SnapshotStatus.ACTIVE);
                    trace.status(source + 1, SnapshotStatus.ACTIVE);
                    trace.add(
                            source + 1,
                            LINE_COPY_RIGHT,
                            "Shift index " + source + " to index " + (source + 1),
                            StepEventType.EXECUTE_LINE,
                            facts(operation.kind(), operation.index(), operation.value(),
                                    source + " -> " + (source + 1), trace, logicalLength,
                                    SnapshotStatus.ACTIVE));
                    trace.beginOperation();
                }
                trace.cells.set(operation.index(), operation.value());
                trace.status(operation.index(), SnapshotStatus.DONE);
                logicalLength++;
                trace.add(
                        operation.index(),
                        LINE_STORE,
                        "Store " + operation.value() + " at index " + operation.index(),
                        StepEventType.EXECUTE_LINE,
                        facts(operation.kind(), operation.index(), operation.value(),
                                "placed", trace, logicalLength, SnapshotStatus.DONE));
            } else {
                trace.add(
                        operation.index(),
                        LINE_DELETE_KIND,
                        "Choose deletion at index " + operation.index(),
                        StepEventType.EXECUTE_LINE,
                        facts(operation.kind(), operation.index(), null, "delete", trace,
                                logicalLength, SnapshotStatus.ACTIVE));
                for (int source = operation.index(); source < logicalLength - 1; source++) {
                    trace.cells.set(source, trace.cells.get(source + 1));
                    trace.status(source, SnapshotStatus.ACTIVE);
                    trace.status(source + 1, SnapshotStatus.ACTIVE);
                    trace.add(
                            source,
                            LINE_COPY_LEFT,
                            "Shift index " + (source + 1) + " to index " + source,
                            StepEventType.EXECUTE_LINE,
                            facts(operation.kind(), operation.index(), null,
                                    (source + 1) + " -> " + source, trace, logicalLength,
                                    SnapshotStatus.ACTIVE));
                    trace.beginOperation();
                }
                trace.removeLast();
                logicalLength--;
                trace.statuses(SnapshotStatus.DONE);
                trace.add(
                        logicalLength == 0 ? -1 : Math.min(operation.index(), logicalLength - 1),
                        LINE_SHRINK,
                        "Remove the trailing duplicate; length = " + logicalLength,
                        StepEventType.EXECUTE_LINE,
                        facts(operation.kind(), operation.index(), null,
                                "shrink", trace, logicalLength, SnapshotStatus.DONE));
            }
            trace.statuses(SnapshotStatus.DONE);
            trace.add(
                    logicalLength == 0 ? -1 : Math.min(operation.index(), logicalLength - 1),
                    0,
                    "Edit complete; array = " + LinearStructureSupport.formatValues(trace.cells),
                    StepEventType.EXECUTE_LINE,
                    facts(operation.kind(), operation.index(), operation.value(),
                            "complete", trace, logicalLength, SnapshotStatus.DONE));
        }

        trace.statuses(SnapshotStatus.DONE);
        trace.add(
                logicalLength == 0 ? -1 : 0,
                0,
                "Complete: array = " + LinearStructureSupport.formatValues(trace.cells),
                StepEventType.COMPLETE,
                facts("complete", -1, null, "complete", trace, logicalLength, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static List<Fact> facts(
            String operation,
            int index,
            Integer value,
            String shift,
            LinearStructureSupport.ArrayTrace trace,
            int logicalLength,
            SnapshotStatus status) {
        return List.of(
                LinearStructureSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                LinearStructureSupport.fact("index", Integer.toString(index), status),
                LinearStructureSupport.fact("value", LinearStructureSupport.integerText(value), status),
                LinearStructureSupport.fact("array", LinearStructureSupport.formatValues(trace.cells), status),
                LinearStructureSupport.fact("shift", shift, status),
                LinearStructureSupport.fact("length", Integer.toString(logicalLength), status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode values = defaultInput.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        defaultInput.put("operation", "insert");
        defaultInput.put("index", DEFAULT_INDEX);
        defaultInput.put("value", DEFAULT_VALUE);
        return new SimulationMetadata(
                TYPE,
                "Array",
                "O(n) insertion/deletion",
                "O(n)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"values\":[8,3,6,1],\"operation\":\"insert\",\"index\":2,\"value\":9}; values length must be 0.."
                        + MAX_VALUES + ", each value must have absolute value at most " + MAX_ABS_VALUE
                        + ". A delete uses operation=delete and omits value. For a multi-edit trace use operations with kind insert or delete.",
                PSEUDOCODE);
    }

    private record Operation(String kind, int index, Integer value) {
        boolean isInsert() {
            return kind.equals("insert");
        }

        String describe() {
            return isInsert()
                    ? "insert(" + index + ", " + value + ")"
                    : "delete(" + index + ")";
        }
    }

    private record Request(int[] values, List<Operation> operations) {
        static Request parse(JsonNode input) {
            ObjectNode object = LinearStructureSupport.objectInput(input, TYPE);
            if (object.has("operations")) {
                LinearStructureSupport.exactFields(object, Set.of("values", "operations"), TYPE);
                int[] values = LinearStructureSupport.values(object, "values", TYPE, MAX_VALUES);
                JsonNode operationsNode = object.get("operations");
                if (!operationsNode.isArray()) {
                    throw new IllegalArgumentException(TYPE + " operations must be a JSON array");
                }
                if (operationsNode.size() > MAX_OPERATIONS) {
                    throw new IllegalArgumentException(TYPE + " operations must contain at most " + MAX_OPERATIONS + " entries");
                }
                List<Operation> operations = new ArrayList<>(operationsNode.size());
                int length = values.length;
                for (int index = 0; index < operationsNode.size(); index++) {
                    JsonNode operationNode = operationsNode.get(index);
                    String context = TYPE + " operation " + index;
                    LinearStructureSupport.exactFields(operationNode, Set.of("kind", "index", "value"), context);
                    ObjectNode operation = (ObjectNode) operationNode;
                    String kind = LinearStructureSupport.text(operation.get("kind"), context + " kind");
                    int editIndex = LinearStructureSupport.integer(operation.get("index"), context + " index");
                    if (kind.equals("insert")) {
                        LinearStructureSupport.requireIndex(editIndex, 0, length, context + " index");
                        if (!operation.has("value")) {
                            throw new IllegalArgumentException(context + " insert requires value");
                        }
                        int value = LinearStructureSupport.boundedValue(operation.get("value"), context + " value");
                        if (length >= MAX_VALUES) {
                            throw new IllegalArgumentException(TYPE + " insertion would exceed " + MAX_VALUES + " values");
                        }
                        operations.add(new Operation(kind, editIndex, value));
                        length++;
                    } else if (kind.equals("delete")) {
                        LinearStructureSupport.requireIndex(editIndex, 0, length - 1, context + " index");
                        if (operation.has("value")) {
                            throw new IllegalArgumentException(context + " delete does not accept value");
                        }
                        operations.add(new Operation(kind, editIndex, null));
                        length--;
                    } else {
                        throw new IllegalArgumentException(context + " kind must be exactly insert or delete");
                    }
                }
                return new Request(values, List.copyOf(operations));
            }

            LinearStructureSupport.exactFields(object, Set.of("values", "operation", "index", "value"), TYPE);
            int[] values = LinearStructureSupport.values(object, "values", TYPE, MAX_VALUES);
            String operation = LinearStructureSupport.text(object.get("operation"), TYPE + " operation");
            int index = LinearStructureSupport.integer(object.get("index"), TYPE + " index");
            if (operation.equals("insert")) {
                LinearStructureSupport.requireIndex(index, 0, values.length, TYPE + " insert index");
                if (!object.has("value")) {
                    throw new IllegalArgumentException(TYPE + " insert requires value");
                }
                int value = LinearStructureSupport.boundedValue(object.get("value"), TYPE + " value");
                if (values.length >= MAX_VALUES) {
                    throw new IllegalArgumentException(TYPE + " insertion would exceed " + MAX_VALUES + " values");
                }
                return new Request(values, List.of(new Operation(operation, index, value)));
            }
            if (operation.equals("delete")) {
                LinearStructureSupport.requireIndex(index, 0, values.length - 1, TYPE + " delete index");
                if (object.has("value")) {
                    throw new IllegalArgumentException(TYPE + " delete does not accept value");
                }
                return new Request(values, List.of(new Operation(operation, index, null)));
            }
            throw new IllegalArgumentException(TYPE + " operation must be exactly insert or delete");
        }
    }
}
