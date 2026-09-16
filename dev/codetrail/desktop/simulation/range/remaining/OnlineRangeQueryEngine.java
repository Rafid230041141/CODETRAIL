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

/** Original-order online range operations using a transparent bounded array scan. */
public final class OnlineRangeQueryEngine implements SimulationEngine {
    public static final String TYPE = "ONLINE_RANGE_QUERY";
    public static final int MIN_VALUES = RemainingRangeSupport.MIN_VALUES;
    public static final int MAX_VALUES = RemainingRangeSupport.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RemainingRangeSupport.MAX_ABS_VALUE;
    public static final int MAX_OPERATIONS = RemainingRangeSupport.MAX_OPERATIONS;
    public static final int MAX_TRACE_STEPS = RemainingRangeSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_QUERY = 4;
    private static final int LINE_SCAN = 5;
    private static final int LINE_UPDATE = 7;
    private static final int LINE_RETURN = 8;
    private static final List<String> COLUMNS = List.of("operation", "kind", "target", "result");
    private static final List<String> PSEUDOCODE = List.of(
            "onlineRange(values, operations):",
            "    for each operation in original order:",
            "        if operation.kind == query:",
            "            answer = 0",
            "            for i = left .. right: answer += values[i]",
            "        else:",
            "            values[index] = value immediately",
            "        emit this operation's current result");
    private static final int[] DEFAULT_VALUES = {5, 2, 7, 1, 3};
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int[] sourceValues = RemainingRangeSupport.readValues(input, TYPE);
        List<RemainingRangeSupport.Operation> operations = RemainingRangeSupport.readOperations(input, TYPE);
        long[] values = new long[sourceValues.length];
        for (int index = 0; index < sourceValues.length; index++) {
            values[index] = sourceValues[index];
        }
        long[] queryAnswers = new long[operations.size()];
        boolean[] queryKnown = new boolean[operations.size()];
        String[] operationResults = new String[operations.size()];
        boolean[] operationDone = new boolean[operations.size()];
        SnapshotStatus[] statuses = RemainingRangeSupport.statuses(operations.size(), SnapshotStatus.DEFAULT);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                operations,
                values,
                queryAnswers,
                queryKnown,
                operationResults,
                operationDone,
                statuses,
                -1,
                -1,
                "initialize",
                "Initialize the online array; future operations remain unread and unprocessed",
                0,
                "pending",
                "pending",
                "pending",
                "pending",
                "pending");

        String lastResult = "pending";
        String lastAnswer = "pending";
        for (int operationIndex = 0; operationIndex < operations.size(); operationIndex++) {
            RemainingRangeSupport.Operation operation = operations.get(operationIndex);
            statuses[operationIndex] = SnapshotStatus.ACTIVE;
            if (operation.kind().equals("query")) {
                long partial = 0L;
                add(
                        steps,
                        operations,
                        values,
                        queryAnswers,
                        queryKnown,
                        operationResults,
                        operationDone,
                        statuses,
                        operationIndex,
                        operationIndex,
                        "query",
                        "Read query " + operationIndex + " in original order: sum [" + operation.left() + ","
                                + operation.right() + "]",
                        LINE_QUERY,
                        Long.toString(operationIndex),
                        "query",
                        "[" + operation.left() + "," + operation.right() + "]",
                        "pending",
                        Long.toString(partial));
                for (int index = operation.left(); index <= operation.right(); index++) {
                    partial += values[index];
                    add(
                            steps,
                            operations,
                            values,
                            queryAnswers,
                            queryKnown,
                            operationResults,
                            operationDone,
                            statuses,
                            operationIndex,
                            index,
                            "query-scan",
                            "Process operation " + operationIndex + " immediately: add values[" + index + "] = "
                                    + values[index] + "; partial sum = " + partial,
                            LINE_SCAN,
                            Long.toString(operationIndex),
                            "query",
                            "[" + operation.left() + "," + operation.right() + "]",
                            "pending",
                            Long.toString(partial));
                }
                queryAnswers[operationIndex] = partial;
                queryKnown[operationIndex] = true;
                operationResults[operationIndex] = Long.toString(partial);
                operationDone[operationIndex] = true;
                statuses[operationIndex] = SnapshotStatus.DONE;
                lastResult = Long.toString(partial);
                lastAnswer = Long.toString(partial);
                add(
                        steps,
                        operations,
                        values,
                        queryAnswers,
                        queryKnown,
                        operationResults,
                        operationDone,
                        statuses,
                        operationIndex,
                        -1,
                        "query-answer",
                        "Return query " + operationIndex + " now with inclusive sum " + partial,
                        LINE_RETURN,
                        Long.toString(operationIndex),
                        "query",
                        "[" + operation.left() + "," + operation.right() + "]",
                        Long.toString(partial),
                        Long.toString(partial));
            } else {
                long oldValue = values[operation.index()];
                long delta = operation.value() - oldValue;
                values[operation.index()] = operation.value();
                add(
                        steps,
                        operations,
                        values,
                        queryAnswers,
                        queryKnown,
                        operationResults,
                        operationDone,
                        statuses,
                        operationIndex,
                        operation.index(),
                        "update",
                        "Process update " + operationIndex + " immediately: values[" + operation.index() + "] changes from "
                                + oldValue + " to " + operation.value() + " (delta " + delta + ")",
                        LINE_UPDATE,
                        Long.toString(operationIndex),
                        "update",
                        "index " + operation.index(),
                        "pending",
                        "pending");
                operationResults[operationIndex] = "values[" + operation.index() + "] = " + operation.value();
                operationDone[operationIndex] = true;
                statuses[operationIndex] = SnapshotStatus.DONE;
                lastResult = operationResults[operationIndex];
                lastAnswer = Long.toString(operation.value());
                add(
                        steps,
                        operations,
                        values,
                        queryAnswers,
                        queryKnown,
                        operationResults,
                        operationDone,
                        statuses,
                        operationIndex,
                        operation.index(),
                        "update-complete",
                        "Update " + operationIndex + " is visible before the next operation: " + operationResults[operationIndex],
                        LINE_RETURN,
                        Long.toString(operationIndex),
                        "update",
                        "index " + operation.index(),
                        Long.toString(operation.value()),
                        Long.toString(operation.value()));
            }
        }

        java.util.Arrays.fill(statuses, SnapshotStatus.DONE);
        add(
                steps,
                operations,
                values,
                queryAnswers,
                queryKnown,
                operationResults,
                operationDone,
                statuses,
                -1,
                -1,
                "complete",
                "Complete: all online operations were processed in original order; last result = " + lastResult,
                0,
                "complete",
                "complete",
                "complete",
                lastAnswer,
                lastAnswer);
        return List.copyOf(steps);
    }

    private static void add(
            List<SimulationStep> steps,
            List<RemainingRangeSupport.Operation> operations,
            long[] values,
            long[] queryAnswers,
            boolean[] queryKnown,
            String[] operationResults,
            boolean[] operationDone,
            SnapshotStatus[] statuses,
            int activeOperation,
            int focusIndex,
            String phase,
            String narration,
            int highlightedLine,
            String operationIndex,
            String operationKind,
            String target,
            String answer,
            String partialAnswer) {
        List<List<TypedCell>> rows = new ArrayList<>(operations.size());
        for (int index = 0; index < operations.size(); index++) {
            RemainingRangeSupport.Operation operation = operations.get(index);
            SnapshotStatus status = statuses[index];
            String rowTarget = operation.kind().equals("query")
                    ? "[" + operation.left() + "," + operation.right() + "]"
                    : "index " + operation.index();
            String result = operationDone[index] ? operationResults[index] : "pending";
            rows.add(List.of(
                    new TypedCell("operation-" + index, Integer.toString(index), status),
                    new TypedCell("kind-" + index, operation.kind(), status),
                    new TypedCell("target-" + index, rowTarget, status),
                    new TypedCell("result-" + index, result, status)));
        }
        RemainingRangeSupport.addTableStep(
                steps,
                TYPE,
                COLUMNS,
                rows,
                highlightedLine,
                narration,
                phase.equals("initialize") ? StepEventType.INITIALIZE
                        : phase.equals("complete") ? StepEventType.COMPLETE : StepEventType.EXECUTE_LINE,
                facts(
                        phase,
                        activeOperation,
                        focusIndex,
                        operationIndex,
                        operationKind,
                        target,
                        values,
                        operations,
                        queryAnswers,
                        queryKnown,
                        answer,
                        partialAnswer));
    }

    private static List<Fact> facts(
            String phase,
            int activeOperation,
            int focusIndex,
            String operationIndex,
            String operationKind,
            String target,
            long[] values,
            List<RemainingRangeSupport.Operation> operations,
            long[] queryAnswers,
            boolean[] queryKnown,
            String answer,
            String partialAnswer) {
        return List.of(
                RemainingRangeSupport.fact("renderer", "online", SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("focus-index", phase.equals("query-scan") || phase.startsWith("update")
                        ? Integer.toString(focusIndex) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("query-left", activeOperation >= 0 && operationKind.equals("query")
                        ? Integer.toString(operations.get(activeOperation).left()) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("query-right", activeOperation >= 0 && operationKind.equals("query")
                        ? Integer.toString(operations.get(activeOperation).right()) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("teaching-equation", phase.equals("query-scan")
                        ? (Long.parseLong(partialAnswer) - values[focusIndex]) + " + " + values[focusIndex] + " = " + partialAnswer
                        : "pending", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("operation-index", operationIndex, activeOperation < 0
                        ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("operation", operationKind, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("target", target, activeOperation < 0
                        ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("processing-order", operationIndex, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("values", RemainingRangeSupport.formatValues(values), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("operations", RemainingRangeSupport.formatOperations(operations), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("partial-answer", partialAnswer, SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("answers", RemainingRangeSupport.formatKnownAnswers(queryAnswers, queryKnown), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("answer", answer, SnapshotStatus.DONE),
                RemainingRangeSupport.fact("result", answer, SnapshotStatus.DONE));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        ArrayNode operations = input.putArray("operations");
        ObjectNode query = operations.addObject();
        query.put("kind", "query");
        query.put("left", 1);
        query.put("right", 3);
        ObjectNode update = operations.addObject();
        update.put("kind", "update");
        update.put("index", 2);
        update.put("value", 6);
        ObjectNode finalQuery = operations.addObject();
        finalQuery.put("kind", "query");
        finalQuery.put("left", 0);
        finalQuery.put("right", 4);
        return new SimulationMetadata(
                TYPE,
                "Online Range Query",
                "O(n) direct range scan per query and O(1) point assignment",
                "O(n)",
                RendererFamily.TABLE,
                input,
                "Enter JSON as {\"values\":[5,2,7,1,3],\"operations\":[{\"kind\":\"query\",\"left\":1,\"right\":3},{\"kind\":\"update\",\"index\":2,\"value\":6}]}; values length must be "
                        + MIN_VALUES + ".." + MAX_VALUES + ", there can be at most " + MAX_OPERATIONS
                        + " original-order query/update operations, indices are inclusive zero-based, and values have absolute value at most "
                        + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
