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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Block sums with real edge scans, complete-block jumps, and point assignment. */
public final class SqrtDecompositionEngine implements SimulationEngine {
    public static final String TYPE = "SQRT_DECOMPOSITION";
    public static final int MIN_VALUES = RemainingRangeSupport.MIN_VALUES;
    public static final int MAX_VALUES = RemainingRangeSupport.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RemainingRangeSupport.MAX_ABS_VALUE;
    public static final int MAX_OPERATIONS = RemainingRangeSupport.MAX_OPERATIONS;
    public static final int MAX_TRACE_STEPS = RemainingRangeSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_BLOCK_BUILD = 2;
    private static final int LINE_QUERY = 3;
    private static final int LINE_EDGE = 4;
    private static final int LINE_BLOCK = 5;
    private static final int LINE_UPDATE = 6;
    private static final int LINE_RETURN = 7;
    private static final List<String> PSEUDOCODE = List.of(
            "sqrtRange(values, blockSize, operations):",
            "    blockSum[b] = sum of values in block b",
            "    for each operation in original order:",
            "        query: scan partial edge values",
            "        query: consume every complete block with blockSum",
            "        update: values[index] = value; adjust its blockSum",
            "        emit the current range sum or updated value");
    private static final int[] DEFAULT_VALUES = {4, 1, 7, 3, 2, 6, 5, 8};
    private static final int DEFAULT_BLOCK_SIZE = 3;
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        JsonNode object = RemainingRangeSupport.requireObject(input, TYPE);
        int[] sourceValues = RemainingRangeSupport.readValues(object, TYPE);
        int blockSize = RemainingRangeSupport.readBlockSize(object.get("blockSize"), sourceValues.length,
                TYPE + " blockSize");
        List<RemainingRangeSupport.Operation> operations = RemainingRangeSupport.readOperations(input, TYPE);
        long[] values = new long[sourceValues.length];
        int blockCount = (sourceValues.length + blockSize - 1) / blockSize;
        long[] blockSums = new long[blockCount];
        SnapshotStatus[] statuses = RemainingRangeSupport.statuses(values.length, SnapshotStatus.DEFAULT);
        List<SimulationStep> steps = new ArrayList<>();
        String[] results = new String[operations.size()];
        boolean[] known = new boolean[operations.size()];

        for (int index = 0; index < sourceValues.length; index++) {
            values[index] = sourceValues[index];
        }
        add(
                steps,
                values,
                blockSums,
                blockSize,
                statuses,
                operations,
                results,
                known,
                -1,
                "initialize",
                "Initialize " + blockCount + " blocks of size " + blockSize + " for the values",
                LINE_METHOD,
                StepEventType.INITIALIZE,
                "pending",
                "pending",
                "pending",
                "pending",
                "pending",
                "pending");

        for (int block = 0; block < blockCount; block++) {
            int start = block * blockSize;
            int end = Math.min(values.length - 1, start + blockSize - 1);
            for (int index = start; index <= end; index++) {
                statuses[index] = SnapshotStatus.ACTIVE;
                blockSums[block] += values[index];
                add(
                        steps,
                        values,
                        blockSums,
                        blockSize,
                        statuses,
                        operations,
                        results,
                        known,
                        index,
                        "build",
                        "Add values[" + index + "] = " + values[index] + " to block " + block
                                + "; block sum = " + blockSums[block],
                        LINE_BLOCK_BUILD,
                        StepEventType.EXECUTE_LINE,
                        "pending",
                        "block " + block,
                        Integer.toString(block),
                        "pending",
                        "pending",
                        "pending");
                statuses[index] = SnapshotStatus.DONE;
            }
        }

        String lastResult = "pending";
        String lastAnswer = "pending";
        for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
            RemainingRangeSupport.Operation operation = operations.get(operationIndex);
            Arrays.fill(statuses, SnapshotStatus.DONE);
            if (operation.kind().equals("query")) {
                long answer = 0L;
                int cursor = operation.left();
                String consumed = "";
                add(
                        steps,
                        values,
                        blockSums,
                        blockSize,
                        statuses,
                        operations,
                        results,
                        known,
                        operationIndex,
                        "query",
                        "Process query " + operationIndex + " in original order for [" + operation.left() + ","
                                + operation.right() + "]",
                        LINE_QUERY,
                        StepEventType.EXECUTE_LINE,
                        Integer.toString(operationIndex),
                        "query",
                        "[" + operation.left() + "," + operation.right() + "]",
                        "pending",
                        "0",
                        consumed);
                while (cursor <= operation.right() && cursor % blockSize != 0) {
                    statuses[cursor] = SnapshotStatus.ACTIVE;
                    answer += values[cursor];
                    int index = cursor++;
                    add(
                            steps,
                            values,
                            blockSums,
                            blockSize,
                            statuses,
                            operations,
                            results,
                            known,
                            index,
                            "edge-left",
                            "Scan partial edge values[" + index + "] = " + values[index]
                                    + "; partial sum = " + answer,
                            LINE_EDGE,
                            StepEventType.EXECUTE_LINE,
                            Integer.toString(operationIndex),
                            "query",
                            "[" + operation.left() + "," + operation.right() + "]",
                            "pending",
                            Long.toString(answer),
                            consumed);
                    statuses[index] = SnapshotStatus.DONE;
                }
                while (cursor + blockSize - 1 <= operation.right()) {
                    int block = cursor / blockSize;
                    answer += blockSums[block];
                    int start = cursor;
                    int end = cursor + blockSize - 1;
                    consumed = consumed.isEmpty() ? "block " + block : consumed + ", block " + block;
                    for (int index = start; index <= end; index++) {
                        statuses[index] = SnapshotStatus.ACTIVE;
                    }
                    cursor += blockSize;
                    add(
                            steps,
                            values,
                            blockSums,
                            blockSize,
                            statuses,
                            operations,
                            results,
                            known,
                            start,
                            "whole-block",
                            "Consume complete block " + block + " [" + start + "," + end + "] with stored sum "
                                    + blockSums[block] + "; partial sum = " + answer,
                            LINE_BLOCK,
                            StepEventType.EXECUTE_LINE,
                            Integer.toString(operationIndex),
                            "query",
                            "[" + operation.left() + "," + operation.right() + "]",
                            "pending",
                            Long.toString(answer),
                            consumed);
                    for (int index = start; index <= end; index++) {
                        statuses[index] = SnapshotStatus.DONE;
                    }
                }
                while (cursor <= operation.right()) {
                    statuses[cursor] = SnapshotStatus.ACTIVE;
                    answer += values[cursor];
                    int index = cursor++;
                    add(
                            steps,
                            values,
                            blockSums,
                            blockSize,
                            statuses,
                            operations,
                            results,
                            known,
                            index,
                            "edge-right",
                            "Scan remaining edge values[" + index + "] = " + values[index]
                                    + "; partial sum = " + answer,
                            LINE_EDGE,
                            StepEventType.EXECUTE_LINE,
                            Integer.toString(operationIndex),
                            "query",
                            "[" + operation.left() + "," + operation.right() + "]",
                            "pending",
                            Long.toString(answer),
                            consumed);
                    statuses[index] = SnapshotStatus.DONE;
                }
                results[operationIndex] = Long.toString(answer);
                known[operationIndex] = true;
                lastResult = results[operationIndex];
                lastAnswer = results[operationIndex];
                add(
                        steps,
                        values,
                        blockSums,
                        blockSize,
                        statuses,
                        operations,
                        results,
                        known,
                        operationIndex,
                        "query-answer",
                        "Return query " + operationIndex + " with inclusive range sum " + answer,
                        LINE_RETURN,
                        StepEventType.EXECUTE_LINE,
                        Integer.toString(operationIndex),
                        "query",
                        "[" + operation.left() + "," + operation.right() + "]",
                        Long.toString(answer),
                        Long.toString(answer),
                        consumed);
            } else {
                long oldValue = values[operation.index()];
                long delta = operation.value() - oldValue;
                statuses[operation.index()] = SnapshotStatus.ACTIVE;
                values[operation.index()] = operation.value();
                blockSums[operation.index() / blockSize] += delta;
                add(
                        steps,
                        values,
                        blockSums,
                        blockSize,
                        statuses,
                        operations,
                        results,
                        known,
                        operationIndex,
                        "update",
                        "Update values[" + operation.index() + "] from " + oldValue + " to " + operation.value()
                                + " (delta " + delta + ")",
                        LINE_UPDATE,
                        StepEventType.EXECUTE_LINE,
                        Integer.toString(operationIndex),
                        "update",
                        "index " + operation.index(),
                        "pending",
                        "pending",
                        "none");
                results[operationIndex] = "values[" + operation.index() + "] = " + operation.value();
                known[operationIndex] = true;
                lastResult = results[operationIndex];
                lastAnswer = Long.toString(operation.value());
                statuses[operation.index()] = SnapshotStatus.DONE;
                add(
                        steps,
                        values,
                        blockSums,
                        blockSize,
                        statuses,
                        operations,
                        results,
                        known,
                        operationIndex,
                        "update-complete",
                        "Adjust block " + (operation.index() / blockSize) + " by delta " + delta
                                + " before the next operation",
                        LINE_RETURN,
                        StepEventType.EXECUTE_LINE,
                        Integer.toString(operationIndex),
                        "update",
                        "index " + operation.index(),
                        Long.toString(operation.value()),
                        "pending",
                        "none");
            }
        }

        Arrays.fill(statuses, SnapshotStatus.DONE);
        add(
                steps,
                values,
                blockSums,
                blockSize,
                statuses,
                operations,
                results,
                known,
                -1,
                "complete",
                "Complete: block sums and values reflect every operation; last result = " + lastResult,
                0,
                StepEventType.COMPLETE,
                "complete",
                "complete",
                "complete",
                lastAnswer,
                lastAnswer,
                "complete");
        return List.copyOf(steps);
    }

    private static void add(
            List<SimulationStep> steps,
            long[] values,
            long[] blockSums,
            int blockSize,
            SnapshotStatus[] statuses,
            List<RemainingRangeSupport.Operation> operations,
            String[] results,
            boolean[] known,
            int activeOperation,
            String phase,
            String narration,
            int highlightedLine,
            StepEventType eventType,
            String operationIndex,
            String operationKind,
            String target,
            String answer,
            String partialAnswer,
            String consumed) {
        int focusIndex = -1;
        for (int index = 0; index < statuses.length; index++) {
            if (statuses[index] == SnapshotStatus.ACTIVE) {
                focusIndex = index;
                break;
            }
        }
        RemainingRangeSupport.addArrayStep(
                steps,
                TYPE,
                values,
                statuses,
                focusIndex,
                highlightedLine,
                narration,
                eventType,
                facts(phase, activeOperation, focusIndex, operationIndex, operationKind, target, values, blockSums, blockSize,
                        operations, results, known, answer, partialAnswer, consumed));
    }

    private static List<Fact> facts(
            String phase,
            int activeOperation,
            int focusIndex,
            String operationIndex,
            String operationKind,
            String target,
            long[] values,
            long[] blockSums,
            int blockSize,
            List<RemainingRangeSupport.Operation> operations,
            String[] results,
            boolean[] known,
            String answer,
            String partialAnswer,
            String consumed) {
        int operationStatusIndex = phase.equals("build") ? -1 : activeOperation;
        return List.of(
                RemainingRangeSupport.fact("renderer", "sqrt", SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("focus-block", focusIndex >= 0 ? Integer.toString(focusIndex / blockSize) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("query-left", operationKind.equals("query")
                        ? Integer.toString(operations.get(Integer.parseInt(operationIndex)).left()) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("query-right", operationKind.equals("query")
                        ? Integer.toString(operations.get(Integer.parseInt(operationIndex)).right()) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("operation-index", operationIndex, operationStatusIndex < 0
                        ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("operation", operationKind, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("target", target, operationStatusIndex < 0
                        ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("processing-order", operationIndex, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("values", RemainingRangeSupport.formatValues(values), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("block-size", Integer.toString(blockSize), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("block-ranges", formatBlockRanges(values.length, blockSize), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("block-sums", RemainingRangeSupport.formatValues(blockSums), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("blocks", formatBlocks(blockSums, values.length, blockSize), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("operations", RemainingRangeSupport.formatOperations(operations), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("consumed-blocks", consumed, SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("partial-answer", partialAnswer, SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("operation-results", formatResults(results, known), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("answer", answer, SnapshotStatus.DONE),
                RemainingRangeSupport.fact("result", answer, SnapshotStatus.DONE));
    }

    private static String formatBlockRanges(int length, int blockSize) {
        StringBuilder result = new StringBuilder();
        int blockCount = (length + blockSize - 1) / blockSize;
        for (int block = 0; block < blockCount; block++) {
            if (block > 0) {
                result.append(", ");
            }
            int start = block * blockSize;
            int end = Math.min(length - 1, start + blockSize - 1);
            result.append("b").append(block).append("[").append(start).append(",").append(end).append("]");
        }
        return result.toString();
    }

    private static String formatBlocks(long[] sums, int length, int blockSize) {
        StringBuilder result = new StringBuilder();
        for (int block = 0; block < sums.length; block++) {
            if (block > 0) {
                result.append(", ");
            }
            int start = block * blockSize;
            int end = Math.min(length - 1, start + blockSize - 1);
            result.append("b").append(block).append("[").append(start).append("..").append(end).append("]=")
                    .append(sums[block]);
        }
        return result.toString();
    }

    private static String formatResults(String[] results, boolean[] known) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < results.length; index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(known[index] ? results[index] : "pending");
        }
        return result.append(']').toString();
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        input.put("blockSize", DEFAULT_BLOCK_SIZE);
        ArrayNode operations = input.putArray("operations");
        ObjectNode query = operations.addObject();
        query.put("kind", "query");
        query.put("left", 1);
        query.put("right", 6);
        ObjectNode update = operations.addObject();
        update.put("kind", "update");
        update.put("index", 4);
        update.put("value", 9);
        return new SimulationMetadata(
                TYPE,
                "Sqrt Decomposition",
                "O(sqrt(n)) range sum and O(1) point assignment after O(n) build",
                "O(n)",
                RendererFamily.ARRAY,
                input,
                "Enter JSON as {\"values\":[4,1,7,3,2,6,5,8],\"blockSize\":3,\"operations\":[{\"kind\":\"query\",\"left\":1,\"right\":6},{\"kind\":\"update\",\"index\":4,\"value\":9}]}; values length must be "
                        + MIN_VALUES + ".." + MAX_VALUES + ", blockSize must be in 1..n, there can be at most "
                        + MAX_OPERATIONS + " original-order operations, indices are inclusive zero-based, and values have absolute value at most "
                        + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
