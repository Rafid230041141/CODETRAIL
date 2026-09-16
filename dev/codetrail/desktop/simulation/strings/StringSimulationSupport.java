package dev.codetrail.desktop.simulation.strings;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Small shared boundary helpers for the bounded string simulations. */
final class StringSimulationSupport {
    static final int MAX_TEXT_LENGTH = 32;
    static final int MAX_PATTERN_LENGTH = 16;
    static final int MAX_TRACE_STEPS = 2048;

    private StringSimulationSupport() {
    }

    static String readLowercaseString(
            JsonNode input,
            String type,
            String field,
            int minimumLength,
            int maximumLength) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        JsonNode valueNode = input.get(field);
        if (valueNode == null || !valueNode.isTextual()) {
            throw new IllegalArgumentException(type + " " + field + " must be a lowercase ASCII string");
        }
        String value = valueNode.textValue();
        if (value.length() < minimumLength || value.length() > maximumLength) {
            throw new IllegalArgumentException(
                    type + " " + field + " length must be " + minimumLength + ".." + maximumLength);
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character < 'a' || character > 'z') {
                throw new IllegalArgumentException(
                        type + " " + field + " must contain only lowercase ASCII letters");
            }
        }
        return value;
    }

    static SimulationStep tableStep(
            List<String> columns,
            List<List<TypedCell>> rows,
            List<Fact> facts,
            int highlightedLine,
            String narration,
            StepEventType eventType) {
        TableState state = new TableState(columns, rows, facts);
        return new SimulationStep(
                new SimulationSnapshot(state, Set.of(), Set.of()),
                highlightedLine,
                narration,
                eventType,
                null);
    }

    static TypedCell cell(String key, String value, SnapshotStatus status) {
        return new TypedCell(key, Objects.requireNonNull(value, "value"), Objects.requireNonNull(status, "status"));
    }

    static List<TypedCell> row(TypedCell... cells) {
        return List.of(cells);
    }

    static String formatKnownArray(int[] values, boolean[] known) {
        StringBuilder formatted = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                formatted.append(", ");
            }
            formatted.append(known[index] ? Integer.toString(values[index]) : "?");
        }
        return formatted.append(']').toString();
    }

    static String formatArray(int[] values) {
        StringBuilder formatted = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                formatted.append(", ");
            }
            formatted.append(values[index]);
        }
        return formatted.append(']').toString();
    }

    static String formatMatches(List<Integer> matches) {
        return matches.toString();
    }

    static String quotedChar(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return "-";
        }
        return "'" + Character.toString(text.charAt(index)) + "'";
    }

    static String alignment(String text, String pattern, int textIndex, int patternIndex) {
        if (text.isEmpty() || pattern.isEmpty() || textIndex < 0 || patternIndex < 0) {
            return "none";
        }
        int start = textIndex - patternIndex;
        int end = start + pattern.length() - 1;
        return "text[" + start + ".." + end + "] with pattern[0.." + (pattern.length() - 1) + "]";
    }

    static String boundedText(String text) {
        return text.isEmpty() ? "(empty)" : text;
    }

}
