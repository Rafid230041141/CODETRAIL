package dev.codetrail.desktop.simulation.sorting;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.ArrayState;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Package-local boundaries shared only by the sorting simulation engines. */
final class SortingSupport {
    static final int MAX_ARRAY_LENGTH = 24;
    static final int MAX_ABS_VALUE = 999;

    private SortingSupport() {
    }

    static int[] readBoundedArray(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        JsonNode arrayNode = input.get("array");
        if (arrayNode == null || !arrayNode.isArray()) {
            throw new IllegalArgumentException(type + " input array must be a JSON array");
        }
        if (arrayNode.size() > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException(
                    type + " array length must be at most " + MAX_ARRAY_LENGTH);
        }
        int[] values = new int[arrayNode.size()];
        for (int index = 0; index < arrayNode.size(); index++) {
            JsonNode valueNode = arrayNode.get(index);
            if (valueNode == null || !valueNode.isIntegralNumber() || !valueNode.canConvertToInt()) {
                throw new IllegalArgumentException(type + " array values must be bounded integers");
            }
            int value = valueNode.intValue();
            if (Math.abs((long) value) > MAX_ABS_VALUE) {
                throw new IllegalArgumentException(
                        type + " array values must have absolute value at most " + MAX_ABS_VALUE);
            }
            values[index] = value;
        }
        return values;
    }

    static Element[] elements(int[] values) {
        Objects.requireNonNull(values, "values");
        Element[] elements = new Element[values.length];
        for (int index = 0; index < values.length; index++) {
            elements[index] = new Element(Integer.toString(index), values[index]);
        }
        return elements;
    }

    static SnapshotStatus[] statuses(int size, SnapshotStatus status) {
        SnapshotStatus[] statuses = new SnapshotStatus[size];
        Arrays.fill(statuses, Objects.requireNonNull(status, "status"));
        return statuses;
    }

    static SnapshotStatus[] rangeStatuses(int size, int low, int high, SnapshotStatus status) {
        SnapshotStatus[] statuses = statuses(size, SnapshotStatus.DEFAULT);
        if (low <= high) {
            Arrays.fill(statuses, low, high + 1, Objects.requireNonNull(status, "status"));
        }
        return statuses;
    }

    static SnapshotStatus[] activePositions(int size, int... positions) {
        SnapshotStatus[] statuses = statuses(size, SnapshotStatus.DEFAULT);
        for (int position : positions) {
            if (position >= 0 && position < size) {
                statuses[position] = SnapshotStatus.ACTIVE;
            }
        }
        return statuses;
    }

    static SnapshotStatus[] donePrefixWithActiveSuffix(int size, int endExclusive) {
        SnapshotStatus[] statuses = statuses(size, SnapshotStatus.ACTIVE);
        if (endExclusive > 0) {
            Arrays.fill(statuses, 0, Math.min(size, endExclusive), SnapshotStatus.DONE);
        }
        return statuses;
    }

    static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    static List<Fact> withFacts(List<Fact> facts, Fact... additional) {
        List<Fact> combined = new ArrayList<>(facts);
        combined.addAll(List.of(additional));
        return List.copyOf(combined);
    }

    static String formatElement(Element element) {
        return element.value + "@" + element.key;
    }

    static String formatElements(Iterable<Element> elements) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Element element : elements) {
            joiner.add(element == null ? "_" : formatElement(element));
        }
        return joiner.toString();
    }

    static String formatElements(Element[] elements) {
        return formatElements(Arrays.asList(elements));
    }

    static String formatFrequency(int[] frequencies, int minimum, int maximum) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (int value = minimum; value <= maximum; value++) {
            int count = frequencies[value - minimum];
            if (count > 0) {
                joiner.add(value + "=" + count);
            }
        }
        return joiner.toString();
    }

    static String formatPositions(int[] positions, int[] present, int minimum) {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(present, "present");
        if (positions.length != present.length) {
            throw new IllegalArgumentException("positions and present arrays must have equal length");
        }
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (int offset = 0; offset < positions.length; offset++) {
            if (present[offset] > 0) {
                joiner.add((minimum + offset) + "->" + positions[offset]);
            }
        }
        return joiner.toString();
    }

    static String formatBuckets(List<? extends List<Element>> buckets) {
        StringJoiner joiner = new StringJoiner("; ");
        for (int index = 0; index < buckets.size(); index++) {
            joiner.add(index + ":" + formatElements(buckets.get(index)));
        }
        return joiner.toString();
    }

    static String formatOutput(Element[] output) {
        return formatElements(output);
    }

    static record Element(String key, int value) {
        Element {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("element key must be nonblank");
            }
        }
    }

    /** Mutable trace facade that always snapshots the current primary array. */
    static final class Trace {
        private final Element[] elements;
        private final int maxSteps;
        private final List<SimulationStep> steps = new ArrayList<>();

        Trace(Element[] elements, int maxSteps) {
            this.elements = Objects.requireNonNull(elements, "elements");
            if (maxSteps <= 0) {
                throw new IllegalArgumentException("maxSteps must be positive");
            }
            this.maxSteps = maxSteps;
        }

        List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        void add(
                SnapshotStatus[] statuses,
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType) {
            add(statuses, focusIndex, highlightedLine, narration, eventType, List.of());
        }

        void add(
                SnapshotStatus[] statuses,
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            Objects.requireNonNull(statuses, "statuses");
            if (statuses.length != elements.length) {
                throw new IllegalArgumentException("status count must match array length");
            }
            Objects.requireNonNull(narration, "narration");
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");
            if (steps.size() >= maxSteps) {
                throw new IllegalStateException("sorting trace exceeded bounded step limit");
            }
            List<TypedCell> cells = new ArrayList<>(elements.length);
            for (int index = 0; index < elements.length; index++) {
                Element element = elements[index];
                cells.add(new TypedCell(element.key, Integer.toString(element.value), statuses[index]));
            }
            ArrayState state = new ArrayState(cells, focusIndex, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }
    }
}
