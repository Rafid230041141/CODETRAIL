package dev.codetrail.desktop.simulation.structures.trees;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Small parsing and formatting helpers shared by the bounded tree family. */
final class OptionalSmallHelper {
    private OptionalSmallHelper() {
    }

    static ObjectNode requireObject(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        return (ObjectNode) input;
    }

    static int readInt(JsonNode node, String field, String type) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(type + " " + field + " must be an integer");
        }
        return node.intValue();
    }

    static String readText(JsonNode node, String field, String type) {
        if (node == null || !node.isTextual() || node.textValue().isBlank()) {
            throw new IllegalArgumentException(type + " " + field + " must be nonblank text");
        }
        return node.textValue();
    }

    static void requireExactFields(ObjectNode object, Set<String> allowed, String context) {
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowed.contains(field)) {
                throw new IllegalArgumentException(context + " has unsupported field: " + field);
            }
        }
    }

    static List<Integer> readIntList(
            JsonNode node,
            String field,
            String type,
            int minimumSize,
            int maximumSize,
            int maximumAbsoluteValue) {
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException(type + " " + field + " must be a JSON array");
        }
        if (node.size() < minimumSize || node.size() > maximumSize) {
            throw new IllegalArgumentException(type + " " + field + " length must be "
                    + minimumSize + ".." + maximumSize);
        }
        List<Integer> values = new ArrayList<>(node.size());
        for (int index = 0; index < node.size(); index++) {
            int value = readInt(node.get(index), field + "[" + index + "]", type);
            if (Math.abs((long) value) > maximumAbsoluteValue) {
                throw new IllegalArgumentException(type + " " + field + " values must have absolute value at most "
                        + maximumAbsoluteValue);
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    static String readLowercase(
            JsonNode node,
            String field,
            String type,
            int minimumLength,
            int maximumLength) {
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException(type + " " + field + " must be a lowercase ASCII string");
        }
        String value = node.textValue();
        if (value.length() < minimumLength || value.length() > maximumLength) {
            throw new IllegalArgumentException(type + " " + field + " length must be "
                    + minimumLength + ".." + maximumLength);
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 'a' || character > 'z') {
                throw new IllegalArgumentException(type + " " + field
                        + " must contain only lowercase ASCII letters");
            }
        }
        return value;
    }

    static List<String> readLowercaseList(
            JsonNode node,
            String field,
            String type,
            int minimumSize,
            int maximumSize,
            int maximumWordLength,
            int maximumTotalLength) {
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException(type + " " + field + " must be a JSON array");
        }
        if (node.size() < minimumSize || node.size() > maximumSize) {
            throw new IllegalArgumentException(type + " " + field + " length must be "
                    + minimumSize + ".." + maximumSize);
        }
        List<String> words = new ArrayList<>(node.size());
        int totalLength = 0;
        for (int index = 0; index < node.size(); index++) {
            String word = readLowercase(node.get(index), field + "[" + index + "]", type, 1, maximumWordLength);
            totalLength += word.length();
            if (totalLength > maximumTotalLength) {
                throw new IllegalArgumentException(type + " " + field
                        + " total character length must be at most " + maximumTotalLength);
            }
            words.add(word);
        }
        return List.copyOf(words);
    }

    static TypedCell cell(String key, String value, SnapshotStatus status) {
        return new TypedCell(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(value, "value"),
                Objects.requireNonNull(status, "status"));
    }

    static String formatInts(List<Integer> values) {
        return values.toString();
    }

    static String formatStrings(List<String> values) {
        return values.toString();
    }

    static String formatMap(List<String> keys, List<Integer> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("map key/value lengths differ");
        }
        StringBuilder formatted = new StringBuilder("{");
        for (int index = 0; index < keys.size(); index++) {
            if (index > 0) {
                formatted.append(", ");
            }
            formatted.append(keys.get(index)).append('=').append(values.get(index));
        }
        return formatted.append('}').toString();
    }

    static <T> Set<T> linkedSet() {
        return new LinkedHashSet<>();
    }
}
