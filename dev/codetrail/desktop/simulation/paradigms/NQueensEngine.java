package dev.codetrail.desktop.simulation.paradigms;

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
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Backtracking N-Queens trace showing every try, rejection, placement, and undo. */
public final class NQueensEngine implements SimulationEngine {
    public static final String TYPE = "BACKTRACKING_N_QUEENS";
    public static final int MIN_N = 1;
    public static final int MAX_N = 6;
    public static final int MAX_TRACE_STEPS = 4096;

    private static final int LINE_SEARCH = 1;
    private static final int LINE_RECORD = 2;
    private static final int LINE_TRY = 4;
    private static final int LINE_SAFE = 5;
    private static final int LINE_PLACE = 6;
    private static final int LINE_RECURSE = 7;
    private static final int LINE_UNDO = 8;
    private static final int LINE_REJECT = 9;

    private static final int DEFAULT_N = 4;
    private static final List<String> PSEUDOCODE = List.of(
            "search(row):",
            "    if row == n: record solution; return",
            "    for col = 0 .. n - 1:",
            "        try (row, col)",
            "        if safe(row, col):",
            "            place queen(row, col)",
            "            search(row + 1)",
            "            undo queen(row, col)",
            "        else: reject (row, col)");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        ParadigmSupport.requireObject(input, TYPE);
        int n = ParadigmSupport.boundedInt(
                ParadigmSupport.requireField(input, TYPE, "n"),
                TYPE + " n",
                MIN_N,
                MAX_N);
        Model model = new Model(n);
        List<SimulationStep> steps = new ArrayList<>();

        model.phase = "initialize";
        add(
                steps,
                model,
                0,
                "Initialize an " + n + " by " + n + " board and enumerate all solutions",
                StepEventType.INITIALIZE);
        solve(model, 0, steps);

        model.searchRow = -1;
        model.attemptRow = -1;
        model.attemptColumn = -1;
        model.attemptStatus = null;
        model.clearSafety();
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: " + model.solutions.size() + " non-attacking solution(s)",
                StepEventType.COMPLETE);
        return List.copyOf(steps);
    }

    private void solve(Model model, int row, List<SimulationStep> steps) {
        model.searchRow = row;
        model.clearSafety();
        model.phase = "search";
        add(steps, model, LINE_SEARCH, "Search row " + row, StepEventType.EXECUTE_LINE);
        if (row == model.n) {
            model.solutions.add(model.placement.clone());
            model.phase = "record";
            model.attemptRow = -1;
            model.attemptColumn = -1;
            model.attemptStatus = null;
            add(
                    steps,
                    model,
                    LINE_RECORD,
                    "Record solution " + Arrays.toString(model.placement),
                    StepEventType.EXECUTE_LINE);
            return;
        }

        for (int column = 0; column < model.n; column++) {
            model.searchRow = row;
            model.attemptRow = row;
            model.attemptColumn = column;
            model.attemptStatus = SnapshotStatus.ACTIVE;
            model.clearSafety();
            model.phase = "try";
            add(
                    steps,
                    model,
                    LINE_TRY,
                    "Try row " + row + ", column " + column,
                    StepEventType.EXECUTE_LINE);

            boolean safe = model.checkSafety(row, column);
            model.phase = "safety-check";
            model.attemptStatus = safe ? SnapshotStatus.ACTIVE : SnapshotStatus.REJECTED;
            add(steps, model, LINE_SAFE, model.safetyCheck, StepEventType.EXECUTE_LINE);
            model.phase = safe ? "safe" : "fail";
            if (!safe) {
                model.attemptStatus = SnapshotStatus.REJECTED;
                add(
                        steps,
                        model,
                        LINE_REJECT,
                        "Reject (" + row + "," + column + "): " + model.safetyCheck,
                        StepEventType.EXECUTE_LINE);
                model.attemptRow = -1;
                model.attemptColumn = -1;
                model.attemptStatus = null;
                continue;
            }

            model.place(row, column);
            model.attemptRow = -1;
            model.attemptColumn = -1;
            model.attemptStatus = null;
            model.phase = "place";
            add(
                    steps,
                    model,
                    LINE_PLACE,
                    "Place queen at row " + row + ", column " + column,
                    StepEventType.EXECUTE_LINE);
            model.phase = "recurse";
            add(
                    steps,
                    model,
                    LINE_RECURSE,
                    "Search the next row after placing (" + row + "," + column + ")",
                    StepEventType.EXECUTE_LINE);
            solve(model, row + 1, steps);

            model.searchRow = row;
            model.attemptRow = -1;
            model.attemptColumn = -1;
            model.attemptStatus = null;
            model.clearSafety();
            model.remove(row, column);
            model.phase = "undo";
            add(
                    steps,
                    model,
                    LINE_UNDO,
                    "Undo queen at row " + row + ", column " + column,
                    StepEventType.EXECUTE_LINE);
            model.undoRow = -1;
            model.undoColumn = -1;
        }
    }

    private static void add(
            List<SimulationStep> steps,
            Model model,
            int line,
            String narration,
            StepEventType eventType) {
        ParadigmSupport.add(
                steps,
                model.state(),
                Set.of(),
                Set.of(),
                line,
                narration,
                eventType,
                TYPE,
                MAX_TRACE_STEPS);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("n", DEFAULT_N);
        return new SimulationMetadata(
                TYPE,
                "Backtracking: N-Queens",
                "O(n!)",
                "O(n²)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"n\":4}; n must be an integer from " + MIN_N + " through " + MAX_N
                        + ". The trace enumerates all solutions and explicitly undoes each tentative queen.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final int n;
        private final int[] placement;
        private final boolean[] columns;
        private final boolean[] downDiagonals;
        private final boolean[] upDiagonals;
        private final List<int[]> solutions = new ArrayList<>();
        private int searchRow = 0;
        private int attemptRow = -1;
        private int attemptColumn = -1;
        private int undoRow = -1;
        private int undoColumn = -1;
        private SnapshotStatus attemptStatus;
        private int attackerRow = -1;
        private int attackerColumn = -1;
        private String safetyCheck = "not evaluated";
        private String phase = "pending";

        private Model(int n) {
            this.n = n;
            this.placement = new int[n];
            Arrays.fill(this.placement, -1);
            this.columns = new boolean[n];
            this.downDiagonals = new boolean[2 * n - 1];
            this.upDiagonals = new boolean[2 * n - 1];
        }

        private void clearSafety() {
            attackerRow = -1;
            attackerColumn = -1;
            safetyCheck = "not evaluated";
        }

        private boolean checkSafety(int row, int column) {
            clearSafety();
            for (int previous = 0; previous < row; previous++) {
                int queenColumn = placement[previous];
                if (queenColumn == column || Math.abs(row - previous) == Math.abs(column - queenColumn)) {
                    attackerRow = previous;
                    attackerColumn = queenColumn;
                    safetyCheck = queenColumn == column
                            ? "Queen (" + previous + "," + queenColumn + ") shares column " + column
                            : "Queen (" + previous + "," + queenColumn + "): |" + row + "-" + previous
                                    + "| = |" + column + "-" + queenColumn + "| = " + (row - previous)
                                    + " (same diagonal)";
                    return false;
                }
            }
            safetyCheck = "Safe: column " + column + ", row-col " + (row - column)
                    + ", row+col " + (row + column) + " are unused";
            return true;
        }

        private boolean isSafe(int row, int column) {
            return !columns[column]
                    && !downDiagonals[row - column + n - 1]
                    && !upDiagonals[row + column];
        }

        private void place(int row, int column) {
            if (placement[row] != -1 || !isSafe(row, column)) {
                throw new IllegalStateException("cannot place an unsafe queen");
            }
            placement[row] = column;
            columns[column] = true;
            downDiagonals[row - column + n - 1] = true;
            upDiagonals[row + column] = true;
        }

        private void remove(int row, int column) {
            if (placement[row] != column) {
                throw new IllegalStateException("undo must remove the current row queen");
            }
            placement[row] = -1;
            undoRow = row;
            undoColumn = column;
            columns[column] = false;
            downDiagonals[row - column + n - 1] = false;
            upDiagonals[row + column] = false;
        }

        private TableState state() {
            List<String> columns = new ArrayList<>(n + 1);
            columns.add("row");
            for (int column = 0; column < n; column++) {
                columns.add("c" + column);
            }
            List<List<TypedCell>> rows = new ArrayList<>(n);
            int deepest = deepestPlacedRow();
            for (int row = 0; row < n; row++) {
                SnapshotStatus rowStatus = SnapshotStatus.DEFAULT;
                if (placement[row] != -1) {
                    rowStatus = row == deepest ? SnapshotStatus.ACTIVE : SnapshotStatus.DONE;
                }
                if (attemptRow == row && attemptStatus != null) {
                    rowStatus = attemptStatus;
                }
                List<TypedCell> cells = new ArrayList<>(n + 1);
                cells.add(ParadigmSupport.cell(
                        "row-" + row,
                        Integer.toString(row),
                        rowStatus));
                for (int column = 0; column < n; column++) {
                    SnapshotStatus status = SnapshotStatus.DEFAULT;
                    String value = ".";
                    if (placement[row] == column) {
                        status = row == attackerRow && column == attackerColumn && attemptRow >= 0
                                ? SnapshotStatus.REJECTED
                                : row == deepest ? SnapshotStatus.ACTIVE : SnapshotStatus.DONE;
                        value = "Q";
                    } else if (attemptRow == row && attemptColumn == column && attemptStatus != null) {
                        status = attemptStatus;
                        value = attemptStatus == SnapshotStatus.REJECTED ? "x" : "?";
                    }
                    cells.add(ParadigmSupport.cell(
                            "board-" + row + "-" + column,
                            value,
                            status));
                }
                rows.add(List.copyOf(cells));
            }
            List<Fact> facts = List.of(
                    ParadigmSupport.fact("algorithm", "N_QUEENS", SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("teaching-equation", safetyCheck.equals("not evaluated")
                            ? phase.equals("complete") ? "Found " + solutions.size() + " solutions; every tentative queen has been undone"
                            : "Place one queen per row; columns and diagonals must stay free" : safetyCheck, SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("teaching-detail", attackerRow >= 0 && attemptRow >= 0
                            ? "Candidate (" + attemptRow + "," + attemptColumn + ") is attacked by queen ("
                                    + attackerRow + "," + attackerColumn + ")."
                            : "A diagonal shares row-col or row+col. Undo a queen before trying the next column.", SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("target-row", Integer.toString(attemptRow), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("target-column", Integer.toString(attemptColumn < 0 ? -1 : attemptColumn + 1), SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("n", Integer.toString(n), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("phase", phase, phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("row", Integer.toString(searchRow), searchRow < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("undo-row", Integer.toString(undoRow), undoRow < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("undo-column", Integer.toString(undoColumn), undoColumn < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("attempt-row", Integer.toString(attemptRow), attemptRow < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("attempt-column", Integer.toString(attemptColumn), attemptColumn < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                    ParadigmSupport.fact("attacker-row", Integer.toString(attackerRow), attackerRow < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.REJECTED),
                    ParadigmSupport.fact("attacker-column", Integer.toString(attackerColumn), attackerColumn < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.REJECTED),
                    ParadigmSupport.fact("safety-check", safetyCheck, attackerRow < 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.REJECTED),
                    ParadigmSupport.fact("solutions", Integer.toString(solutions.size()), phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("placement", ParadigmSupport.formatInts(placement), SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("solution-placements", ParadigmSupport.formatNestedInts(solutions), phase.equals("complete") ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT),
                    ParadigmSupport.fact("search-mode", "all-solutions", SnapshotStatus.DEFAULT));
            return new TableState(columns, rows, facts);
        }

        private int deepestPlacedRow() {
            for (int row = n - 1; row >= 0; row--) {
                if (placement[row] != -1) {
                    return row;
                }
            }
            return -1;
        }
    }
}
