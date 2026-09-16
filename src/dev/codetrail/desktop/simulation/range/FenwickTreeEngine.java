package dev.codetrail.desktop.simulation.range;

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
import java.util.ArrayList;
import java.util.List;

/** Fenwick tree (BIT) build, point update, and inclusive prefix-query trace. */
public final class FenwickTreeEngine implements SimulationEngine {
    public static final String TYPE = "FENWICK_TREE";
    public static final int MIN_VALUES = RangeInput.MIN_VALUES;
    public static final int MAX_VALUES = RangeInput.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RangeInput.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = RangeTreeSupport.MAX_TRACE_STEPS;

    private static final int LINE_BUILD = 1;
    private static final int LINE_ADD = 2;
    private static final int LINE_ADD_HEADER = 3;
    private static final int LINE_ADD_POSITION = 4;
    private static final int LINE_ADD_CELL = 5;
    private static final int LINE_QUERY_HEADER = 6;
    private static final int LINE_QUERY_POSITION = 7;
    private static final int LINE_QUERY_CELL = 8;
    private static final int LINE_QUERY_RETURN = 9;
    private static final int LINE_RETURN = 10;

    private static final int[] DEFAULT_VALUES = {2, 1, 3, 5, 4};
    private static final List<String> PSEUDOCODE = List.of(
            "build(values):",
            "    for each index i: add(values[i])",
            "add(index, delta):",
            "    pos = index + 1",
            "    while pos <= n: bit[pos] += delta; pos += lowbit(pos)",
            "prefixSum(index):",
            "    pos = index + 1; answer = 0",
            "    while pos > 0: answer += bit[pos]; pos -= lowbit(pos)",
            "return answer",
            "return updated values and prefix answer");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        RangeInput.FenwickRequest request = RangeInput.parseFenwick(input);
        int[] initialValues = request.values();
        RangeTreeSupport.FenwickModel model = new RangeTreeSupport.FenwickModel(initialValues);
        RangeTreeSupport.FenwickTraceBuilder trace = new RangeTreeSupport.FenwickTraceBuilder(model);

        trace.add(
                0,
                "Initialize a Fenwick tree for " + request.operation() + " over " + initialValues.length + " value(s)",
                StepEventType.INITIALIZE,
                facts(model, request.operation(), -1, -1, "initialize", "pending", "pending", SnapshotStatus.ACTIVE));
        build(initialValues, request.operation(), model, trace);

        trace.resetStatuses();
        String result;
        if (request.operation().equals("update")) {
            result = update(request, model, trace);
        } else {
            result = query(request, model, trace);
        }

        for (int row = 0; row < model.length(); row++) {
            trace.status(row, SnapshotStatus.DONE);
        }
        trace.add(
                LINE_RETURN,
                "Return Fenwick " + request.operation() + " result " + result,
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), request.index(), request.index() + 1,
                        "return", result, result, SnapshotStatus.DONE));
        trace.add(
                0,
                "Complete: Fenwick " + request.operation() + " result = " + result,
                StepEventType.COMPLETE,
                facts(model, request.operation(), request.index(), request.index() + 1,
                        "complete", result, result, SnapshotStatus.DONE));
        return trace.steps();
    }

    private static void build(
            int[] initialValues,
            String operation,
            RangeTreeSupport.FenwickModel model,
            RangeTreeSupport.FenwickTraceBuilder trace) {
        for (int zeroIndex = 0; zeroIndex < initialValues.length; zeroIndex++) {
            int oneIndex = zeroIndex + 1;
            trace.activateOneBased(oneIndex);
            trace.add(
                    LINE_BUILD,
                    "Build BIT contributions for values[" + zeroIndex + "] = " + initialValues[zeroIndex],
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, zeroIndex, oneIndex, "build", "pending", "pending", SnapshotStatus.ACTIVE));
            trace.add(
                    LINE_ADD,
                    "Add initial value " + initialValues[zeroIndex] + " at public index " + zeroIndex,
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, zeroIndex, oneIndex, "build", "pending", "pending", SnapshotStatus.ACTIVE));
            trace.add(
                    LINE_ADD_HEADER,
                    "Start one-based BIT update from pos = " + oneIndex,
                    StepEventType.EXECUTE_LINE,
                    facts(model, operation, zeroIndex, oneIndex, "build", "pending", "pending", SnapshotStatus.ACTIVE));
            int position = oneIndex;
            while (position <= model.length()) {
                trace.activateOneBased(position);
                model.addBitCell(position, initialValues[zeroIndex]);
                int next = position + RangeTreeSupport.FenwickModel.lowbit(position);
                trace.add(
                        LINE_ADD_CELL,
                        "Add " + initialValues[zeroIndex] + " to bit[" + position + "]; next pos = " + next,
                        StepEventType.EXECUTE_LINE,
                        transitionFacts(model, operation, zeroIndex, position, next, "build", "pending", "pending"));
                trace.doneOneBased(position);
                position = next;
            }
        }
    }

    private static String update(
            RangeInput.FenwickRequest request,
            RangeTreeSupport.FenwickModel model,
            RangeTreeSupport.FenwickTraceBuilder trace) {
        int zeroIndex = request.index();
        int oneIndex = zeroIndex + 1;
        int delta = request.delta();
        trace.add(
                LINE_ADD_HEADER,
                "Update public index " + zeroIndex + " by delta " + delta,
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), zeroIndex, oneIndex, "update", "pending", "pending", SnapshotStatus.ACTIVE));
        model.changeValue(zeroIndex, delta);
        trace.add(
                LINE_ADD_POSITION,
                "Convert public index " + zeroIndex + " to internal pos = " + oneIndex,
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), zeroIndex, oneIndex, "update", "pending", "pending", SnapshotStatus.ACTIVE));
        int position = oneIndex;
        while (position <= model.length()) {
            trace.activateOneBased(position);
            model.addBitCell(position, delta);
            int next = position + RangeTreeSupport.FenwickModel.lowbit(position);
            trace.add(
                    LINE_ADD_CELL,
                    "Update bit[" + position + "] by " + delta + "; next pos = " + next,
                    StepEventType.EXECUTE_LINE,
                    transitionFacts(model, request.operation(), zeroIndex, position, next, "update", "pending",
                            Long.toString(model.value(zeroIndex))));
            trace.doneOneBased(position);
            position = next;
        }
        return Long.toString(model.value(zeroIndex));
    }

    private static String query(
            RangeInput.FenwickRequest request,
            RangeTreeSupport.FenwickModel model,
            RangeTreeSupport.FenwickTraceBuilder trace) {
        int zeroIndex = request.index();
        int position = zeroIndex + 1;
        long answer = 0L;
        trace.add(
                LINE_QUERY_HEADER,
                "Compute inclusive prefix sum through public index " + zeroIndex,
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), zeroIndex, position, "query", "pending", "pending", SnapshotStatus.ACTIVE));
        trace.add(
                LINE_QUERY_POSITION,
                "Set pos = " + position + " and answer = 0",
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), zeroIndex, position, "query", "0", "pending", SnapshotStatus.ACTIVE));
        while (position > 0) {
            trace.activateOneBased(position);
            answer += model.bitValue(position);
            int next = position - RangeTreeSupport.FenwickModel.lowbit(position);
            trace.add(
                    LINE_QUERY_CELL,
                    "Read bit[" + position + "] and move to pos = " + next + "; partial answer = " + answer,
                    StepEventType.EXECUTE_LINE,
                    transitionFacts(model, request.operation(), zeroIndex, position, next, "query", Long.toString(answer),
                            Long.toString(answer)));
            trace.doneOneBased(position);
            position = next;
        }
        trace.add(
                LINE_QUERY_RETURN,
                "Return inclusive prefix answer " + answer + " through index " + zeroIndex,
                StepEventType.EXECUTE_LINE,
                facts(model, request.operation(), zeroIndex, zeroIndex + 1, "query", Long.toString(answer),
                        Long.toString(answer), SnapshotStatus.DONE));
        return Long.toString(answer);
    }

    private static List<Fact> transitionFacts(
            RangeTreeSupport.FenwickModel model, String operation, int publicIndex,
            int position, int next, String phase, String partialAnswer, String answer) {
        List<Fact> result = new ArrayList<>(facts(model, operation, publicIndex, position,
                phase, partialAnswer, answer, SnapshotStatus.ACTIVE));
        int lowbit = RangeTreeSupport.FenwickModel.lowbit(position);
        result.add(RangeTreeSupport.fact("next-bit-index", Integer.toString(next), SnapshotStatus.ACTIVE));
        result.add(RangeTreeSupport.fact("next-index-binary", RangeTreeSupport.binaryIndex(next, model.length()), SnapshotStatus.ACTIVE));
        result.add(RangeTreeSupport.fact("bit-transition", position + (next < position ? " − " : " + ")
                + lowbit + " = " + next, SnapshotStatus.ACTIVE));
        result.add(RangeTreeSupport.fact("bit-direction", next < position
                ? "Query: remove the lowest set bit" : "Update: add the lowest set bit", SnapshotStatus.ACTIVE));
        return List.copyOf(result);
    }

    private static List<Fact> facts(
            RangeTreeSupport.FenwickModel model,
            String operation,
            int publicIndex,
            int oneBasedIndex,
            String phase,
            String partialAnswer,
            String answer,
            SnapshotStatus status) {
        String bitRange = oneBasedIndex > 0 && oneBasedIndex <= model.length()
                ? RangeTreeSupport.bitRange(oneBasedIndex, model.length())
                : "none";
        return List.of(
                RangeTreeSupport.fact("renderer", "fenwick", SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("index-binary", oneBasedIndex > 0 ? RangeTreeSupport.binaryIndex(oneBasedIndex, model.length()) : "none", status),
                RangeTreeSupport.fact("lowbit-binary", oneBasedIndex > 0 ? RangeTreeSupport.binaryIndex(RangeTreeSupport.FenwickModel.lowbit(oneBasedIndex), model.length()) : "none", status),
                RangeTreeSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("index", Integer.toString(publicIndex), status),
                RangeTreeSupport.fact("bit-index", Integer.toString(oneBasedIndex), status),
                RangeTreeSupport.fact("lowbit", oneBasedIndex > 0 && oneBasedIndex <= model.length()
                        ? Integer.toString(RangeTreeSupport.FenwickModel.lowbit(oneBasedIndex)) : "none", status),
                RangeTreeSupport.fact("bit-range", bitRange, status),
                RangeTreeSupport.fact("bit-ranges", RangeTreeSupport.formatBitRanges(model.length()), SnapshotStatus.DEFAULT),
                RangeTreeSupport.fact("values", RangeTreeSupport.formatValues(model.values()), status),
                RangeTreeSupport.fact("partial-answer", partialAnswer, status),
                RangeTreeSupport.fact("answer", answer, status),
                RangeTreeSupport.fact("result", answer, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        input.put("operation", "update");
        input.put("index", 2);
        input.put("delta", 4);
        return new SimulationMetadata(
                TYPE,
                "Fenwick Tree (Binary Indexed Tree)",
                "O(log n) update and prefix query after O(n log n) build",
                "O(n)",
                RendererFamily.TABLE,
                input,
                "Enter JSON as {\"values\":[2,1,3,5,4],\"operation\":\"update\",\"index\":2,\"delta\":4}; "
                        + "values length must be " + MIN_VALUES + ".." + MAX_VALUES
                        + ", values and delta must have absolute value at most " + MAX_ABS_VALUE
                        + ", indices are zero-based, update changes one value by delta, and query returns the inclusive prefix sum.",
                PSEUDOCODE);
    }
}
