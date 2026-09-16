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
import java.util.Arrays;
import java.util.List;

/** Static range-min trace using overlapping power-of-two sparse-table blocks. */
public final class SparseTableEngine implements SimulationEngine {
    public static final String TYPE = "SPARSE_TABLE";
    public static final int MIN_VALUES = RemainingRangeSupport.MIN_VALUES;
    public static final int MAX_VALUES = RemainingRangeSupport.MAX_VALUES;
    public static final int MAX_ABS_VALUE = RemainingRangeSupport.MAX_ABS_VALUE;
    public static final int MAX_TRACE_STEPS = RemainingRangeSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_BASE = 2;
    private static final int LINE_BUILD = 4;
    private static final int LINE_LEVEL = 3;
    private static final int LINE_QUERY_LEVEL = 6;
    private static final int LINE_QUERY_BLOCKS = 7;
    private static final int LINE_RETURN = 7;
    private static final List<String> PSEUDOCODE = List.of(
            "buildSparse(values):",
            "    table[0][i] = values[i]",
            "    for level = 1 while 2^level <= n:",
            "        table[level][i] = min(table[level - 1][i], table[level - 1][i + 2^(level - 1)])",
            "rangeMin(left, right):",
            "    level = floor(log2(right - left + 1))",
            "    return min(table[level][left], table[level][right - 2^level + 1])");
    private static final int[] DEFAULT_VALUES = {7, 2, 5, 1, 6, 3};
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        JsonNode object = RemainingRangeSupport.requireObject(input, TYPE);
        int[] values = RemainingRangeSupport.readValues(object, TYPE);
        String operation = RemainingRangeSupport.readOperation(object, TYPE, "range-min");
        int left = RemainingRangeSupport.readIndex(object.get("left"), values.length, TYPE + " left");
        int right = RemainingRangeSupport.readIndex(object.get("right"), values.length, TYPE + " right");
        RemainingRangeSupport.requireRange(left, right, values.length, TYPE + " query range");

        int levels = 32 - Integer.numberOfLeadingZeros(values.length);
        long[][] table = new long[levels][values.length];
        boolean[][] built = new boolean[levels][values.length];
        SnapshotStatus[] statuses = RemainingRangeSupport.statuses(levels, SnapshotStatus.DEFAULT);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                values,
                table,
                built,
                statuses,
                -1,
                "Initialize sparse-table levels for static range minimum queries",
                LINE_METHOD,
                StepEventType.INITIALIZE,
                facts(operation, "initialize", left, right, -1, -1, -1, -1, table, built, "pending", "pending", "none", SnapshotStatus.ACTIVE));

        statuses[0] = SnapshotStatus.ACTIVE;
        for (int index = 0; index < values.length; index++) {
            table[0][index] = values[index];
            built[0][index] = true;
            add(
                    steps,
                    values,
                    table,
                    built,
                    statuses,
                    index,
                    "Base level L0 stores values[" + index + "] = " + values[index],
                    LINE_BASE,
                    StepEventType.EXECUTE_LINE,
                    facts(operation, "base", left, right, 0, index, -1, -1, table, built,
                            "pending", "pending", "none", SnapshotStatus.ACTIVE));
        }
        statuses[0] = SnapshotStatus.DONE;

        for (int level = 1; level < levels; level++) {
            int length = 1 << level;
            if (length > values.length) {
                break;
            }
            statuses[level] = SnapshotStatus.ACTIVE;
            int half = length >>> 1;
            int count = values.length - length + 1;
            for (int start = 0; start < count; start++) {
                table[level][start] = Math.min(table[level - 1][start], table[level - 1][start + half]);
                built[level][start] = true;
                add(
                        steps,
                        values,
                        table,
                        built,
                        statuses,
                        start,
                        "Build L" + level + " block [" + start + "," + (start + length - 1) + "] = "
                                + table[level][start],
                        LINE_BUILD,
                        StepEventType.EXECUTE_LINE,
                        facts(operation, "build", left, right, level, start, -1, -1, table, built,
                                "pending", "pending", "none", SnapshotStatus.ACTIVE));
            }
            statuses[level] = SnapshotStatus.DONE;
            add(
                    steps,
                    values,
                    table,
                    built,
                    statuses,
                    -1,
                    "Finish level L" + level + " of length " + length + " with " + count + " block(s)",
                    LINE_LEVEL,
                    StepEventType.EXECUTE_LINE,
                    facts(operation, "level", left, right, level, -1, -1, -1, table, built,
                            "pending", "pending", "none", SnapshotStatus.DEFAULT));
        }

        int queryLength = right - left + 1;
        int queryLevel = floorLog2(queryLength);
        int span = 1 << queryLevel;
        int leftStart = left;
        int rightStart = right - span + 1;
        long leftValue = table[queryLevel][leftStart];
        long rightValue = table[queryLevel][rightStart];
        long answer = Math.min(leftValue, rightValue);
        statuses[queryLevel] = SnapshotStatus.ACTIVE;
        add(
                steps,
                values,
                table,
                built,
                statuses,
                leftStart,
                "Use level " + queryLevel + " (block length " + span + ") for range [" + left + "," + right + "]",
                LINE_QUERY_LEVEL,
                StepEventType.EXECUTE_LINE,
                facts(operation, "query-level", left, right, queryLevel, -1, leftStart, rightStart, table, built,
                        "pending", "pending", leftStart == rightStart ? "no" : "yes", SnapshotStatus.ACTIVE));
        add(
                steps,
                values,
                table,
                built,
                statuses,
                rightStart,
                "Read overlapping blocks [" + leftStart + "," + (leftStart + span - 1) + "] and [" + rightStart + ","
                        + (rightStart + span - 1) + "]; min(" + leftValue + "," + rightValue + ") = " + answer,
                LINE_QUERY_BLOCKS,
                StepEventType.EXECUTE_LINE,
                facts(operation, "query-blocks", left, right, queryLevel, -1, leftStart, rightStart, table, built,
                        Long.toString(answer), Long.toString(answer), leftStart == rightStart ? "no" : "yes", SnapshotStatus.ACTIVE));

        Arrays.fill(statuses, SnapshotStatus.DONE);
        add(
                steps,
                values,
                table,
                built,
                statuses,
                -1,
                "Return static range minimum " + answer + " from the two power-of-two blocks",
                LINE_RETURN,
                StepEventType.EXECUTE_LINE,
                facts(operation, "return", left, right, queryLevel, -1, leftStart, rightStart, table, built,
                        Long.toString(answer), Long.toString(answer), leftStart == rightStart ? "no" : "yes", SnapshotStatus.DONE));
        add(
                steps,
                values,
                table,
                built,
                statuses,
                -1,
                "Complete: range-min [" + left + "," + right + "] = " + answer,
                0,
                StepEventType.COMPLETE,
                facts(operation, "complete", left, right, queryLevel, -1, leftStart, rightStart, table, built,
                        Long.toString(answer), Long.toString(answer), leftStart == rightStart ? "no" : "yes", SnapshotStatus.DONE));
        return List.copyOf(steps);
    }

    private static int floorLog2(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("log2 input must be positive");
        }
        return 31 - Integer.numberOfLeadingZeros(value);
    }

    private static void add(
            List<SimulationStep> steps,
            int[] values,
            long[][] table,
            boolean[][] built,
            SnapshotStatus[] statuses,
            int focusIndex,
            String narration,
            int highlightedLine,
            StepEventType eventType,
            List<Fact> facts) {
        int currentLevel = Integer.parseInt(facts.stream().filter(f -> f.key().equals("level")).findFirst().orElseThrow().value());
        int leftBlock = Integer.parseInt(facts.stream().filter(f -> f.key().equals("left-block")).findFirst().orElseThrow().value());
        int rightBlock = Integer.parseInt(facts.stream().filter(f -> f.key().equals("right-block")).findFirst().orElseThrow().value());
        List<String> columns = new ArrayList<>();
        columns.add("Start");
        for (int level = 0; level < table.length; level++) columns.add("Len " + (1 << level));
        List<List<TypedCell>> rows = new ArrayList<>(values.length);
        for (int start = 0; start < values.length; start++) {
            List<TypedCell> row = new ArrayList<>();
            row.add(new TypedCell("sparse-start-" + start, Integer.toString(start), SnapshotStatus.DEFAULT));
            for (int level = 0; level < table.length; level++) {
                boolean valid = start + (1 << level) <= values.length;
                boolean active = level == currentLevel && (start == leftBlock || start == rightBlock
                        || (leftBlock < 0 && start == focusIndex));
                SnapshotStatus status = active ? SnapshotStatus.ACTIVE
                        : built[level][start] ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
                row.add(new TypedCell("sparse-" + level + "-" + start,
                        !valid ? "—" : built[level][start] ? Long.toString(table[level][start]) : "·", status));
            }
            rows.add(List.copyOf(row));
        }
        RemainingRangeSupport.addTableStep(
                steps,
                TYPE,
                columns,
                rows,
                highlightedLine,
                narration,
                eventType,
                facts);
    }

    private static String formatLevel(long[] row, boolean[] built, int valueCount, int length) {
        StringBuilder result = new StringBuilder("[");
        int count = valueCount - length + 1;
        for (int start = 0; start < valueCount; start++) {
            if (start > 0) {
                result.append(", ");
            }
            if (start < count && built[start]) {
                result.append(row[start]);
            } else if (start < count) {
                result.append("pending");
            } else {
                result.append("-");
            }
        }
        return result.append(']').toString();
    }

    private static String formatTable(long[][] table, boolean[][] built) {
        StringBuilder result = new StringBuilder();
        for (int level = 0; level < table.length; level++) {
            if (level > 0) {
                result.append("; ");
            }
            result.append("L").append(level).append('=').append(formatLevel(
                    table[level], built[level], table[0].length, 1 << level));
        }
        return result.toString();
    }

    private static List<Fact> facts(
            String operation,
            String phase,
            int left,
            int right,
            int level,
            int start,
            int leftBlock,
            int rightBlock,
            long[][] table,
            boolean[][] built,
            String answer,
            String result,
            String overlap,
            SnapshotStatus status) {
        return List.of(
                RemainingRangeSupport.fact("renderer", "sparse", SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("teaching-equation", leftBlock >= 0 && rightBlock >= 0
                        ? "min(T[" + level + "][" + leftBlock + "]=" + table[level][leftBlock]
                                + ", T[" + level + "][" + rightBlock + "]=" + table[level][rightBlock] + ") = "
                                + Math.min(table[level][leftBlock], table[level][rightBlock])
                        : phase.equals("build") && level > 0 && start >= 0
                                ? "min(" + table[level - 1][start] + ", " + table[level - 1][start + (1 << (level - 1))]
                                        + ") = " + table[level][start] : "pending", status),
                RemainingRangeSupport.fact("overlap-explanation", leftBlock >= 0
                        ? "Overlapping values do not change min: min(x, x) = x" : "pending", status),
                RemainingRangeSupport.fact("operation", operation, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("left", Integer.toString(left), status),
                RemainingRangeSupport.fact("right", Integer.toString(right), status),
                RemainingRangeSupport.fact("range", "[" + left + "," + right + "]", status),
                RemainingRangeSupport.fact("level", Integer.toString(level), status),
                RemainingRangeSupport.fact("block-length", level < 0 ? "pending" : Integer.toString(1 << level), status),
                RemainingRangeSupport.fact("start", Integer.toString(start), status),
                RemainingRangeSupport.fact("left-block", Integer.toString(leftBlock), status),
                RemainingRangeSupport.fact("right-block", Integer.toString(rightBlock), status),
                RemainingRangeSupport.fact("overlap", overlap, status),
                RemainingRangeSupport.fact("levels", formatTable(table, built), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("sparse-table", formatTable(table, built), SnapshotStatus.DEFAULT),
                RemainingRangeSupport.fact("answer", answer, status),
                RemainingRangeSupport.fact("result", result, status));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        ArrayNode values = input.putArray("values");
        for (int value : DEFAULT_VALUES) {
            values.add(value);
        }
        input.put("operation", "range-min");
        input.put("left", 1);
        input.put("right", 5);
        return new SimulationMetadata(
                TYPE,
                "Sparse Table",
                "O(n log n) preprocessing and O(1) static range-min query",
                "O(n log n)",
                RendererFamily.TABLE,
                input,
                "Enter JSON as {\"values\":[7,2,5,1,6,3],\"operation\":\"range-min\",\"left\":1,\"right\":5}; values length must be "
                        + MIN_VALUES + ".." + MAX_VALUES + ", the operation is range-min, indices are inclusive zero-based, and values have absolute value at most "
                        + MAX_ABS_VALUE + ".",
                PSEUDOCODE);
    }
}
