package dev.codetrail.desktop.simulation.range.remaining;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.ArrayState;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Small package-local boundary shared by the remaining bounded range engines. */
final class RemainingRangeSupport {
    static final int MIN_VALUES = 1;
    static final int MAX_VALUES = 16;
    static final int MAX_ABS_VALUE = 999;
    static final int MAX_OPERATIONS = 12;
    static final int MAX_TRACE_STEPS = 4096;

    private RemainingRangeSupport() {
    }

    static JsonNode requireObject(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        return input;
    }

    static int[] readValues(JsonNode input, String type) {
        JsonNode object = requireObject(input, type);
        JsonNode valuesNode = object.get("values");
        if (valuesNode == null || !valuesNode.isArray()) {
            throw new IllegalArgumentException(type + " values must be a JSON array");
        }
        if (valuesNode.size() < MIN_VALUES || valuesNode.size() > MAX_VALUES) {
            throw new IllegalArgumentException(
                    type + " values length must be in the inclusive range " + MIN_VALUES + ".." + MAX_VALUES);
        }
        int[] values = new int[valuesNode.size()];
        for (int index = 0; index < valuesNode.size(); index++) {
            values[index] = readBoundedInt(valuesNode.get(index), type + " values[" + index + "]");
        }
        return values;
    }

    static int readBoundedInt(JsonNode node, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be a bounded integer");
        }
        int value = node.intValue();
        if (Math.abs((long) value) > MAX_ABS_VALUE) {
            throw new IllegalArgumentException(
                    field + " must have absolute value at most " + MAX_ABS_VALUE);
        }
        return value;
    }

    static int readIndex(JsonNode node, int length, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer index");
        }
        int index = node.intValue();
        if (index < 0 || index >= length) {
            throw new IllegalArgumentException(field + " must be in the range 0.." + (length - 1));
        }
        return index;
    }

    static void requireRange(int left, int right, int length, String field) {
        if (left < 0 || right < left || right >= length) {
            throw new IllegalArgumentException(
                    field + " must satisfy 0 <= left <= right < " + length);
        }
    }

    static int readBlockSize(JsonNode node, int length, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer block size");
        }
        int blockSize = node.intValue();
        if (blockSize < 1 || blockSize > length) {
            throw new IllegalArgumentException(field + " must be in the range 1.." + length);
        }
        return blockSize;
    }

    static String readKind(JsonNode object, String field, String... allowed) {
        JsonNode kindNode = object.get("kind");
        if (kindNode == null || !kindNode.isTextual()) {
            throw new IllegalArgumentException(field + " kind must be a string");
        }
        String kind = kindNode.textValue();
        for (String candidate : allowed) {
            if (candidate.equals(kind)) {
                return kind;
            }
        }
        throw new IllegalArgumentException(field + " kind is unsupported: " + kind);
    }

    static String readOperation(JsonNode object, String type, String... allowed) {
        JsonNode operationNode = object.get("operation");
        if (operationNode == null || !operationNode.isTextual()) {
            throw new IllegalArgumentException(type + " operation must be a string");
        }
        String operation = operationNode.textValue();
        for (String candidate : allowed) {
            if (candidate.equals(operation)) {
                return operation;
            }
        }
        throw new IllegalArgumentException(type + " operation is unsupported: " + operation);
    }

    static List<RangeQuery> readQueries(JsonNode input, String type) {
        JsonNode object = requireObject(input, type);
        int[] values = readValues(object, type);
        JsonNode queriesNode = object.get("queries");
        if (queriesNode == null || !queriesNode.isArray() || queriesNode.isEmpty()) {
            throw new IllegalArgumentException(type + " queries must be a nonempty JSON array");
        }
        if (queriesNode.size() > MAX_OPERATIONS) {
            throw new IllegalArgumentException(type + " queries count must be at most " + MAX_OPERATIONS);
        }
        List<RangeQuery> queries = new ArrayList<>(queriesNode.size());
        for (int queryIndex = 0; queryIndex < queriesNode.size(); queryIndex++) {
            JsonNode queryNode = queriesNode.get(queryIndex);
            if (queryNode == null || !queryNode.isObject()) {
                throw new IllegalArgumentException(
                        type + " query " + queryIndex + " must be a JSON object");
            }
            String field = type + " query " + queryIndex;
            int left = readIndex(queryNode.get("left"), values.length, field + " left");
            int right = readIndex(queryNode.get("right"), values.length, field + " right");
            requireRange(left, right, values.length, field + " range");
            queries.add(new RangeQuery(left, right));
        }
        return List.copyOf(queries);
    }

    static List<Operation> readOperations(JsonNode input, String type) {
        JsonNode object = requireObject(input, type);
        int[] values = readValues(object, type);
        JsonNode operationsNode = object.get("operations");
        if (operationsNode == null || !operationsNode.isArray() || operationsNode.isEmpty()) {
            throw new IllegalArgumentException(type + " operations must be a nonempty JSON array");
        }
        if (operationsNode.size() > MAX_OPERATIONS) {
            throw new IllegalArgumentException(
                    type + " operations count must be at most " + MAX_OPERATIONS);
        }
        List<Operation> operations = new ArrayList<>(operationsNode.size());
        for (int operationIndex = 0; operationIndex < operationsNode.size(); operationIndex++) {
            JsonNode operationNode = operationsNode.get(operationIndex);
            if (operationNode == null || !operationNode.isObject()) {
                throw new IllegalArgumentException(
                        type + " operation " + operationIndex + " must be a JSON object");
            }
            String field = type + " operation " + operationIndex;
            String kind = readKind(operationNode, field, "query", "update");
            if (kind.equals("query")) {
                int left = readIndex(operationNode.get("left"), values.length, field + " left");
                int right = readIndex(operationNode.get("right"), values.length, field + " right");
                requireRange(left, right, values.length, field + " range");
                operations.add(new Operation(kind, left, right, -1, 0));
            } else {
                int index = readIndex(operationNode.get("index"), values.length, field + " index");
                int value = readBoundedInt(operationNode.get("value"), field + " value");
                operations.add(new Operation(kind, -1, -1, index, value));
            }
        }
        return List.copyOf(operations);
    }

    static SnapshotStatus[] statuses(int size, SnapshotStatus status) {
        SnapshotStatus[] result = new SnapshotStatus[size];
        Arrays.fill(result, Objects.requireNonNull(status, "status"));
        return result;
    }

    static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    static String formatValues(int[] values) {
        Objects.requireNonNull(values, "values");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (int value : values) {
            joiner.add(Integer.toString(value));
        }
        return joiner.toString();
    }

    static String formatValues(long[] values) {
        Objects.requireNonNull(values, "values");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (long value : values) {
            joiner.add(Long.toString(value));
        }
        return joiner.toString();
    }

    static String formatFrequencies(Map<Integer, Integer> frequencies) {
        Objects.requireNonNull(frequencies, "frequencies");
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() > 0) {
                joiner.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return joiner.toString();
    }

    static String formatRanges(List<RangeQuery> queries) {
        Objects.requireNonNull(queries, "queries");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (RangeQuery query : queries) {
            joiner.add("[" + query.left() + "," + query.right() + "]");
        }
        return joiner.toString();
    }

    static String formatOperations(List<Operation> operations) {
        Objects.requireNonNull(operations, "operations");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Operation operation : operations) {
            if (operation.kind().equals("query")) {
                joiner.add("query[" + operation.left() + "," + operation.right() + "]");
            } else {
                joiner.add("update[" + operation.index() + "]=" + operation.value());
            }
        }
        return joiner.toString();
    }

    static String formatKnownAnswers(long[] answers, boolean[] known) {
        if (answers.length != known.length) {
            throw new IllegalArgumentException("answer and known arrays must have equal lengths");
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (int index = 0; index < answers.length; index++) {
            joiner.add(known[index] ? Long.toString(answers[index]) : "pending");
        }
        return joiner.toString();
    }

    static void addArrayStep(
            List<SimulationStep> steps,
            String type,
            long[] values,
            SnapshotStatus[] statuses,
            int focusIndex,
            int highlightedLine,
            String narration,
            StepEventType eventType,
            List<Fact> facts) {
        Objects.requireNonNull(steps, "steps");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(statuses, "statuses");
        if (values.length != statuses.length) {
            throw new IllegalArgumentException(type + " status count must match array length");
        }
        if (steps.size() >= MAX_TRACE_STEPS) {
            throw new IllegalStateException(type + " trace exceeded " + MAX_TRACE_STEPS + " steps");
        }
        List<TypedCell> cells = new ArrayList<>(values.length);
        for (int index = 0; index < values.length; index++) {
            cells.add(new TypedCell(Integer.toString(index), Long.toString(values[index]),
                    Objects.requireNonNull(statuses[index], "status")));
        }
        ArrayState state = new ArrayState(cells, focusIndex, facts);
        steps.add(new SimulationStep(
                new SimulationSnapshot(state, Set.of(), Set.of()),
                highlightedLine,
                Objects.requireNonNull(narration, "narration"),
                Objects.requireNonNull(eventType, "eventType"),
                null));
    }

    static void addTableStep(
            List<SimulationStep> steps,
            String type,
            List<String> columns,
            List<List<TypedCell>> rows,
            int highlightedLine,
            String narration,
            StepEventType eventType,
            List<Fact> facts) {
        Objects.requireNonNull(steps, "steps");
        Objects.requireNonNull(columns, "columns");
        Objects.requireNonNull(rows, "rows");
        if (steps.size() >= MAX_TRACE_STEPS) {
            throw new IllegalStateException(type + " trace exceeded " + MAX_TRACE_STEPS + " steps");
        }
        TableState state = new TableState(columns, rows, facts);
        if (state.rendererFamily() != RendererFamily.TABLE) {
            throw new IllegalStateException(type + " did not create a TABLE state");
        }
        steps.add(new SimulationStep(
                new SimulationSnapshot(state, Set.of(), Set.of()),
                highlightedLine,
                Objects.requireNonNull(narration, "narration"),
                Objects.requireNonNull(eventType, "eventType"),
                null));
    }

    record RangeQuery(int left, int right) {
        RangeQuery {
            if (left < 0 || right < left) {
                throw new IllegalArgumentException("range query endpoints are malformed");
            }
        }
    }

    record Operation(String kind, int left, int right, int index, int value) {
        Operation {
            Objects.requireNonNull(kind, "kind");
            if (!kind.equals("query") && !kind.equals("update")) {
                throw new IllegalArgumentException("unsupported operation kind: " + kind);
            }
        }
    }
}
