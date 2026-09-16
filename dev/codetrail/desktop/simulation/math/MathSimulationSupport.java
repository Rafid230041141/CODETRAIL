package dev.codetrail.desktop.simulation.math;

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

/** Small shared boundary helpers for the bounded mathematics simulations. */
final class MathSimulationSupport {
    static final int MAX_TRACE_STEPS = 2048;

    private MathSimulationSupport() {
    }

    static long readBoundedLong(
            JsonNode input,
            String type,
            String field,
            long minimum,
            long maximum) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        JsonNode valueNode = input.get(field);
        if (valueNode == null || !valueNode.isIntegralNumber() || !valueNode.canConvertToLong()) {
            throw new IllegalArgumentException(type + " " + field + " must be an integer");
        }
        long value = valueNode.longValue();
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    type + " " + field + " must be in the inclusive range " + minimum + ".." + maximum);
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
        return new TypedCell(
                Objects.requireNonNull(key, "key"),
                Objects.requireNonNull(value, "value"),
                Objects.requireNonNull(status, "status"));
    }

    static List<TypedCell> row(TypedCell... cells) {
        return List.of(cells);
    }
}
