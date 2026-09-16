package dev.codetrail.desktop.simulation.searching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.Fact;
import java.util.List;

/** Lower-bound binary search trace that returns the first matching index. */
public final class BinarySearchEngine implements SimulationEngine {
    public static final String TYPE = "BINARY_SEARCH";
    public static final int MAX_ARRAY_LENGTH = SearchingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SearchingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = SearchingSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_BOUNDS = 2;
    private static final int LINE_WHILE = 3;
    private static final int LINE_MIDDLE = 4;
    private static final int LINE_COMPARE = 5;
    private static final int LINE_LOW = 6;
    private static final int LINE_ELSE = 7;
    private static final int LINE_HIGH = 8;
    private static final int LINE_CHECK_CANDIDATE = 9;
    private static final int LINE_RETURN_MATCH = 10;
    private static final int LINE_RETURN_MISS = 11;
    private static final int[] DEFAULT_ARRAY = {1, 2, 2, 4, 7, 9};
    private static final int DEFAULT_TARGET = 2;
    private static final List<String> PSEUDOCODE = List.of(
            "lower_bound(a, target):",
            "    low = 0; high = length(a)",
            "    while low < high:",
            "        middle = low + floor((high - low) / 2)",
            "        if a[middle] < target:",
            "            low = middle + 1",
            "        else:",
            "            high = middle",
            "    if low < length(a) and a[low] == target:",
            "        return low",
            "    return -1");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int[] values = SearchingSupport.readBoundedArray(input, TYPE, true);
        int target = SearchingSupport.readBoundedTarget(input, TYPE);
        SearchingSupport.TraceBuilder trace = new SearchingSupport.TraceBuilder(
                values, TYPE, MAX_TRACE_STEPS);

        int low = 0;
        int high = values.length;
        int middle = -1;
        trace.add(
                SearchingSupport.searchWindowStatuses(values.length, low, high),
                -1,
                0,
                "Initialize lower_bound search for target " + target,
                StepEventType.INITIALIZE,
                facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
        trace.add(
                SearchingSupport.searchWindowStatuses(values.length, low, high),
                -1,
                LINE_METHOD,
                "Search the sorted array for the first matching position",
                StepEventType.EXECUTE_LINE,
                facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
        trace.add(
                SearchingSupport.searchWindowStatuses(values.length, low, high),
                -1,
                LINE_BOUNDS,
                "Set the half-open search window to low = 0 and high = " + high,
                StepEventType.EXECUTE_LINE,
                facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));

        while (low < high) {
            trace.add(
                    SearchingSupport.searchWindowStatuses(values.length, low, high),
                    -1,
                    LINE_WHILE,
                    "Check the shrinking window [" + low + ", " + high + ")",
                    StepEventType.EXECUTE_LINE,
                    facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
            middle = low + (high - low) / 2;
            trace.add(
                    comparisonStatuses(values.length, low, high, middle),
                    middle,
                    LINE_MIDDLE,
                    "Choose middle = " + middle + " without overflowing the index range",
                    StepEventType.EXECUTE_LINE,
                    facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
            trace.add(
                    comparisonStatuses(values.length, low, high, middle),
                    middle,
                    LINE_COMPARE,
                    "Compare a[" + middle + "] = " + values[middle] + " with target " + target,
                    StepEventType.EXECUTE_LINE,
                    facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));

            if (values[middle] < target) {
                low = middle + 1;
                trace.add(
                        SearchingSupport.searchWindowStatuses(values.length, low, high),
                        low < values.length ? low : -1,
                        LINE_LOW,
                        "a[middle] is smaller, so move low to " + low,
                        StepEventType.EXECUTE_LINE,
                        facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
            } else {
                trace.add(
                        comparisonStatuses(values.length, low, high, middle),
                        middle,
                        LINE_ELSE,
                        "a[middle] is at least the target; keep the left side for lower_bound",
                        StepEventType.EXECUTE_LINE,
                        facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
                high = middle;
                trace.add(
                        SearchingSupport.searchWindowStatuses(values.length, low, high),
                        high,
                        LINE_HIGH,
                        "Keep index " + high + " as a candidate; search [" + low + ", " + high + ") for an earlier match",
                        StepEventType.EXECUTE_LINE,
                        facts(target, low, middle, high, "pending", SnapshotStatus.ACTIVE));
            }
        }

        SnapshotStatus[] candidateStatuses = SearchingSupport.searchWindowStatuses(
                values.length, low, high);
        if (low < values.length) {
            candidateStatuses[low] = SnapshotStatus.ACTIVE;
        }
        trace.add(
                candidateStatuses,
                low < values.length ? low : -1,
                LINE_CHECK_CANDIDATE,
                "Check lower_bound candidate at index " + low,
                StepEventType.EXECUTE_LINE,
                facts(target, low, middle, high, "checking candidate", SnapshotStatus.ACTIVE));

        if (low < values.length && values[low] == target) {
            candidateStatuses[low] = SnapshotStatus.DONE;
            trace.add(
                    candidateStatuses,
                    low,
                    LINE_RETURN_MATCH,
                    "Return the first matching index " + low,
                    StepEventType.EXECUTE_LINE,
                    facts(target, low, middle, high, Integer.toString(low), SnapshotStatus.DONE));
            trace.add(
                    candidateStatuses,
                    low,
                    0,
                    "Complete: first matching index = " + low,
                    StepEventType.COMPLETE,
                    facts(target, low, middle, high, Integer.toString(low), SnapshotStatus.DONE));
        } else {
            if (low < values.length) {
                candidateStatuses[low] = SnapshotStatus.REJECTED;
            }
            trace.add(
                    candidateStatuses,
                    -1,
                    LINE_RETURN_MISS,
                    "The lower_bound candidate is not equal to the target; return -1",
                    StepEventType.EXECUTE_LINE,
                    facts(target, low, middle, high, "-1", SnapshotStatus.DONE));
            trace.add(
                    candidateStatuses,
                    -1,
                    0,
                    "Complete: target " + target + " is absent, so the answer is -1",
                    StepEventType.COMPLETE,
                    facts(target, low, middle, high, "-1", SnapshotStatus.DONE));
        }
        return trace.steps();
    }

    private static SnapshotStatus[] comparisonStatuses(int size, int low, int high, int middle) {
        SnapshotStatus[] statuses = SearchingSupport.searchWindowStatuses(size, low, high);
        if (middle >= low && middle < high) {
            statuses[middle] = SnapshotStatus.ACTIVE;
        }
        return statuses;
    }

    private static List<Fact> facts(
            int target,
            int low,
            int middle,
            int high,
            String result,
            SnapshotStatus resultStatus) {
        SnapshotStatus boundStatus = resultStatus == SnapshotStatus.DONE
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        int visibleMiddle = middle >= low && middle < high ? middle : -1;
        return List.of(
                new Fact("target", Integer.toString(target), SnapshotStatus.DEFAULT),
                new Fact("low", Integer.toString(low), boundStatus),
                new Fact("middle", Integer.toString(visibleMiddle), boundStatus),
                new Fact("high", Integer.toString(high), boundStatus),
                new Fact("candidate", Integer.toString(high), boundStatus),
                new Fact("result", result, resultStatus));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode array = defaultInput.putArray("array");
        for (int value : DEFAULT_ARRAY) {
            array.add(value);
        }
        defaultInput.put("target", DEFAULT_TARGET);
        return new SimulationMetadata(
                TYPE,
                "Binary Search",
                "O(log n)",
                "O(1)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[1,2,2,4,7,9],\"target\":2}; array length must be 0 through "
                        + MAX_ARRAY_LENGTH + ", values must be bounded integers in nondecreasing order, and target must have absolute value at most "
                        + MAX_ABS_VALUE + ". The first matching index is returned, or -1 when absent.",
                PSEUDOCODE);
    }
}
