package dev.codetrail.desktop.simulation.range;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validated public inputs shared by the bounded range-query simulations.
 *
 * <p>The JSON boundary deliberately uses zero-based indices.  Fenwick's
 * internal one-based walk is an implementation detail of its engine and is
 * exposed in its typed trace facts.</p>
 */
public final class RangeInput {
    public static final int MIN_VALUES = 1;
    public static final int MAX_VALUES = 16;
    public static final int MAX_ABS_VALUE = 999;
    public static final int MAX_OPERATIONS = 12;

    private RangeInput() {
    }

    /** Parse the segment-tree public shape. */
    public static SegmentRequest parseSegment(JsonNode input) {
        JsonNode object = requireObject(input, "SEGMENT_TREE");
        int[] values = readValues(object, "SEGMENT_TREE");
        String operation = readOperation(object, "SEGMENT_TREE", "range-sum", "range-min", "point-set");
        if (operation.equals("point-set")) {
            int index = readIndex(object.get("index"), values.length, "SEGMENT_TREE index");
            int value = readBoundedInt(object.get("value"), "SEGMENT_TREE value");
            return new SegmentRequest(values, operation, -1, -1, index, value);
        }
        int left = readIndex(object.get("left"), values.length, "SEGMENT_TREE left");
        int right = readIndex(object.get("right"), values.length, "SEGMENT_TREE right");
        requireRange(left, right, values.length, "SEGMENT_TREE query range");
        return new SegmentRequest(values, operation, left, right, -1, -1);
    }

    /** Parse the Fenwick/BIT public shape. */
    public static FenwickRequest parseFenwick(JsonNode input) {
        JsonNode object = requireObject(input, "FENWICK_TREE");
        int[] values = readValues(object, "FENWICK_TREE");
        String operation = readOperation(object, "FENWICK_TREE", "update", "query");
        int index = readIndex(object.get("index"), values.length, "FENWICK_TREE index");
        if (operation.equals("update")) {
            int delta = readBoundedInt(object.get("delta"), "FENWICK_TREE delta");
            return new FenwickRequest(values, operation, index, delta);
        }
        return new FenwickRequest(values, operation, index, 0);
    }

    /** Parse the lazy segment-tree sequence shape. */
    public static LazyRequest parseLazy(JsonNode input) {
        JsonNode object = requireObject(input, "SEGMENT_TREE_LAZY");
        int[] values = readValues(object, "SEGMENT_TREE_LAZY");
        JsonNode operationsNode = object.get("operations");
        if (operationsNode == null || !operationsNode.isArray() || operationsNode.isEmpty()) {
            throw new IllegalArgumentException("SEGMENT_TREE_LAZY operations must be a nonempty JSON array");
        }
        if (operationsNode.size() > MAX_OPERATIONS) {
            throw new IllegalArgumentException(
                    "SEGMENT_TREE_LAZY operations count must be at most " + MAX_OPERATIONS);
        }
        List<LazyOperation> operations = new ArrayList<>(operationsNode.size());
        for (int operationIndex = 0; operationIndex < operationsNode.size(); operationIndex++) {
            JsonNode operationNode = operationsNode.get(operationIndex);
            if (operationNode == null || !operationNode.isObject()) {
                throw new IllegalArgumentException(
                        "SEGMENT_TREE_LAZY operation " + operationIndex + " must be a JSON object");
            }
            String field = "SEGMENT_TREE_LAZY operation " + operationIndex;
            String kind = readKind(operationNode, field, "range-add", "range-sum");
            int left = readIndex(operationNode.get("left"), values.length, field + " left");
            int right = readIndex(operationNode.get("right"), values.length, field + " right");
            requireRange(left, right, values.length, field + " range");
            int delta = kind.equals("range-add")
                    ? readBoundedInt(operationNode.get("delta"), field + " delta")
                    : 0;
            operations.add(new LazyOperation(kind, left, right, delta));
        }
        return new LazyRequest(values, operations);
    }

    /** Read a bounded values array, returning a fresh primitive array. */
    public static int[] readValues(JsonNode input, String type) {
        JsonNode object = requireObject(input, type);
        return readValuesObject(object, type);
    }

    private static int[] readValuesObject(JsonNode object, String type) {
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

    private static JsonNode requireObject(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        return input;
    }

    private static String readOperation(JsonNode object, String type, String... allowed) {
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

    private static String readKind(JsonNode object, String type, String... allowed) {
        JsonNode kindNode = object.get("kind");
        if (kindNode == null || !kindNode.isTextual()) {
            throw new IllegalArgumentException(type + " kind must be a string");
        }
        String kind = kindNode.textValue();
        for (String candidate : allowed) {
            if (candidate.equals(kind)) {
                return kind;
            }
        }
        throw new IllegalArgumentException(type + " kind is unsupported: " + kind);
    }

    private static int readBoundedInt(JsonNode node, String field) {
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

    private static int readIndex(JsonNode node, int length, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer index");
        }
        int index = node.intValue();
        if (index < 0 || index >= length) {
            throw new IllegalArgumentException(field + " must be in the range 0.." + (length - 1));
        }
        return index;
    }

    private static void requireRange(int left, int right, int length, String field) {
        if (left < 0 || right < left || right >= length) {
            throw new IllegalArgumentException(
                    field + " must satisfy 0 <= left <= right < " + length);
        }
    }

    private static int[] copyValues(int[] values) {
        Objects.requireNonNull(values, "values");
        if (values.length < MIN_VALUES || values.length > MAX_VALUES) {
            throw new IllegalArgumentException("values length is outside the bounded range");
        }
        int[] copy = values.clone();
        for (int value : copy) {
            if (Math.abs((long) value) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException("values contain an out-of-bounds integer");
            }
        }
        return copy;
    }

    /** Validated one-operation segment-tree request. */
    public record SegmentRequest(
            int[] values,
            String operation,
            int left,
            int right,
            int index,
            int value) {
        public SegmentRequest {
            values = copyValues(values);
            Objects.requireNonNull(operation, "operation");
            if (!operation.equals("range-sum") && !operation.equals("range-min")
                    && !operation.equals("point-set")) {
                throw new IllegalArgumentException("unsupported segment operation: " + operation);
            }
            if (operation.equals("point-set")) {
                if (index < 0 || index >= values.length) {
                    throw new IllegalArgumentException("point-set index is outside the values array");
                }
                if (Math.abs((long) value) > MAX_ABS_VALUE) {
                    throw new IllegalArgumentException("point-set value is out of bounds");
                }
            } else {
                requireRange(left, right, values.length, "segment query range");
            }
        }

        @Override
        public int[] values() {
            return values.clone();
        }

        public boolean isQuery() {
            return operation.equals("range-sum") || operation.equals("range-min");
        }
    }

    /** Validated one-operation Fenwick request. */
    public record FenwickRequest(int[] values, String operation, int index, int delta) {
        public FenwickRequest {
            values = copyValues(values);
            Objects.requireNonNull(operation, "operation");
            if (!operation.equals("update") && !operation.equals("query")) {
                throw new IllegalArgumentException("unsupported Fenwick operation: " + operation);
            }
            if (index < 0 || index >= values.length) {
                throw new IllegalArgumentException("Fenwick index is outside the values array");
            }
            if (Math.abs((long) delta) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException("Fenwick delta is out of bounds");
            }
        }

        @Override
        public int[] values() {
            return values.clone();
        }
    }

    /** Validated lazy-segment operation. */
    public record LazyOperation(String kind, int left, int right, int delta) {
        public LazyOperation {
            Objects.requireNonNull(kind, "kind");
            if (!kind.equals("range-add") && !kind.equals("range-sum")) {
                throw new IllegalArgumentException("unsupported lazy operation: " + kind);
            }
            if (left < 0 || right < left || right >= MAX_VALUES) {
                // The input parser performs the length-aware check. This keeps
                // direct construction bounded without inventing a second size.
                throw new IllegalArgumentException("lazy operation range is malformed");
            }
            if (Math.abs((long) delta) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException("lazy operation delta is out of bounds");
            }
        }
    }

    /** Validated bounded sequence for lazy propagation. */
    public record LazyRequest(int[] values, List<LazyOperation> operations) {
        public LazyRequest {
            values = copyValues(values);
            Objects.requireNonNull(operations, "operations");
            if (operations.isEmpty() || operations.size() > MAX_OPERATIONS) {
                throw new IllegalArgumentException("lazy operation count is outside the bounded range");
            }
            for (LazyOperation operation : operations) {
                Objects.requireNonNull(operation, "operation");
                if (operation.left() < 0 || operation.right() >= values.length
                        || operation.right() < operation.left()) {
                    throw new IllegalArgumentException("lazy operation range is outside the values array");
                }
            }
            operations = List.copyOf(operations);
        }

        @Override
        public int[] values() {
            return values.clone();
        }
    }
}
