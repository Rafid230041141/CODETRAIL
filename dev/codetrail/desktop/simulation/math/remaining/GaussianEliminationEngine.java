package dev.codetrail.desktop.simulation.math.remaining;

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

/** Bounded augmented-matrix elimination with pivot, swap, scale, and rank facts. */
public final class GaussianEliminationEngine implements SimulationEngine {
    public static final String TYPE = "GAUSSIAN_ELIMINATION";
    public static final int MIN_EQUATIONS = 1;
    public static final int MAX_EQUATIONS = 4;
    public static final int MIN_VARIABLES = 1;
    public static final int MAX_VARIABLES = 4;
    public static final double MIN_VALUE = -1_000_000.0;
    public static final double MAX_VALUE = 1_000_000.0;
    public static final double EPSILON = 1.0e-10;
    public static final int MAX_TRACE_STEPS = RemainingMathSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_COLUMN = 3;
    private static final int LINE_PIVOT = 4;
    private static final int LINE_ZERO_PIVOT = 5;
    private static final int LINE_SWAP = 6;
    private static final int LINE_SCALE = 7;
    private static final int LINE_ELIMINATION = 9;
    private static final int LINE_ROW_OPERATION = 10;
    private static final int LINE_RANK = 11;
    private static final int LINE_CLASSIFY = 12;
    private static final int LINE_BACK_SUBSTITUTE = 13;
    private static final int LINE_RETURN = 14;

    private static final List<String> PSEUDOCODE = List.of(
            "gaussianElimination(augmented):",
            "    pivotRow = 0",
            "    for column = 0 through variables - 1:",
            "        choose the remaining row with largest absolute pivot",
            "        if pivot is zero: continue to the next column",
            "        swap the pivot row into pivotRow",
            "        scale the pivot row so its pivot is one",
            "        for each row below pivotRow:",
            "            factor = row[column]",
            "            row -= factor * pivotRow",
            "        pivotRow++",
            "    classify rank, consistency, and solution kind",
            "    back-substitute when the solution is unique",
            "    return echelon matrix and rank facts");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        double[][] parsed = parseMatrix(input);
        Model model = new Model(parsed);
        List<SimulationStep> steps = new ArrayList<>();

        add(steps, model, 0,
                "Initialize Gaussian elimination for a " + model.equations + " by " + model.variables
                        + " augmented system",
                StepEventType.INITIALIZE);
        model.phase = "method";
        add(steps, model, LINE_METHOD,
                "Use row operations to expose independent pivots and classify the system",
                StepEventType.EXECUTE_LINE);
        model.phase = "initialize";
        add(steps, model, LINE_INITIALIZE,
                "Set the pivot row to the first equation and retain the augmented column",
                StepEventType.EXECUTE_LINE);

        int pivotRow = 0;
        for (int column = 0; column < model.variables && pivotRow < model.equations; column++) {
            model.activeColumn = column;
            model.activeRow = pivotRow;
            model.pivotRow = pivotRow;
            model.rowEquation = "";
            model.backSubstitution = "";
            model.phase = "pivot";
            model.operation = "Inspect x" + (column + 1) + " for pivot R" + (pivotRow + 1);
            add(steps, model, LINE_COLUMN,
                    "Inspect coefficient column " + (column + 1) + " for pivot row " + (pivotRow + 1),
                    StepEventType.EXECUTE_LINE);

            int selected = pivotRow;
            for (int row = pivotRow + 1; row < model.equations; row++) {
                if (Math.abs(model.matrix[row][column]) > Math.abs(model.matrix[selected][column])) {
                    selected = row;
                }
            }
            model.selectedRow = selected;
            model.operation = "Select R" + (selected + 1) + " for x" + (column + 1);
            add(steps, model, LINE_PIVOT,
                    "Choose R" + (selected + 1) + ": |" + RemainingMathSupport.formatDouble(model.matrix[selected][column])
                            + "| is the largest remaining pivot magnitude",
                    StepEventType.EXECUTE_LINE);

            if (Math.abs(model.matrix[selected][column]) <= EPSILON) {
                model.operation = "Skip zero column x" + (column + 1);
                add(steps, model, LINE_ZERO_PIVOT,
                        "No non-zero pivot remains in this column; preserve the current pivot row",
                        StepEventType.EXECUTE_LINE);
                continue;
            }

            if (selected != pivotRow) {
                double[] temporary = model.matrix[selected];
                model.matrix[selected] = model.matrix[pivotRow];
                model.matrix[pivotRow] = temporary;
                model.operation = "R" + (selected + 1) + " ↔ R" + (pivotRow + 1);
                add(steps, model, LINE_SWAP,
                        "Swap the selected pivot row into the current pivot position",
                        StepEventType.EXECUTE_LINE);
            }

            model.selectedRow = pivotRow;
            double pivot = model.matrix[pivotRow][column];
            for (int value = 0; value <= model.variables; value++) {
                model.matrix[pivotRow][value] /= pivot;
                model.matrix[pivotRow][value] = clean(model.matrix[pivotRow][value]);
            }
            model.operation = "R" + (pivotRow + 1) + " ← R" + (pivotRow + 1) + " / " + RemainingMathSupport.formatDouble(pivot);
            add(steps, model, LINE_SCALE,
                    "Scale the pivot row so its leading coefficient is one",
                    StepEventType.EXECUTE_LINE);

            model.pivotColumns.add(column);
            for (int row = pivotRow + 1; row < model.equations; row++) {
                model.activeRow = row;
                double factor = model.matrix[row][column];
                model.eliminationFactor = factor;
                model.rowEquation = "R" + (row + 1) + " ← R" + (row + 1) + " − "
                        + RemainingMathSupport.formatDouble(factor) + " × R" + (pivotRow + 1);
                model.operation = model.rowEquation;
                add(steps, model, LINE_ELIMINATION,
                        "Inspect the row below the pivot and read its elimination factor",
                        StepEventType.EXECUTE_LINE);
                if (Math.abs(factor) <= EPSILON) {
                    model.operation = "R" + (row + 1) + " already has zero for x" + (column + 1);
                    add(steps, model, LINE_ROW_OPERATION,
                            "Leave the row unchanged because its pivot-column entry is already zero",
                            StepEventType.EXECUTE_LINE);
                    continue;
                }
                model.targetRowBefore = formatRow(model.matrix[row]);
                model.pivotRowValues = formatRow(model.matrix[pivotRow]);
                for (int value = 0; value <= model.variables; value++) {
                    model.matrix[row][value] = clean(
                            model.matrix[row][value] - factor * model.matrix[pivotRow][value]);
                }
                model.targetRowAfter = formatRow(model.matrix[row]);
                model.operation = model.rowEquation;
                add(steps, model, LINE_ROW_OPERATION,
                        model.rowEquation + "; eliminate the x" + (column + 1) + " coefficient",
                        StepEventType.EXECUTE_LINE);
                model.targetRowBefore = "";
                model.pivotRowValues = "";
                model.targetRowAfter = "";
            }
            pivotRow++;
            model.rank = pivotRow;
            model.activeRow = pivotRow < model.equations ? pivotRow : -1;
            model.operation = "rank = " + model.rank;
            add(steps, model, LINE_RANK,
                    "Record the successful pivot and advance the pivot row",
                    StepEventType.EXECUTE_LINE);
        }

        model.rank = model.pivotColumns.size();
        model.augmentedRank = rankOf(model.matrix, model.variables + 1);
        model.inconsistent = hasInconsistentRow(model.matrix, model.variables);
        model.outcome = model.inconsistent
                ? "inconsistent"
                : model.rank == model.variables ? "unique" : "infinite";
        model.freeColumns.clear();
        for (int column = 0; column < model.variables; column++) {
            if (!model.pivotColumns.contains(column)) {
                model.freeColumns.add(column);
            }
        }
        model.phase = "classify";
        model.pivotRow = -1;
        model.selectedRow = -1;
        model.rowEquation = "";
        model.operation = "rank(A) = " + model.rank + "; rank([A|b]) = " + model.augmentedRank + ": " + model.outcome;
        model.activeRow = -1;
        model.activeColumn = -1;
        add(steps, model, LINE_CLASSIFY,
                "Classify the echelon rows using coefficient rank and augmented rank",
                StepEventType.EXECUTE_LINE);

        if (model.outcome.equals("unique")) {
            model.solution = new double[model.variables];
            for (int pivot = model.rank - 1; pivot >= 0; pivot--) {
                int column = model.pivotColumns.get(pivot);
                double value = model.matrix[pivot][model.variables];
                for (int later = column + 1; later < model.variables; later++) {
                    value -= model.matrix[pivot][later] * model.solution[later];
                }
                model.solution[column] = clean(value / model.matrix[pivot][column]);
                StringBuilder equation = new StringBuilder("x" + (column + 1) + " = ("
                        + RemainingMathSupport.formatDouble(model.matrix[pivot][model.variables]));
                for (int later = column + 1; later < model.variables; later++) {
                    equation.append(" − ").append(RemainingMathSupport.formatDouble(model.matrix[pivot][later]))
                            .append(" × ").append(RemainingMathSupport.formatDouble(model.solution[later]));
                }
                model.backSubstitution = equation.append(") / ")
                        .append(RemainingMathSupport.formatDouble(model.matrix[pivot][column]))
                        .append(" = ").append(RemainingMathSupport.formatDouble(model.solution[column])).toString();
                model.operation = model.backSubstitution;
                model.activeRow = pivot;
                model.pivotRow = pivot;
                model.activeColumn = column;
                model.phase = "back-substitute";
                add(steps, model, LINE_BACK_SUBSTITUTE,
                        model.backSubstitution,
                        StepEventType.EXECUTE_LINE);
            }
        }

        model.activeRow = -1;
        model.activeColumn = -1;
        model.phase = "return";
        add(steps, model, LINE_RETURN,
                "Return the echelon matrix, rank, consistency, and solution facts",
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(steps, model, 0,
                "Complete Gaussian elimination with outcome " + model.outcome,
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
                columns(model), rows(model), facts(model), line, narration, eventType));
    }

    private static List<String> columns(Model model) {
        List<String> columns = new ArrayList<>();
        columns.add("row");
        for (int column = 0; column < model.variables; column++) {
            columns.add("x" + (column + 1));
        }
        columns.add("rhs");
        return List.copyOf(columns);
    }

    private static List<List<TypedCell>> rows(Model model) {
        List<List<TypedCell>> rows = new ArrayList<>();
        for (int row = 0; row < model.equations; row++) {
            SnapshotStatus status = rowStatus(model, row);
            List<TypedCell> cells = new ArrayList<>();
            cells.add(RemainingMathSupport.cell("row-" + row, Integer.toString(row + 1), status));
            for (int column = 0; column <= model.variables; column++) {
                SnapshotStatus cellStatus = row == model.pivotRow || row == model.selectedRow
                        ? SnapshotStatus.ACTIVE : status;
                if (row == model.activeRow && column == model.activeColumn) cellStatus = SnapshotStatus.ACTIVE;
                String key = column == model.variables ? "rhs-" + row : "x" + (column + 1) + "-" + row;
                cells.add(RemainingMathSupport.cell(key,
                        RemainingMathSupport.formatDouble(model.matrix[row][column]),
                        model.phase.equals("return") || model.phase.equals("complete") || model.phase.equals("classify")
                                ? status : cellStatus));
            }
            rows.add(List.copyOf(cells));
        }
        return List.copyOf(rows);
    }

    private static SnapshotStatus rowStatus(Model model, int row) {
        if (model.phase.equals("complete") || model.phase.equals("return")) {
            return isInconsistentRow(model.matrix[row], model.variables)
                    ? SnapshotStatus.REJECTED
                    : SnapshotStatus.DONE;
        }
        if (row == model.activeRow) {
            return SnapshotStatus.ACTIVE;
        }
        if (model.phase.equals("classify") && isInconsistentRow(model.matrix[row], model.variables)) {
            return SnapshotStatus.REJECTED;
        }
        if (row < model.rank && model.rank > 0) {
            return SnapshotStatus.DONE;
        }
        return SnapshotStatus.DEFAULT;
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus resultStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        String outcome = model.outcome;
        String solution = solutionText(model);
        return List.of(
                RemainingMathSupport.fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("pivot-row", model.pivotRow < 0 ? "none" : Integer.toString(model.pivotRow + 1), resultStatus),
                RemainingMathSupport.fact("selected-row", model.selectedRow < 0 ? "none" : Integer.toString(model.selectedRow + 1), resultStatus),
                RemainingMathSupport.fact("target-row", model.activeRow < 0 ? "none" : Integer.toString(model.activeRow + 1), resultStatus),
                RemainingMathSupport.fact("row-equation", model.rowEquation, resultStatus),
                RemainingMathSupport.fact("target-row-before", model.targetRowBefore, resultStatus),
                RemainingMathSupport.fact("pivot-row-values", model.pivotRowValues, resultStatus),
                RemainingMathSupport.fact("target-row-after", model.targetRowAfter, resultStatus),
                RemainingMathSupport.fact("back-substitution", model.backSubstitution, resultStatus),
                RemainingMathSupport.fact("teaching-equation", model.phase.equals("back-substitute")
                        ? model.backSubstitution : model.operation, resultStatus),
                RemainingMathSupport.fact("teaching-detail", "R1 is the first displayed equation; row operations preserve its solution set.", SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("phase", model.phase, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("equations", Integer.toString(model.equations), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("variables", Integer.toString(model.variables), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("rank", Integer.toString(model.rank), resultStatus),
                RemainingMathSupport.fact("coefficient-rank", Integer.toString(model.rank), resultStatus),
                RemainingMathSupport.fact("augmented-rank", Integer.toString(model.augmentedRank), resultStatus),
                RemainingMathSupport.fact("outcome", outcome, resultStatus),
                RemainingMathSupport.fact("solution-kind", outcome, resultStatus),
                RemainingMathSupport.fact("solution", solution, resultStatus),
                RemainingMathSupport.fact("solution-vector", solution, resultStatus),
                RemainingMathSupport.fact("pivot-columns", formatColumns(model.pivotColumns), resultStatus),
                RemainingMathSupport.fact("free-columns", formatColumns(model.freeColumns), resultStatus),
                RemainingMathSupport.fact("active-row", Integer.toString(model.activeRow),
                        model.activeRow >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("active-column", Integer.toString(model.activeColumn),
                        model.activeColumn >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("elimination-factor",
                        RemainingMathSupport.formatDouble(model.eliminationFactor),
                        model.activeRow >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("operation", model.operation, resultStatus),
                RemainingMathSupport.fact(
                        "invariant",
                        "row swaps, scaling, and row subtraction preserve the solution set",
                        resultStatus));
    }

    private static String formatRow(double[] row) {
        List<String> values = new ArrayList<>(row.length);
        for (double value : row) {
            values.add(RemainingMathSupport.formatDouble(value));
        }
        return RemainingMathSupport.formatStringList(values);
    }

    private static String solutionText(Model model) {
        if (model.outcome.equals("unique") && model.solution != null) {
            List<String> values = new ArrayList<>();
            for (double value : model.solution) {
                values.add(RemainingMathSupport.formatDouble(value));
            }
            return RemainingMathSupport.formatStringList(values);
        }
        if (model.outcome.equals("infinite")) {
            return "infinitely many";
        }
        if (model.outcome.equals("inconsistent")) {
            return "none";
        }
        return "pending";
    }

    private static String formatColumns(List<Integer> columns) {
        List<String> names = new ArrayList<>();
        for (Integer column : columns) {
            names.add("x" + (column + 1));
        }
        return RemainingMathSupport.formatStringList(names);
    }

    private static int rankOf(double[][] source, int columns) {
        double[][] matrix = new double[source.length][];
        for (int row = 0; row < source.length; row++) {
            matrix[row] = Arrays.copyOf(source[row], columns);
        }
        int rank = 0;
        for (int column = 0; column < columns && rank < matrix.length; column++) {
            int selected = rank;
            for (int row = rank + 1; row < matrix.length; row++) {
                if (Math.abs(matrix[row][column]) > Math.abs(matrix[selected][column])) {
                    selected = row;
                }
            }
            if (Math.abs(matrix[selected][column]) <= EPSILON) {
                continue;
            }
            double[] temporary = matrix[selected];
            matrix[selected] = matrix[rank];
            matrix[rank] = temporary;
            double pivot = matrix[rank][column];
            for (int row = rank + 1; row < matrix.length; row++) {
                double factor = matrix[row][column] / pivot;
                for (int value = column; value < columns; value++) {
                    matrix[row][value] = clean(matrix[row][value] - factor * matrix[rank][value]);
                }
            }
            rank++;
        }
        return rank;
    }

    private static boolean hasInconsistentRow(double[][] matrix, int variables) {
        for (double[] row : matrix) {
            if (isInconsistentRow(row, variables)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInconsistentRow(double[] row, int variables) {
        boolean coefficientsZero = true;
        for (int column = 0; column < variables; column++) {
            if (Math.abs(row[column]) > EPSILON) {
                coefficientsZero = false;
                break;
            }
        }
        return coefficientsZero && Math.abs(row[variables]) > EPSILON;
    }

    private static double clean(double value) {
        return Math.abs(value) <= EPSILON ? 0.0 : value;
    }

    private static double[][] parseMatrix(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(TYPE + " input must be a JSON object");
        }
        JsonNode matrixNode = input.get("matrix");
        if (matrixNode == null || !matrixNode.isArray()) {
            throw new IllegalArgumentException(TYPE + " matrix must be an array of rows");
        }
        int equations = matrixNode.size();
        if (equations < MIN_EQUATIONS || equations > MAX_EQUATIONS) {
            throw new IllegalArgumentException(TYPE + " requires " + MIN_EQUATIONS + ".." + MAX_EQUATIONS
                    + " equations");
        }
        JsonNode firstRow = matrixNode.get(0);
        if (firstRow == null || !firstRow.isArray()) {
            throw new IllegalArgumentException(TYPE + " matrix rows must be arrays");
        }
        int columns = firstRow.size();
        int variables = columns - 1;
        if (variables < MIN_VARIABLES || variables > MAX_VARIABLES) {
            throw new IllegalArgumentException(TYPE + " requires " + MIN_VARIABLES + ".." + MAX_VARIABLES
                    + " variables plus one augmented column");
        }
        double[][] result = new double[equations][columns];
        for (int row = 0; row < equations; row++) {
            JsonNode rowNode = matrixNode.get(row);
            if (rowNode == null || !rowNode.isArray() || rowNode.size() != columns) {
                throw new IllegalArgumentException(TYPE + " matrix must be rectangular with " + columns + " columns");
            }
            for (int column = 0; column < columns; column++) {
                JsonNode valueNode = rowNode.get(column);
                if (valueNode == null || !valueNode.isNumber()) {
                    throw new IllegalArgumentException(TYPE + " matrix values must be finite numbers");
                }
                double value = valueNode.doubleValue();
                if (!Double.isFinite(value) || value < MIN_VALUE || value > MAX_VALUE) {
                    throw new IllegalArgumentException(TYPE + " matrix values must be finite and in the inclusive range "
                            + RemainingMathSupport.formatDouble(MIN_VALUE) + ".."
                            + RemainingMathSupport.formatDouble(MAX_VALUE));
                }
                result[row][column] = clean(value);
            }
        }
        return result;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode matrix = defaultInput.putArray("matrix");
        ArrayNode first = matrix.addArray();
        first.add(2);
        first.add(1);
        first.add(5);
        ArrayNode second = matrix.addArray();
        second.add(1);
        second.add(-1);
        second.add(1);
        return new SimulationMetadata(
                TYPE,
                "Matrix Ops / Gaussian Elimination",
                "O(nm·min(n,m))",
                "O(1) beyond matrix",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"matrix\":[[2,1,5],[1,-1,1]]}; use a rectangular augmented matrix with "
                        + MIN_EQUATIONS + ".." + MAX_EQUATIONS + " equations, " + MIN_VARIABLES + ".."
                        + MAX_VARIABLES + " variables, and finite values in " + RemainingMathSupport.formatDouble(MIN_VALUE)
                        + ".." + RemainingMathSupport.formatDouble(MAX_VALUE) + ".",
                PSEUDOCODE);
    }

    private static final class Model {
        private final int equations;
        private final int variables;
        private final double[][] matrix;
        private final List<Integer> pivotColumns = new ArrayList<>();
        private final List<Integer> freeColumns = new ArrayList<>();
        private int rank;
        private int augmentedRank;
        private int activeRow = -1;
        private int activeColumn = -1;
        private int selectedRow = -1;
        private int pivotRow = -1;
        private String rowEquation = "";
        private String targetRowBefore = "";
        private String pivotRowValues = "";
        private String targetRowAfter = "";
        private String backSubstitution = "";
        private double eliminationFactor;
        private boolean inconsistent;
        private String outcome = "pending";
        private double[] solution;
        private String phase = "initialize";
        private String operation = "none";

        private Model(double[][] matrix) {
            this.equations = matrix.length;
            this.variables = matrix[0].length - 1;
            this.matrix = matrix;
        }
    }
}
