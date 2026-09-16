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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Offline Mo trace with real block ordering and a sliding distinct-count window. */
public final class MoRangeQueryEngine implements SimulationEngine {
    public static final String TYPE = "MO_RANGE_QUERY";
    public static final int MIN_VALUES = RemainingRangeSupport.MIN_VALUES;
    public static final int MAX_VALUES = RemainingRangeSupport.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RemainingRangeSupport.MAX_ABS_VALUE;
    public static final int MAX_OPERATIONS = RemainingRangeSupport.MAX_OPERATIONS;
    public static final int MAX_TRACE_STEPS = RemainingRangeSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_ORDER = 3;
    private static final int LINE_ADD = 5;
    private static final int LINE_REMOVE = 5;
    private static final int LINE_ANSWER = 6;
    private static final int LINE_RETURN = 7;
    private static final List<String> COLUMNS = List.of("query id", "range", "distinct count", "processing order");
    private static final List<String> PSEUDOCODE = List.of(
            "moQueries(values, queries):",
            "    block = floor(sqrt(n))",
            "    sort queries by left / block, then alternating right",
            "    window = empty; frequency = {}; distinct = 0",
            "    move left and right while adding/removing values",
            "    answer[query.id] = distinct",
            "    return answers in original query order");
    private static final int[] DEFAULT_VALUES = {1, 2, 1, 3, 2, 4};
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int[] values = RemainingRangeSupport.readValues(input, TYPE);
        List<RemainingRangeSupport.RangeQuery> original = RemainingRangeSupport.readQueries(input, TYPE);
        int blockSize = Math.max(1, (int) Math.sqrt(values.length));
        List<IndexedQuery> ordered = new ArrayList<>(original.size());
        for (int queryIndex = 0; queryIndex < original.size(); queryIndex++) {
            ordered.add(new IndexedQuery(queryIndex, original.get(queryIndex)));
        }
        ordered.sort(queryComparator(blockSize));

        Map<Integer, Integer> frequencies = new TreeMap<>();
        long[] answers = new long[original.size()];
        boolean[] known = new boolean[original.size()];
        int[] window = {0, -1};
        int[] distinct = {0};
        List<SimulationStep> steps = new ArrayList<>();
        SnapshotStatus[] statuses = RemainingRangeSupport.statuses(original.size(), SnapshotStatus.DEFAULT);
        String processingOrder = formatIndexedQueries(ordered);
        int[] processingRanks = new int[original.size()];
        for (int rank = 0; rank < ordered.size(); rank++) processingRanks[ordered.get(rank).id()] = rank + 1;

        add(
                steps,
                original,
                answers,
                known,
                statuses,
                -1,
                values,
                blockSize,
                processingOrder,
                processingRanks,
                window[0],
                window[1],
                frequencies,
                distinct[0],
                "initialize",
                "Initialize an empty sliding window for " + original.size() + " range querie(s)",
                0,
                "pending");
        add(
                steps,
                original,
                answers,
                known,
                statuses,
                -1,
                values,
                blockSize,
                processingOrder,
                processingRanks,
                window[0],
                window[1],
                frequencies,
                distinct[0],
                "order",
                "Order queries by left block " + blockSize + " and alternate right endpoints: " + processingOrder,
                LINE_ORDER,
                "pending");

        for (IndexedQuery indexed : ordered) {
            RemainingRangeSupport.RangeQuery query = indexed.query();
            statuses[indexed.id()] = SnapshotStatus.ACTIVE;
            while (window[0] > query.left()) {
                int index = --window[0];
                addValue(values[index], frequencies, distinct);
                add(
                        steps,
                        original,
                        answers,
                        known,
                        statuses,
                        indexed.id(),
                        values,
                        blockSize,
                        processingOrder,
                        processingRanks,
                        window[0],
                        window[1],
                        frequencies,
                        distinct[0],
                        "add",
                        "Add values[" + index + "] = " + values[index]
                                + "; " + frequencyTransition(values[index], frequencies, true) + "; move the left edge",
                        LINE_ADD,
                        "pending");
            }
            while (window[1] < query.right()) {
                int index = ++window[1];
                addValue(values[index], frequencies, distinct);
                add(
                        steps,
                        original,
                        answers,
                        known,
                        statuses,
                        indexed.id(),
                        values,
                        blockSize,
                        processingOrder,
                        processingRanks,
                        window[0],
                        window[1],
                        frequencies,
                        distinct[0],
                        "add",
                        "Add values[" + index + "] = " + values[index]
                                + "; " + frequencyTransition(values[index], frequencies, true) + "; move the right edge",
                        LINE_ADD,
                        "pending");
            }
            while (window[0] < query.left()) {
                int index = window[0]++;
                removeValue(values[index], frequencies, distinct);
                add(
                        steps,
                        original,
                        answers,
                        known,
                        statuses,
                        indexed.id(),
                        values,
                        blockSize,
                        processingOrder,
                        processingRanks,
                        window[0],
                        window[1],
                        frequencies,
                        distinct[0],
                        "remove",
                        "Remove values[" + index + "] = " + values[index]
                                + "; " + frequencyTransition(values[index], frequencies, false) + "; move the left edge",
                        LINE_REMOVE,
                        "pending");
            }
            while (window[1] > query.right()) {
                int index = window[1]--;
                removeValue(values[index], frequencies, distinct);
                add(
                        steps,
                        original,
                        answers,
                        known,
                        statuses,
                        indexed.id(),
                        values,
                        blockSize,
                        processingOrder,
                        processingRanks,
                        window[0],
                        window[1],
                        frequencies,
                        distinct[0],
                        "remove",
                        "Remove values[" + index + "] = " + values[index]
                                + "; " + frequencyTransition(values[index], frequencies, false) + "; move the right edge",
                        LINE_REMOVE,
                        "pending");
            }

            long answer = distinct[0];
            answers[indexed.id()] = answer;
            known[indexed.id()] = true;
            statuses[indexed.id()] = SnapshotStatus.DONE;
            add(
                    steps,
                    original,
                    answers,
                    known,
                    statuses,
                    indexed.id(),
                    values,
                    blockSize,
                    processingOrder,
                    processingRanks,
                    window[0],
                    window[1],
                    frequencies,
                    distinct[0],
                    "answer",
                    "Answer query " + indexed.id() + " in block order with distinct count " + answer
                            + "; store it at its original query id",
                    LINE_ANSWER,
                    Long.toString(answer));
        }

        java.util.Arrays.fill(statuses, SnapshotStatus.DONE);
        String finalAnswer = RemainingRangeSupport.formatKnownAnswers(answers, known);
        add(
                steps,
                original,
                answers,
                known,
                statuses,
                -1,
                values,
                blockSize,
                processingOrder,
                processingRanks,
                window[0],
                window[1],
                frequencies,
                distinct[0],
                "return",
                "Return distinct counts in original query order " + finalAnswer,
                LINE_RETURN,
                finalAnswer);
        add(
                steps,
                original,
                answers,
                known,
                statuses,
                -1,
                values,
                blockSize,
                processingOrder,
                processingRanks,
                window[0],
                window[1],
                frequencies,
                distinct[0],
                "complete",
                "Complete: Mo's window processing returned " + finalAnswer,
                0,
                finalAnswer);
        return List.copyOf(steps);
    }

    private static Comparator<IndexedQuery> queryComparator(int blockSize) {
        return (first, second) -> {
            int firstBlock = first.query().left() / blockSize;
            int secondBlock = second.query().left() / blockSize;
            if (firstBlock != secondBlock) {
                return Integer.compare(firstBlock, secondBlock);
            }
            int rightComparison = Integer.compare(first.query().right(), second.query().right());
            return (firstBlock & 1) == 0 ? rightComparison : -rightComparison;
        };
    }

    private static String frequencyTransition(int value, Map<Integer, Integer> frequencies, boolean adding) {
        int current = frequencies.getOrDefault(value, 0);
        int previous = current + (adding ? -1 : 1);
        return "freq[" + value + "]: " + previous + " → " + current
                + (adding && previous == 0 ? "; distinct +1" : !adding && current == 0 ? "; distinct −1" : "; distinct unchanged");
    }

    private static void addValue(int value, Map<Integer, Integer> frequencies, int[] distinct) {
        int previous = frequencies.getOrDefault(value, 0);
        frequencies.put(value, previous + 1);
        if (previous == 0) {
            distinct[0]++;
        }
    }

    private static void removeValue(int value, Map<Integer, Integer> frequencies, int[] distinct) {
        int previous = frequencies.getOrDefault(value, 0);
        if (previous <= 0) {
            throw new IllegalStateException("Mo window attempted to remove an absent value");
        }
        if (previous == 1) {
            frequencies.remove(value);
            distinct[0]--;
        } else {
            frequencies.put(value, previous - 1);
        }
    }

    private static String formatIndexedQueries(List<IndexedQuery> queries) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < queries.size(); index++) {
            if (index > 0) {
                result.append(" -> ");
            }
            IndexedQuery query = queries.get(index);
            result.append("q").append(query.id()).append("[")
                    .append(query.query().left()).append(",")
                    .append(query.query().right()).append("]");
        }
        return result.toString();
    }

    private static void add(
            List<SimulationStep> steps,
            List<RemainingRangeSupport.RangeQuery> queries,
            long[] answers,
            boolean[] known,
            SnapshotStatus[] statuses,
            int activeQuery,
            int[] values,
            int blockSize,
            String processingOrder,
            int[] processingRanks,
            int windowLeft,
            int windowRight,
            Map<Integer, Integer> frequencies,
            int distinct,
            String phase,
            String narration,
            int highlightedLine,
            String answer) {
        List<List<TypedCell>> rows = new ArrayList<>(queries.size());
        for (int queryIndex = 0; queryIndex < queries.size(); queryIndex++) {
            RemainingRangeSupport.RangeQuery query = queries.get(queryIndex);
            SnapshotStatus status = statuses[queryIndex];
            String queryAnswer = known[queryIndex] ? Long.toString(answers[queryIndex]) : "pending";
            rows.add(List.of(
                    new TypedCell("query-id-" + queryIndex, "q" + queryIndex, status),
                    new TypedCell("query-range-" + queryIndex,
                            "[" + query.left() + "," + query.right() + "]", status),
                    new TypedCell("query-answer-" + queryIndex, queryAnswer, status),
                    new TypedCell("query-order-" + queryIndex, Integer.toString(processingRanks[queryIndex]), status)));
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
                        activeQuery,
                        blockSize,
                        processingOrder,
                        processingRanks,
                        windowLeft,
                        windowRight,
                        frequencies,
                        distinct,
                        queries,
                        answers,
                        known,
                        values,
                        answer));
    }

    private static List<Fact> facts(
            String phase,
            int activeQuery,
            int blockSize,
            String processingOrder,
            int[] processingRanks,
            int windowLeft,
            int windowRight,
            Map<Integer, Integer> frequencies,
            int distinct,
            List<RemainingRangeSupport.RangeQuery> queries,
            long[] answers,
            boolean[] known,
            int[] values,
            String answer) {
        String window = windowRight < windowLeft ? "empty" : "[" + windowLeft + "," + windowRight + "]";
        return List.of(
                RemainingRangeSupport.fact("renderer", "mo", SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("processing-rank", activeQuery >= 0 ? Integer.toString(processingRanks[activeQuery]) : "none", SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("values", RemainingRangeSupport.formatValues(values), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("block-size", Integer.toString(blockSize), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("active-query", Integer.toString(activeQuery),
                        activeQuery < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("window", window, SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("window-left", Integer.toString(windowLeft), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("window-right", Integer.toString(windowRight), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("frequencies", RemainingRangeSupport.formatFrequencies(frequencies), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("distinct-count", Integer.toString(distinct), SnapshotStatus.ACTIVE),
                RemainingRangeSupport.fact("query-order", RemainingRangeSupport.formatRanges(queries), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("processing-order", processingOrder, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("answers", RemainingRangeSupport.formatKnownAnswers(answers, known), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("answer", answer, SnapshotStatus.DONE),
                RemainingRangeSupport.fact("result", answer, SnapshotStatus.DONE));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        ArrayNode queries = input.putArray("queries");
        addQuery(queries, 0, 3);
        addQuery(queries, 2, 5);
        addQuery(queries, 1, 4);
        return new SimulationMetadata(
                TYPE,
                "Offline Range Query (Mo's)",
                "O((n + q) sqrt(n)) window movement after O(q log q) ordering",
                "O(n + q)",
                RendererFamily.TABLE,
                input,
                "Enter JSON as {\"values\":[1,2,1,3,2,4],\"queries\":[{\"left\":0,\"right\":3},{\"left\":2,\"right\":5}]}; values length must be "
                        + MIN_VALUES + ".." + MAX_VALUES + ", there can be at most " + MAX_OPERATIONS
                        + " inclusive zero-based queries, and values have absolute value at most " + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }

    private static void addQuery(ArrayNode queries, int left, int right) {
        ObjectNode query = queries.addObject();
        query.put("left", left);
        query.put("right", right);
    }

    private record IndexedQuery(int id, RemainingRangeSupport.RangeQuery query) {
    }
}
