package dev.codetrail.desktop.simulation.searching;

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
import java.util.List;

/** Bounded sequential search trace for the shared ARRAY renderer. */
public final class LinearSearchEngine implements SimulationEngine {
    public static final String TYPE = "LINEAR_SEARCH";
    public static final int MAX_ARRAY_LENGTH = SearchingSupport.MAX_ARRAY_LENGTH;
    public static final int MAX_ABS_VALUE = SearchingSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = SearchingSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_LOOP = 2;
    private static final int LINE_COMPARE = 3;
    private static final int LINE_RETURN_MATCH = 4;
    private static final int LINE_RETURN_MISS = 5;
    private static final int[] DEFAULT_ARRAY = {7, 4, 9, 4, 1};
    private static final int DEFAULT_TARGET = 4;
    private static final List<String> PSEUDOCODE = List.of(
            "linearSearch(a, target):",
            "    for index = 0; index < length(a); index++:",
            "        if a[index] == target:",
            "            return index",
            "    return -1");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int[] values = SearchingSupport.readBoundedArray(input, TYPE, false);
        int target = SearchingSupport.readBoundedTarget(input, TYPE);
        SearchingSupport.TraceBuilder trace = new SearchingSupport.TraceBuilder(
                values, TYPE, MAX_TRACE_STEPS);

        trace.add(
                SearchingSupport.statuses(values.length, SnapshotStatus.DEFAULT),
                -1,
                0,
                "Initialize linear search for target " + target,
                StepEventType.INITIALIZE,
                facts(target, -1, "pending", SnapshotStatus.ACTIVE));
        trace.add(
                SearchingSupport.statuses(values.length, SnapshotStatus.DEFAULT),
                -1,
                LINE_METHOD,
                "Scan the array from left to right",
                StepEventType.EXECUTE_LINE,
                facts(target, -1, "pending", SnapshotStatus.ACTIVE));

        for (int index = 0; index < values.length; index++) {
            SnapshotStatus[] activeStatuses = SearchingSupport.checkedPrefixStatuses(
                    values.length, index, index);
            trace.add(
                    activeStatuses,
                    index,
                    LINE_LOOP,
                    "Check the next index " + index,
                    StepEventType.EXECUTE_LINE,
                    facts(target, index, "checking", SnapshotStatus.ACTIVE));
            trace.add(
                    activeStatuses,
                    index,
                    LINE_COMPARE,
                    "Compare a[" + index + "] = " + values[index] + " with target " + target,
                    StepEventType.EXECUTE_LINE,
                    facts(target, index, "checking", SnapshotStatus.ACTIVE));

            if (values[index] == target) {
                SnapshotStatus[] foundStatuses = SearchingSupport.checkedPrefixStatuses(
                        values.length, index, -1);
                foundStatuses[index] = SnapshotStatus.DONE;
                trace.add(
                        foundStatuses,
                        index,
                        LINE_RETURN_MATCH,
                        "Match found at index " + index + "; return the first matching index",
                        StepEventType.EXECUTE_LINE,
                        facts(target, index, Integer.toString(index), SnapshotStatus.DONE));
                trace.add(
                        foundStatuses,
                        index,
                        0,
                        "Complete: first matching index = " + index,
                        StepEventType.COMPLETE,
                        facts(target, index, Integer.toString(index), SnapshotStatus.DONE));
                return trace.steps();
            }

            SnapshotStatus[] rejectedStatuses = SearchingSupport.checkedPrefixStatuses(
                    values.length, index + 1, -1);
            trace.add(
                    rejectedStatuses,
                    -1,
                    LINE_COMPARE,
                    "a[" + index + "] does not match; reject this position",
                    StepEventType.EXECUTE_LINE,
                    facts(target, index, "not found at index " + index, SnapshotStatus.ACTIVE));
        }

        SnapshotStatus[] missStatuses = SearchingSupport.statuses(values.length, SnapshotStatus.REJECTED);
        trace.add(
                missStatuses,
                -1,
                LINE_RETURN_MISS,
                "The scan is exhausted; return -1",
                StepEventType.EXECUTE_LINE,
                facts(target, -1, "-1", SnapshotStatus.DONE));
        trace.add(
                missStatuses,
                -1,
                0,
                "Complete: target " + target + " is absent, so the answer is -1",
                StepEventType.COMPLETE,
                facts(target, -1, "-1", SnapshotStatus.DONE));
        return trace.steps();
    }

    private static List<Fact> facts(int target, int index, String result, SnapshotStatus resultStatus) {
        SnapshotStatus indexStatus = index < 0
                ? SnapshotStatus.DEFAULT
                : resultStatus == SnapshotStatus.DONE ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        return List.of(
                new Fact("target", Integer.toString(target), SnapshotStatus.DEFAULT),
                new Fact("index", Integer.toString(index), indexStatus),
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
                "Linear Search",
                "O(n)",
                "O(1)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"array\":[7,4,9,4,1],\"target\":4}; array length must be 0 through "
                        + MAX_ARRAY_LENGTH + " and every integer, including target, must have absolute value at most "
                        + MAX_ABS_VALUE + ". The first matching index is returned, or -1 when absent.",
                PSEUDOCODE);
    }
}
