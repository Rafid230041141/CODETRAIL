package dev.codetrail.desktop.simulation.searching;

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

/** Package-local validation and trace helpers for the searching engines. */
final class SearchingSupport {
    static final int MAX_ARRAY_LENGTH = 24;
    static final int MAX_ABS_VALUE = 999;
    static final int MAX_MACHINES = 24;
    static final long MAX_TARGET = 1_000_000_000L;
    static final int MAX_TRACE_STEPS = 2048;

    private SearchingSupport() {
    }

    static int[] readBoundedArray(JsonNode input, String type, boolean requireSorted) {
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
            if (requireSorted && index > 0 && values[index - 1] > value) {
                throw new IllegalArgumentException(type + " input array must be sorted in nondecreasing order");
            }
            values[index] = value;
        }
        return values;
    }

    static int readBoundedTarget(JsonNode input, String type) {
        JsonNode targetNode = input == null ? null : input.get("target");
        if (targetNode == null || !targetNode.isIntegralNumber() || !targetNode.canConvertToInt()) {
            throw new IllegalArgumentException(type + " target must be a bounded integer");
        }
        int target = targetNode.intValue();
        if (Math.abs((long) target) > MAX_ABS_VALUE) {
            throw new IllegalArgumentException(
                    type + " target must have absolute value at most " + MAX_ABS_VALUE);
        }
        return target;
    }

    static MachineInput readMachineInput(JsonNode input, String type) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        JsonNode machinesNode = input.get("machines");
        if (machinesNode == null || !machinesNode.isArray() || machinesNode.isEmpty()) {
            throw new IllegalArgumentException(type + " machines must be a nonempty JSON array");
        }
        if (machinesNode.size() > MAX_MACHINES) {
            throw new IllegalArgumentException(
                    type + " machines count must be at most " + MAX_MACHINES);
        }

        long[] machineTimes = new long[machinesNode.size()];
        long minimum = Long.MAX_VALUE;
        for (int index = 0; index < machinesNode.size(); index++) {
            JsonNode machineNode = machinesNode.get(index);
            if (machineNode == null || !machineNode.isIntegralNumber() || !machineNode.canConvertToLong()) {
                throw new IllegalArgumentException(type + " machine times must be positive 64-bit integers");
            }
            long machineTime = machineNode.longValue();
            if (machineTime <= 0L) {
                throw new IllegalArgumentException(type + " machine times must be positive");
            }
            machineTimes[index] = machineTime;
            minimum = Math.min(minimum, machineTime);
        }

        JsonNode targetNode = input.get("target");
        if (targetNode == null || !targetNode.isIntegralNumber() || !targetNode.canConvertToLong()) {
            throw new IllegalArgumentException(type + " target must be a positive bounded count");
        }
        long target = targetNode.longValue();
        if (target <= 0L || target > MAX_TARGET) {
            throw new IllegalArgumentException(
                    type + " target must be in the inclusive range 1.." + MAX_TARGET);
        }

        // The fastest machine can produce the target in minimum * target time.
        // Reject an input whose guaranteed upper bound cannot be represented as
        // a signed long, so every subsequent interval operation is defined.
        if (minimum > Long.MAX_VALUE / target) {
            throw new IllegalArgumentException(
                    type + " minimum machine time multiplied by target exceeds the supported long range");
        }
        return new MachineInput(machineTimes, target, minimum * target);
    }

    static SnapshotStatus[] statuses(int size, SnapshotStatus status) {
        SnapshotStatus[] statuses = new SnapshotStatus[size];
        Arrays.fill(statuses, Objects.requireNonNull(status, "status"));
        return statuses;
    }

    static SnapshotStatus[] searchWindowStatuses(int size, int low, int high) {
        SnapshotStatus[] statuses = statuses(size, SnapshotStatus.DEFAULT);
        if (low > 0) {
            Arrays.fill(statuses, 0, Math.min(low, size), SnapshotStatus.REJECTED);
        }
        if (high < size) {
            Arrays.fill(statuses, Math.max(0, high + 1), size, SnapshotStatus.REJECTED);
        }
        // high is outside the unexamined window but remains a possible lower-bound
        // answer. Do not mark that retained candidate as rejected.
        return statuses;
    }

    static SnapshotStatus[] checkedPrefixStatuses(int size, int checkedExclusive, int activeIndex) {
        SnapshotStatus[] statuses = statuses(size, SnapshotStatus.DEFAULT);
        if (checkedExclusive > 0) {
            Arrays.fill(statuses, 0, Math.min(checkedExclusive, size), SnapshotStatus.REJECTED);
        }
        if (activeIndex >= 0 && activeIndex < size) {
            statuses[activeIndex] = SnapshotStatus.ACTIVE;
        }
        return statuses;
    }

    static final class TraceBuilder {
        private final String type;
        private final String[] keys;
        private final String[] values;
        private final int maxSteps;
        private final List<SimulationStep> steps = new ArrayList<>();

        TraceBuilder(int[] values, String type, int maxSteps) {
            this.type = requireType(type);
            this.values = new String[Objects.requireNonNull(values, "values").length];
            this.keys = new String[values.length];
            for (int index = 0; index < values.length; index++) {
                this.keys[index] = Integer.toString(index);
                this.values[index] = Integer.toString(values[index]);
            }
            this.maxSteps = requireMaxSteps(maxSteps);
        }

        TraceBuilder(long[] values, String type, int maxSteps) {
            this.type = requireType(type);
            this.values = new String[Objects.requireNonNull(values, "values").length];
            this.keys = new String[values.length];
            for (int index = 0; index < values.length; index++) {
                this.keys[index] = "machine-" + index;
                this.values[index] = Long.toString(values[index]);
            }
            this.maxSteps = requireMaxSteps(maxSteps);
        }

        List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        void add(
                SnapshotStatus[] statuses,
                int focusIndex,
                int highlightedLine,
                String narration,
                StepEventType eventType,
                List<Fact> facts) {
            Objects.requireNonNull(statuses, "statuses");
            if (statuses.length != values.length) {
                throw new IllegalArgumentException("status count must match array length");
            }
            Objects.requireNonNull(narration, "narration");
            Objects.requireNonNull(eventType, "eventType");
            Objects.requireNonNull(facts, "facts");
            if (steps.size() >= maxSteps) {
                throw new IllegalStateException(type + " trace exceeded bounded step limit");
            }

            List<TypedCell> cells = new ArrayList<>(values.length);
            for (int index = 0; index < values.length; index++) {
                cells.add(new TypedCell(keys[index], values[index],
                        Objects.requireNonNull(statuses[index], "status")));
            }
            ArrayState state = new ArrayState(cells, focusIndex, facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, Set.of(), Set.of()),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        private static String requireType(String type) {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("type must be nonblank");
            }
            return type;
        }

        private static int requireMaxSteps(int maxSteps) {
            if (maxSteps <= 0) {
                throw new IllegalArgumentException("maxSteps must be positive");
            }
            return maxSteps;
        }
    }

    record MachineInput(long[] machineTimes, long target, long upperBound) {
        MachineInput {
            machineTimes = machineTimes.clone();
            if (machineTimes.length == 0 || target <= 0L || upperBound <= 0L) {
                throw new IllegalArgumentException("machine input must have positive values and bound");
            }
        }

        @Override
        public long[] machineTimes() {
            return machineTimes.clone();
        }
    }
}
