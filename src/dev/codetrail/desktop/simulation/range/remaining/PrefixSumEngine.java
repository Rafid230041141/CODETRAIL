package dev.codetrail.desktop.simulation.range.remaining;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.List;

/** Static inclusive range-sum trace backed by one cumulative prefix array. */
public final class PrefixSumEngine implements SimulationEngine {
    public static final String TYPE = "PREFIX_SUM";
    public static final int MIN_VALUES = RemainingRangeSupport.MIN_VALUES;
    public static final int MAX_VALUES = RemainingRangeSupport.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RemainingRangeSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = RemainingRangeSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_BUILD = 4;
    private static final int LINE_QUERY = 5;
    private static final int LINE_FORMULA = 6;
    private static final int LINE_RETURN = 7;
    private static final List<String> COLUMNS = List.of("prefix index", "source value", "prefix sum");
    private static final List<String> PSEUDOCODE = List.of(
            "buildPrefix(values):",
            "    prefix[0] = 0",
            "    for i = 0 .. n - 1:",
            "        prefix[i + 1] = prefix[i] + values[i]",
            "rangeSum(left, right):",
            "    answer = prefix[right + 1] - prefix[left]",
            "    return answer");
    private static final int[] DEFAULT_VALUES = {3, 1, 4, 1, 5, 9};
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        JsonNode object = RemainingRangeSupport.requireObject(input, TYPE);
        int[] values = RemainingRangeSupport.readValues(object, TYPE);
        int left = RemainingRangeSupport.readIndex(object.get("left"), values.length, TYPE + " left");
        int right = RemainingRangeSupport.readIndex(object.get("right"), values.length, TYPE + " right");
        RemainingRangeSupport.requireRange(left, right, values.length, TYPE + " query range");

        long[] prefix = new long[values.length + 1];
        boolean[] built = new boolean[prefix.length];
        SnapshotStatus[] statuses = RemainingRangeSupport.statuses(prefix.length, SnapshotStatus.DEFAULT);
        List<SimulationStep> steps = new ArrayList<>();

        prefix[0] = 0L;
        built[0] = true;
        statuses[0] = SnapshotStatus.ACTIVE;
        add(
                steps,
                values,
                prefix,
                built,
                statuses,
                0,
                "Initialize prefix[0] = 0 before accumulating the values",
                LINE_INITIALIZE,
                StepEventType.INITIALIZE,
                facts("initialize", left, right, prefix, built, "pending", "pending", SnapshotStatus.ACTIVE));

        statuses[0] = SnapshotStatus.DONE;
        add(
                steps,
                values,
                prefix,
                built,
                statuses,
                -1,
                "Build the cumulative prefix array from left to right",
                LINE_METHOD,
                StepEventType.EXECUTE_LINE,
                facts("build", left, right, prefix, built, "pending", "pending", SnapshotStatus.DEFAULT));

        for (int index = 0; index < values.length; index++) {
            statuses[index + 1] = SnapshotStatus.ACTIVE;
            prefix[index + 1] = prefix[index] + values[index];
            built[index + 1] = true;
            add(
                    steps,
                    values,
                    prefix,
                    built,
                    statuses,
                    index + 1,
                    "Set prefix[" + (index + 1) + "] = prefix[" + index + "] + values[" + index + "] = "
                            + prefix[index + 1],
                    LINE_BUILD,
                    StepEventType.EXECUTE_LINE,
                    facts("build", left, right, prefix, built, "pending", "pending", SnapshotStatus.ACTIVE));
            statuses[index + 1] = SnapshotStatus.DONE;
        }

        String formula = "prefix[" + (right + 1) + "] - prefix[" + left + "] = "
                + prefix[right + 1] + " - " + prefix[left] + " = " + (prefix[right + 1] - prefix[left]);
        long answer = prefix[right + 1] - prefix[left];
        statuses[left] = SnapshotStatus.ACTIVE;
        statuses[right + 1] = SnapshotStatus.ACTIVE;
        add(
                steps,
                values,
                prefix,
                built,
                statuses,
                right + 1,
                "Read the two prefix boundaries for inclusive range [" + left + "," + right + "]",
                LINE_QUERY,
                StepEventType.EXECUTE_LINE,
                facts("query", left, right, prefix, built, formula, Long.toString(answer), SnapshotStatus.ACTIVE));

        add(steps, values, prefix, built, statuses, right + 1,
                "Keep the prefix through " + right + ", subtract the prefix before " + left + ": " + formula,
                LINE_FORMULA, StepEventType.EXECUTE_LINE,
                facts("formula", left, right, prefix, built, formula, Long.toString(answer), SnapshotStatus.ACTIVE));

        java.util.Arrays.fill(statuses, SnapshotStatus.DONE);
        add(
                steps,
                values,
                prefix,
                built,
                statuses,
                -1,
                "Return range sum " + answer + " from " + formula + "",
                LINE_RETURN,
                StepEventType.EXECUTE_LINE,
                facts("return", left, right, prefix, built, formula, Long.toString(answer), SnapshotStatus.DONE));
        add(
                steps,
                values,
                prefix,
                built,
                statuses,
                -1,
                "Complete: inclusive range sum [" + left + "," + right + "] = " + answer,
                0,
                StepEventType.COMPLETE,
                facts("complete", left, right, prefix, built, formula, Long.toString(answer), SnapshotStatus.DONE));
        return List.copyOf(steps);
    }

    private static void add(
            List<SimulationStep> steps,
            int[] values,
            long[] prefix,
            boolean[] built,
            SnapshotStatus[] statuses,
            int focusIndex,
            String narration,
            int line,
            StepEventType eventType,
            List<Fact> facts) {
        List<List<TypedCell>> rows = new ArrayList<>(prefix.length);
        for (int prefixIndex = 0; prefixIndex < prefix.length; prefixIndex++) {
            String source = prefixIndex == 0 ? "-" : Integer.toString(values[prefixIndex - 1]);
            String prefixValue = built[prefixIndex] ? Long.toString(prefix[prefixIndex]) : "pending";
            SnapshotStatus status = statuses[prefixIndex];
            rows.add(List.of(
                    new TypedCell("prefix-index-" + prefixIndex, "P[" + prefixIndex + "]", status),
                    new TypedCell("source-value-" + prefixIndex, source, status),
                    new TypedCell("prefix-value-" + prefixIndex, prefixValue, status)));
        }
        RemainingRangeSupport.addTableStep(
                steps,
                TYPE,
                COLUMNS,
                rows,
                line,
                narration,
                eventType,
                facts);
    }

    private static List<Fact> facts(
            String phase,
            int left,
            int right,
            long[] prefix,
            boolean[] built,
            String formula,
            String answer,
            SnapshotStatus status) {
        return List.of(
                RemainingRangeSupport.fact("renderer", "prefix", SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("prefix-left", Integer.toString(left), status),
                RemainingRangeSupport.fact("prefix-right", Integer.toString(right + 1), status),
                RemainingRangeSupport.fact("prefix-left-value", built[left] ? Long.toString(prefix[left]) : "pending", status),
                RemainingRangeSupport.fact("prefix-right-value", built[right + 1] ? Long.toString(prefix[right + 1]) : "pending", status),
                RemainingRangeSupport.fact("teaching-equation", formula, status),
                RemainingRangeSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("left", Integer.toString(left), status),
                RemainingRangeSupport.fact("right", Integer.toString(right), status),
                RemainingRangeSupport.fact("range", "[" + left + "," + right + "]", status),
                RemainingRangeSupport.fact("prefix", RemainingRangeSupport.formatKnownAnswers(prefix, built), status),
                RemainingRangeSupport.fact("formula", formula, status),
                RemainingRangeSupport.fact("answer", answer, status),
                RemainingRangeSupport.fact("result", answer, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        input.put("left", 1);
        input.put("right", 4);
        return new SimulationMetadata(
                TYPE,
                "Prefix Sum",
                "O(n) build and O(1) inclusive range sum",
                "O(n)",
                RendererFamily.TABLE,
                input,
                "Enter JSON as {\"values\":[3,1,4,1,5,9],\"left\":1,\"right\":4}; values length must be "
                        + MIN_VALUES + ".." + MAX_VALUES + ", indices are inclusive zero-based, and each value has absolute value at most "
                        + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
