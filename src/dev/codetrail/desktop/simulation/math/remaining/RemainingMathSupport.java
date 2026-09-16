package dev.codetrail.desktop.simulation.math.remaining;

import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.GraphState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.NodeCoordinate;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Small immutable-state helpers owned by the remaining mathematics family. */
final class RemainingMathSupport {
    static final int MAX_TRACE_STEPS = 4096;

    private RemainingMathSupport() {
    }

    static SimulationStep tableStep(
            List<String> columns,
            List<List<TypedCell>> rows,
            List<Fact> facts,
            int highlightedLine,
            String narration,
            StepEventType eventType) {
        return new SimulationStep(
                new SimulationSnapshot(new TableState(columns, rows, facts), Set.of(), Set.of()),
                highlightedLine,
                narration,
                eventType,
                null);
    }

    static SimulationStep graphStep(
            List<Node> nodes,
            List<Edge> edges,
            Map<String, NodeCoordinate> coordinates,
            List<Fact> facts,
            Set<String> activeNodeIds,
            Set<String> activeEdgeIds,
            int highlightedLine,
            String narration,
            StepEventType eventType) {
        return new SimulationStep(
                new SimulationSnapshot(
                        new GraphState(nodes, edges, false, coordinates, facts),
                        activeNodeIds,
                        activeEdgeIds),
                highlightedLine,
                narration,
                eventType,
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

    static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    static long readLong(
            com.fasterxml.jackson.databind.JsonNode input,
            String type,
            String field,
            long minimum,
            long maximum) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        com.fasterxml.jackson.databind.JsonNode node = input.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong()) {
            throw new IllegalArgumentException(type + " " + field + " must be an integer");
        }
        long value = node.longValue();
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    type + " " + field + " must be in the inclusive range " + minimum + ".." + maximum);
        }
        return value;
    }

    static String formatDouble(double value) {
        if (!Double.isFinite(value)) {
            return "?";
        }
        if (Math.abs(value) < 0.0000000001) {
            return "0";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0000000001 && rounded >= Long.MIN_VALUE && rounded <= Long.MAX_VALUE) {
            return Long.toString((long) rounded);
        }
        String text = String.format(java.util.Locale.ROOT, "%.10f", value);
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, end);
    }

    static String formatLongList(List<Long> values) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(values.get(index));
        }
        return result.append(']').toString();
    }

    static String formatIntList(List<Integer> values) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(values.get(index));
        }
        return result.append(']').toString();
    }

    static String formatStringList(List<String> values) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(values.get(index));
        }
        return result.append(']').toString();
    }

    static String formatRows(List<? extends List<String>> rows) {
        StringBuilder result = new StringBuilder("[");
        for (int row = 0; row < rows.size(); row++) {
            if (row > 0) {
                result.append(", ");
            }
            result.append('[');
            List<String> values = rows.get(row);
            for (int column = 0; column < values.size(); column++) {
                if (column > 0) {
                    result.append(", ");
                }
                result.append(values.get(column));
            }
            result.append(']');
        }
        return result.append(']').toString();
    }

    static Set<String> singletonId(String id) {
        return Set.of(Objects.requireNonNull(id, "id"));
    }

}
