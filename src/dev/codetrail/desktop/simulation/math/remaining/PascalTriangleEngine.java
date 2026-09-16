package dev.codetrail.desktop.simulation.math.remaining;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Pascal triangle construction with explicit parent dependencies. */
public final class PascalTriangleEngine implements SimulationEngine {
    public static final String TYPE = "PASCAL_TRIANGLE";
    public static final int MIN_N = 0;
    public static final int MAX_N = 12;
    public static final int MAX_TRACE_STEPS = RemainingMathSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_ROW = 3;
    private static final int LINE_BOUNDARY = 4;
    private static final int LINE_INTERIOR = 5;
    private static final int LINE_DEPENDENCIES = 6;
    private static final int LINE_RIGHT_PARENT = 7;
    private static final int LINE_ASSIGN = 8;
    private static final int LINE_RETURN = 9;

    private static final List<String> PSEUDOCODE = List.of(
            "pascal(n):",
            "    triangle[0][0] = 1",
            "    for row = 1 through n, and column = 0 through row:",
            "        if column == 0 or column == row: triangle[row][column] = 1",
            "        else:  // interior entry",
            "            left = triangle[row - 1][column - 1]",
            "            right = triangle[row - 1][column]",
            "            triangle[row][column] = left + right",
            "    return triangle and row n");

    private static final List<String> TABLE_COLUMNS = List.of(
            "row", "column", "value", "left-parent", "right-parent", "dependency");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int n = (int) RemainingMathSupport.readLong(input, TYPE, "n", MIN_N, MAX_N);
        Model model = new Model(n);
        List<SimulationStep> steps = new ArrayList<>();

        add(steps, model, 0,
                "Initialize Pascal's triangle through row " + n,
                StepEventType.INITIALIZE);
        model.phase = "method";
        add(steps, model, LINE_METHOD,
                "Build each binomial row from its boundary values and two parents",
                StepEventType.EXECUTE_LINE);
        model.phase = "initialize";
        model.currentRow = 0;
        model.currentColumn = 0;
        model.triangle.get(0).get(0).value = BigInteger.ONE;
        model.triangle.get(0).get(0).operation = "boundary";
        model.triangle.get(0).get(0).dependency = "boundary = 1";
        model.completedRows = 1;
        add(steps, model, LINE_INITIALIZE,
                "Set C(0,0) = 1, the first row of the triangle",
                StepEventType.EXECUTE_LINE);

        for (int row = 1; row <= n; row++) {
            model.currentRow = row;
            model.currentColumn = -1;
            model.phase = "row";
            add(steps, model, LINE_ROW,
                    "Start binomial row " + row,
                    StepEventType.EXECUTE_LINE);
            for (int column = 0; column <= row; column++) {
                model.currentColumn = column;
                Entry entry = model.triangle.get(row).get(column);
                add(steps, model, LINE_ROW,
                        "Inspect entry C(" + row + "," + column + ")",
                        StepEventType.EXECUTE_LINE);
                if (column == 0 || column == row) {
                    entry.value = BigInteger.ONE;
                    entry.leftParent = "-";
                    entry.rightParent = "-";
                    entry.dependency = "boundary = 1";
                    entry.operation = "boundary";
                    add(steps, model, LINE_BOUNDARY,
                            "Set the row boundary C(" + row + "," + column + ") to 1",
                            StepEventType.EXECUTE_LINE);
                } else {
                    add(steps, model, LINE_INTERIOR,
                            "Interior entry: combine the two values from the preceding row",
                            StepEventType.EXECUTE_LINE);
                    Entry left = model.triangle.get(row - 1).get(column - 1);
                    Entry right = model.triangle.get(row - 1).get(column);
                    if (left.value == null || right.value == null) {
                        throw new IllegalStateException("Pascal dependency was not computed before its child");
                    }
                    entry.leftParent = "C(" + (row - 1) + "," + (column - 1) + ")=" + left.value;
                    entry.rightParent = "C(" + (row - 1) + "," + column + ")=" + right.value;
                    entry.dependency = entry.leftParent + " + " + entry.rightParent;
                    entry.operation = "add parents";
                    add(steps, model, LINE_DEPENDENCIES,
                            "Read the two entries directly above C(" + row + "," + column + ")",
                            StepEventType.EXECUTE_LINE);
                    add(steps, model, LINE_RIGHT_PARENT,
                            "Read the right parent " + entry.rightParent,
                            StepEventType.EXECUTE_LINE);
                    entry.value = left.value.add(right.value);
                    add(steps, model, LINE_ASSIGN,
                            left.value + " + " + right.value + " = " + entry.value
                                    + "; store C(" + row + "," + column + ")",
                            StepEventType.EXECUTE_LINE);
                }
            }
            model.completedRows = row + 1;
            add(steps, model, LINE_ROW,
                    "Finish row " + row + " and retain it for later dependencies",
                    StepEventType.EXECUTE_LINE);
        }

        model.currentRow = -1;
        model.currentColumn = -1;
        model.phase = "return";
        add(steps, model, LINE_RETURN,
                "Return the complete triangle and requested final row",
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(steps, model, 0,
                "Complete Pascal triangle through row " + n,
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration,
            StepEventType eventType) {
        if (steps.size() >= MAX_TRACE_STEPS) {
            throw new IllegalStateException(TYPE + " trace exceeded bounded step limit");
        }
        steps.add(RemainingMathSupport.tableStep(
                TABLE_COLUMNS, rows(model), facts(model), line, narration, eventType));
    }

    private static List<List<TypedCell>> rows(Model model) {
        List<List<TypedCell>> rows = new ArrayList<>();
        for (int row = 0; row < model.triangle.size(); row++) {
            List<Entry> entries = model.triangle.get(row);
            for (Entry entry : entries) {
                boolean current = entry.row == model.currentRow && entry.column == model.currentColumn;
                boolean parent = model.currentRow > 0 && model.currentColumn > 0
                        && model.currentColumn < model.currentRow && entry.row == model.currentRow - 1
                        && (entry.column == model.currentColumn - 1 || entry.column == model.currentColumn);
                SnapshotStatus status = current || parent
                        ? SnapshotStatus.ACTIVE
                        : entry.value == null ? SnapshotStatus.DEFAULT : SnapshotStatus.DONE;
                rows.add(RemainingMathSupport.row(
                        RemainingMathSupport.cell("row-" + entry.row + "-" + entry.column,
                                Integer.toString(entry.row), status),
                        RemainingMathSupport.cell("column-" + entry.row + "-" + entry.column,
                                Integer.toString(entry.column), status),
                        RemainingMathSupport.cell("value-" + entry.row + "-" + entry.column,
                                entry.value == null ? "?" : entry.value.toString(), status),
                        RemainingMathSupport.cell("left-parent-" + entry.row + "-" + entry.column,
                                entry.leftParent, status),
                        RemainingMathSupport.cell("right-parent-" + entry.row + "-" + entry.column,
                                entry.rightParent, status),
                        RemainingMathSupport.cell("dependency-" + entry.row + "-" + entry.column,
                                entry.dependency, status)));
            }
        }
        return List.copyOf(rows);
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus resultStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        List<String> finalRow = new ArrayList<>();
        for (Entry entry : model.triangle.get(model.n)) {
            finalRow.add(entry.value == null ? "?" : entry.value.toString());
        }
        String operation = model.currentRow < 0
                ? "none"
                : model.triangle.get(model.currentRow).get(Math.max(0, model.currentColumn)).operation;
        boolean interior = model.currentRow > 0 && model.currentColumn > 0 && model.currentColumn < model.currentRow;
        String sum = "";
        if (interior) {
            Entry left = model.triangle.get(model.currentRow - 1).get(model.currentColumn - 1);
            Entry right = model.triangle.get(model.currentRow - 1).get(model.currentColumn);
            Entry child = model.triangle.get(model.currentRow).get(model.currentColumn);
            sum = "C(" + model.currentRow + "," + model.currentColumn + ") = " + left.value + " + " + right.value
                    + " = " + (child.value == null ? "?" : child.value);
        }
        return List.of(
                RemainingMathSupport.fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("parent-left-row", interior ? Integer.toString(model.currentRow - 1) : "none", resultStatus),
                RemainingMathSupport.fact("parent-left-column", interior ? Integer.toString(model.currentColumn - 1) : "none", resultStatus),
                RemainingMathSupport.fact("parent-right-row", interior ? Integer.toString(model.currentRow - 1) : "none", resultStatus),
                RemainingMathSupport.fact("parent-right-column", interior ? Integer.toString(model.currentColumn) : "none", resultStatus),
                RemainingMathSupport.fact("parent-sum", sum, resultStatus),
                RemainingMathSupport.fact("teaching-equation", sum, resultStatus),
                RemainingMathSupport.fact("teaching-detail", "The two highlighted entries in the previous row produce the highlighted child.", SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("phase", model.phase, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("n", Integer.toString(model.n), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("current-row", Integer.toString(model.currentRow), resultStatus),
                RemainingMathSupport.fact("current-column", Integer.toString(model.currentColumn), resultStatus),
                RemainingMathSupport.fact("completed-rows", Integer.toString(model.completedRows), resultStatus),
                RemainingMathSupport.fact("triangle", formatTriangle(model), resultStatus),
                RemainingMathSupport.fact("row-values", RemainingMathSupport.formatStringList(finalRow), resultStatus),
                RemainingMathSupport.fact("target-row", RemainingMathSupport.formatStringList(finalRow), resultStatus),
                RemainingMathSupport.fact("result", RemainingMathSupport.formatStringList(finalRow), resultStatus),
                RemainingMathSupport.fact("operation", operation, resultStatus),
                RemainingMathSupport.fact(
                        "invariant",
                        "each interior entry equals the sum of its two entries in the preceding row",
                        resultStatus));
    }

    private static String formatTriangle(Model model) {
        List<List<String>> values = new ArrayList<>();
        for (List<Entry> row : model.triangle) {
            List<String> rowValues = new ArrayList<>();
            for (Entry entry : row) {
                rowValues.add(entry.value == null ? "?" : entry.value.toString());
            }
            values.add(rowValues);
        }
        return RemainingMathSupport.formatRows(values);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("n", 6);
        return new SimulationMetadata(
                TYPE,
                "Combinatorics (nCr / Pascal's Triangle)",
                "O(n²)",
                "O(n²)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"n\":6}; n must be an integer from " + MIN_N + " through " + MAX_N
                        + ". Each interior cell is computed from its two preceding-row parents.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final int n;
        private final List<List<Entry>> triangle = new ArrayList<>();
        private int currentRow = -1;
        private int currentColumn = -1;
        private int completedRows;
        private String phase = "initialize";

        private Model(int n) {
            this.n = n;
            for (int row = 0; row <= n; row++) {
                List<Entry> entries = new ArrayList<>();
                for (int column = 0; column <= row; column++) {
                    entries.add(new Entry(row, column));
                }
                triangle.add(entries);
            }
        }
    }

    private static final class Entry {
        private final int row;
        private final int column;
        private BigInteger value;
        private String leftParent = "-";
        private String rightParent = "-";
        private String dependency = "pending";
        private String operation = "pending";

        private Entry(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }
}
