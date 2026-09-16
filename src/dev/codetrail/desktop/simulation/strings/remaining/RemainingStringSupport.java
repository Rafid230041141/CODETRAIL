package dev.codetrail.desktop.simulation.strings.remaining;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.GraphState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Small bounded-boundary and snapshot helpers for the remaining string family. */
final class RemainingStringSupport {
    static final int MAX_SUFFIX_TEXT_LENGTH = 12;
    static final int MAX_HASH_TEXT_LENGTH = 32;
    static final int MAX_HASH_PATTERN_LENGTH = 16;
    static final int MAX_TRACE_STEPS = 4096;

    private RemainingStringSupport() {
    }

    static JsonNode requireExactObject(JsonNode input, String type, String... fields) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        Set<String> expected = Set.of(fields);
        Set<String> actual = new HashSet<>();
        input.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) {
            Set<String> missing = new HashSet<>(expected);
            missing.removeAll(actual);
            Set<String> unknown = new HashSet<>(actual);
            unknown.removeAll(expected);
            throw new IllegalArgumentException(
                    type + " input fields must be exactly " + expected
                            + "; missing=" + missing + ", unknown=" + unknown);
        }
        return input;
    }

    static String readLowercaseString(
            JsonNode object,
            String type,
            String field,
            int minimumLength,
            int maximumLength) {
        JsonNode valueNode = object.get(field);
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
                Objects.requireNonNull(narration, "narration"),
                Objects.requireNonNull(eventType, "eventType"),
                null);
    }

    static SimulationStep graphStep(
            List<Node> nodes,
            List<Edge> edges,
            List<Fact> facts,
            Set<String> activeNodeIds,
            Set<String> activeEdgeIds,
            int highlightedLine,
            String narration,
            StepEventType eventType) {
        GraphState state = new GraphState(
                nodes,
                edges,
                true,
                Objects.requireNonNull(facts, "facts"));
        return new SimulationStep(
                new SimulationSnapshot(
                        state,
                        Set.copyOf(Objects.requireNonNull(activeNodeIds, "activeNodeIds")),
                        Set.copyOf(Objects.requireNonNull(activeEdgeIds, "activeEdgeIds"))),
                highlightedLine,
                Objects.requireNonNull(narration, "narration"),
                Objects.requireNonNull(eventType, "eventType"),
                null);
    }

    static TypedCell cell(String key, String value, SnapshotStatus status) {
        return new TypedCell(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(value, "value"),
                Objects.requireNonNull(status, "status"));
    }

    static List<TypedCell> row(TypedCell... cells) {
        return List.of(cells);
    }

    static String formatKnown(int[] values, boolean[] known) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(known, "known");
        if (values.length != known.length) {
            throw new IllegalArgumentException("known flags must match values");
        }
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(known[index] ? Integer.toString(values[index]) : "?");
        }
        return result.append(']').toString();
    }

    static String boundedText(String text) {
        return text.isEmpty() ? "(empty)" : text;
    }

    static String quotedChar(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return "-";
        }
        return "'" + Character.toString(text.charAt(index)) + "'";
    }

    static String nodeId(int stateId) {
        return "q" + stateId;
    }
}
