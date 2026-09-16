package dev.codetrail.desktop.simulation.paradigms;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationState;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Explicit factory for the four algorithmic-paradigm simulation engines. */
public final class ParadigmEngines {
    private ParadigmEngines() {
    }

    public static List<SimulationEngine> all() {
        return List.of(
                new DivideConquerEngine(),
                new NQueensEngine(),
                new GreedyActivityEngine(),
                new DynamicProgrammingEngine());
    }
}

/** Small package-private helpers shared by the paradigm producers. */
final class ParadigmSupport {
    private ParadigmSupport() {
    }

    static JsonNode requireObject(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        return input;
    }

    static JsonNode requireField(JsonNode input, String type, String field) {
        JsonNode value = input.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException(type + " input requires " + field);
        }
        return value;
    }

    static String requireAlgorithm(JsonNode input, String type, String expected) {
        JsonNode algorithm = requireField(input, type, "algorithm");
        if (!algorithm.isTextual() || !expected.equals(algorithm.textValue())) {
            throw new IllegalArgumentException(
                    type + " input algorithm must be exactly " + expected);
        }
        return expected;
    }

    static int boundedInt(JsonNode value, String description, int minimum, int maximum) {
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException(description + " must be an integer");
        }
        int result = value.intValue();
        if (result < minimum || result > maximum) {
            throw new IllegalArgumentException(
                    description + " must be in the inclusive range " + minimum + ".." + maximum);
        }
        return result;
    }

    static int[] intArray(JsonNode input, String type, String field, int maxLength, int maxAbsValue) {
        JsonNode values = requireField(input, type, field);
        if (!values.isArray() || values.size() == 0 || values.size() > maxLength) {
            throw new IllegalArgumentException(
                    type + " " + field + " length must be in the inclusive range 1.." + maxLength);
        }
        int[] result = new int[values.size()];
        for (int index = 0; index < values.size(); index++) {
            JsonNode value = values.get(index);
            if (!value.isIntegralNumber() || !value.canConvertToInt()) {
                throw new IllegalArgumentException(type + " " + field + " values must be integers");
            }
            int number = value.intValue();
            if (number < -maxAbsValue || number > maxAbsValue) {
                throw new IllegalArgumentException(
                        type + " " + field + " values must have absolute value at most " + maxAbsValue);
            }
            result[index] = number;
        }
        return result;
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

    static void add(
            List<SimulationStep> steps,
            SimulationState state,
            Set<String> activeNodeIds,
            Set<String> activeEdgeIds,
            int line,
            String narration,
            StepEventType eventType,
            String type,
            int maxTraceSteps) {
        if (steps.size() >= maxTraceSteps) {
            throw new IllegalStateException(type + " trace exceeded bounded step limit");
        }
        steps.add(new SimulationStep(
                new SimulationSnapshot(state, activeNodeIds, activeEdgeIds),
                line,
                narration,
                eventType,
                null));
    }

    static String formatInts(int[] values) {
        return Arrays.toString(values);
    }

    static String formatIntegerList(List<Integer> values) {
        return values.toString();
    }

    static String formatNestedInts(List<int[]> values) {
        List<String> formatted = new ArrayList<>(values.size());
        for (int[] value : values) {
            formatted.add(Arrays.toString(value));
        }
        return formatted.toString();
    }
}
