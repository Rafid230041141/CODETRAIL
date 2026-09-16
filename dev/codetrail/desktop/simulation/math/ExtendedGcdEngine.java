package dev.codetrail.desktop.simulation.math;

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
import java.util.ArrayList;
import java.util.List;

/** Iterative Euclidean trace with the coefficient certificate for the gcd. */
public final class ExtendedGcdEngine implements SimulationEngine {
    public static final String TYPE = "EXTENDED_GCD";
    public static final long MIN_INPUT = -1_000_000L;
    public static final long MAX_INPUT = 1_000_000L;
    public static final long MAX_ABS_VALUE = 1_000_000L;
    public static final int MAX_TRACE_STEPS = MathSimulationSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_BOTH_ZERO = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_WHILE = 4;
    private static final int LINE_QUOTIENT = 5;
    private static final int LINE_REMAINDER = 6;
    private static final int LINE_COEFFICIENTS = 7;
    private static final int LINE_UPDATE_REMAINDERS = 8;
    private static final int LINE_UPDATE_X = 9;
    private static final int LINE_UPDATE_Y = 10;
    private static final int LINE_NORMALIZE = 11;
    private static final int LINE_RETURN = 12;

    private static final List<String> PSEUDOCODE = List.of(
            "extendedGcd(a, b):",
            "    if a == 0 and b == 0: return (0, 0, 0)",
            "    oldR = a; r = b; oldX = 1; x = 0; oldY = 0; y = 1",
            "    while r != 0:",
            "        q = oldR / r",
            "        remainder = oldR % r",
            "        nextX = oldX - q * x; nextY = oldY - q * y",
            "        (oldR, r, oldX, x, oldY, y) = (r, remainder, x, nextX, y, nextY)",
            "        check a * oldX + b * oldY == oldR",
            "        check a * x + b * y == r",
            "    if oldR < 0: oldR = -oldR; oldX = -oldX; oldY = -oldY",
            "    return (oldR, oldX, oldY)");

    private static final List<String> TABLE_COLUMNS = List.of(
            "step", "r0", "r1", "quotient", "remainder", "x0", "x1", "next-x", "y0", "y1", "next-y");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        long a = MathSimulationSupport.readBoundedLong(
                input, TYPE, "a", MIN_INPUT, MAX_INPUT);
        long b = MathSimulationSupport.readBoundedLong(
                input, TYPE, "b", MIN_INPUT, MAX_INPUT);
        Model model = new Model(a, b);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                model,
                0,
                "Initialize extended Euclid for a = " + a + " and b = " + b,
                StepEventType.INITIALIZE);
        add(
                steps,
                model,
                LINE_METHOD,
                "Compute a gcd together with its Bezout coefficient certificate",
                StepEventType.EXECUTE_LINE);
        model.phase = "guard";
        add(
                steps,
                model,
                LINE_BOTH_ZERO,
                model.bothZero
                        ? "Both inputs are zero; explicitly choose gcd = 0, x = 0, y = 0"
                        : "Check the bothzero guard before starting Euclid's updates",
                StepEventType.EXECUTE_LINE);

        if (model.bothZero) {
            model.resultGcd = 0L;
            model.resultX = 0L;
            model.resultY = 0L;
            model.resultReady = true;
            model.phase = "return";
            add(
                    steps,
                    model,
                    LINE_RETURN,
                    "Return the explicit bothzero result gcd = 0, x = 0, y = 0",
                    StepEventType.EXECUTE_LINE);
            model.phase = "complete";
            add(
                    steps,
                    model,
                    0,
                    "Complete: gcd = 0 with the zero certificate 0 * 0 + 0 * 0 = 0",
                    StepEventType.COMPLETE);
            return List.copyOf(steps);
        }

        model.phase = "initialize";
        add(
                steps,
                model,
                LINE_INITIALIZE,
                "Initialize remainder pair (oldR, r) = (" + model.oldR + ", " + model.r
                        + ") and coefficient pairs for a and b",
                StepEventType.EXECUTE_LINE);

        while (model.r != 0L) {
            int stepNumber = model.nextStep++;
            model.currentRow = EuclidRow.pending(
                    stepNumber,
                    model.oldR,
                    model.r,
                    model.oldX,
                    model.x,
                    model.oldY,
                    model.y);
            model.phase = "iterate";
            add(
                    steps,
                    model,
                    LINE_WHILE,
                    "r = " + model.r + " is nonzero; perform Euclid update " + stepNumber,
                    StepEventType.EXECUTE_LINE);

            long quotient = model.oldR / model.r;
            long remainder = model.oldR % model.r;
            model.lastQuotient = quotient;
            model.lastRemainder = remainder;
            model.currentRow = EuclidRow.computed(
                    stepNumber,
                    model.oldR,
                    model.r,
                    quotient,
                    remainder,
                    model.oldX,
                    model.x,
                    model.oldX - quotient * model.x,
                    model.oldY,
                    model.y,
                    model.oldY - quotient * model.y);
            add(
                    steps,
                    model,
                    LINE_QUOTIENT,
                    "Compute quotient q = " + quotient + " from " + model.currentRow.r0 + " / "
                            + model.currentRow.r1,
                    StepEventType.EXECUTE_LINE);
            add(
                    steps,
                    model,
                    LINE_REMAINDER,
                    "Compute the actual remainder " + remainder + " so " + model.currentRow.r0
                            + " = " + quotient + " * " + model.currentRow.r1 + " + " + remainder,
                    StepEventType.EXECUTE_LINE);

            long nextX = model.oldX - quotient * model.x;
            long nextY = model.oldY - quotient * model.y;
            add(
                    steps,
                    model,
                    LINE_COEFFICIENTS,
                    "nextX = " + model.oldX + " − " + quotient + " × " + model.x + " = " + nextX
                            + "; nextY = " + model.oldY + " − " + quotient + " × " + model.y + " = " + nextY,
                    StepEventType.EXECUTE_LINE);

            // Apply the complete tuple before emitting the three visible update
            // lines, so every immutable snapshot continues to satisfy both
            // coefficient invariants.
            long previousR = model.r;
            model.oldR = previousR;
            model.r = remainder;
            model.oldX = model.x;
            model.x = nextX;
            model.oldY = model.y;
            model.y = nextY;
            add(
                    steps,
                    model,
                    LINE_UPDATE_REMAINDERS,
                    "Shift both remainder/coefficient rows together: oldR = " + model.oldR + ", r = " + model.r,
                    StepEventType.EXECUTE_LINE);
            add(
                    steps,
                    model,
                    LINE_UPDATE_X,
                    "Check " + model.a + " × " + model.oldX + " + " + model.b + " × " + model.oldY + " = " + model.oldR,
                    StepEventType.EXECUTE_LINE);
            add(
                    steps,
                    model,
                    LINE_UPDATE_Y,
                    "Check " + model.a + " × " + model.x + " + " + model.b + " × " + model.y + " = " + model.r,
                    StepEventType.EXECUTE_LINE);
            model.history.add(model.currentRow);
            model.currentRow = null;
        }

        model.resultGcd = model.oldR;
        model.resultX = model.oldX;
        model.resultY = model.oldY;
        model.phase = "normalize";
        if (model.resultGcd < 0L) {
            model.resultGcd = -model.resultGcd;
            model.resultX = -model.resultX;
            model.resultY = -model.resultY;
            add(
                    steps,
                    model,
                    LINE_NORMALIZE,
                    "Normalize the negative terminal remainder to gcd = " + model.resultGcd
                            + " and negate both coefficients",
                    StepEventType.EXECUTE_LINE);
        } else {
            add(
                    steps,
                    model,
                    LINE_NORMALIZE,
                    "Terminal remainder is already nonnegative; keep gcd = " + model.resultGcd,
                    StepEventType.EXECUTE_LINE);
        }
        model.resultReady = true;
        model.history.add(EuclidRow.result(
                model.nextStep,
                model.resultGcd,
                model.resultX,
                model.resultY));
        model.phase = "return";
        add(
                steps,
                model,
                LINE_RETURN,
                "Return gcd = " + model.resultGcd + ", x = " + model.resultX + ", y = " + model.resultY,
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: " + model.a + " * " + model.resultX + " + " + model.b + " * "
                        + model.resultY + " = " + model.resultGcd,
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
        steps.add(MathSimulationSupport.tableStep(
                TABLE_COLUMNS,
                rows(model),
                facts(model, line),
                line,
                narration,
                eventType));
    }

    private static List<List<TypedCell>> rows(Model model) {
        List<EuclidRow> visible = new ArrayList<>(model.history.size() + 2);
        visible.add(model.initialRow);
        visible.addAll(model.history);
        if (model.currentRow != null) {
            visible.add(model.currentRow);
        }
        List<List<TypedCell>> rows = new ArrayList<>(visible.size());
        for (EuclidRow row : visible) {
            boolean initialActive = row == model.initialRow
                    && model.history.isEmpty()
                    && model.currentRow == null
                    && !model.resultReady;
            SnapshotStatus status = model.currentRow != null && row == model.currentRow
                    ? SnapshotStatus.ACTIVE
                    : initialActive ? SnapshotStatus.ACTIVE : SnapshotStatus.DONE;
            rows.add(MathSimulationSupport.row(
                    MathSimulationSupport.cell("step-" + row.step, Integer.toString(row.step), status),
                    MathSimulationSupport.cell("r0-" + row.step, Long.toString(row.r0), status),
                    MathSimulationSupport.cell("r1-" + row.step, Long.toString(row.r1), status),
                    MathSimulationSupport.cell("quotient-" + row.step, row.quotient, status),
                    MathSimulationSupport.cell("remainder-" + row.step, row.remainder, status),
                    MathSimulationSupport.cell("x0-" + row.step, Long.toString(row.x0), status),
                    MathSimulationSupport.cell("x1-" + row.step, Long.toString(row.x1), status),
                    MathSimulationSupport.cell("next-x-" + row.step, row.nextX, status),
                    MathSimulationSupport.cell("y0-" + row.step, Long.toString(row.y0), status),
                    MathSimulationSupport.cell("y1-" + row.step, Long.toString(row.y1), status),
                    MathSimulationSupport.cell("next-y-" + row.step, row.nextY, status)));
        }
        return List.copyOf(rows);
    }

    private static List<Fact> facts(Model model, int line) {
        SnapshotStatus resultStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        long visibleGcd = model.resultReady ? model.resultGcd : model.oldR;
        long visibleX = model.resultReady ? model.resultX : model.oldX;
        long visibleY = model.resultReady ? model.resultY : model.oldY;
        long oldCombination = linearCombination(model.a, model.oldX, model.b, model.oldY);
        long currentCombination = linearCombination(model.a, model.x, model.b, model.y);
        String caseName = model.bothZero
                ? "bothzero"
                : (model.a == 0L || model.b == 0L ? "one zero input" : "ordinary");
        String remainderEquation = model.currentRow == null || model.currentRow.quotient.equals("?") ? ""
                : model.currentRow.r0 + " = " + model.currentRow.quotient + " × " + model.currentRow.r1
                        + " + " + model.currentRow.remainder;
        String xEquation = model.currentRow == null || model.currentRow.nextX.equals("?") ? ""
                : "nextX = " + model.currentRow.x0 + " − " + model.currentRow.quotient
                        + " × " + model.currentRow.x1 + " = " + model.currentRow.nextX;
        String yEquation = model.currentRow == null || model.currentRow.nextY.equals("?") ? ""
                : "nextY = " + model.currentRow.y0 + " − " + model.currentRow.quotient
                        + " × " + model.currentRow.y1 + " = " + model.currentRow.nextY;
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("remainder-equation", remainderEquation, SnapshotStatus.ACTIVE),
                new Fact("coefficient-x-equation", xEquation, SnapshotStatus.ACTIVE),
                new Fact("coefficient-y-equation", yEquation, SnapshotStatus.ACTIVE),
                new Fact("teaching-equation", line >= LINE_COEFFICIENTS && line <= LINE_UPDATE_Y
                        ? xEquation + "; " + yEquation : remainderEquation, SnapshotStatus.ACTIVE),
                new Fact("teaching-detail", "Both remainder rows remain combinations of the original a and b.", SnapshotStatus.DEFAULT),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("a", Long.toString(model.a), SnapshotStatus.DEFAULT),
                new Fact("b", Long.toString(model.b), SnapshotStatus.DEFAULT),
                new Fact("old-r", Long.toString(model.oldR), SnapshotStatus.ACTIVE),
                new Fact("r", Long.toString(model.r), SnapshotStatus.ACTIVE),
                new Fact("quotient", model.lastQuotient == null ? "none" : Long.toString(model.lastQuotient),
                        SnapshotStatus.ACTIVE),
                new Fact("remainder", model.lastRemainder == null ? "none" : Long.toString(model.lastRemainder),
                        SnapshotStatus.ACTIVE),
                new Fact("old-x", Long.toString(model.oldX), SnapshotStatus.ACTIVE),
                new Fact("x-current", Long.toString(model.x), SnapshotStatus.ACTIVE),
                new Fact("next-x", model.currentRow == null ? "none" : model.currentRow.nextX,
                        SnapshotStatus.ACTIVE),
                new Fact("old-y", Long.toString(model.oldY), SnapshotStatus.ACTIVE),
                new Fact("y-current", Long.toString(model.y), SnapshotStatus.ACTIVE),
                new Fact("next-y", model.currentRow == null ? "none" : model.currentRow.nextY,
                        SnapshotStatus.ACTIVE),
                new Fact("gcd", Long.toString(visibleGcd), resultStatus),
                new Fact("x", Long.toString(visibleX), resultStatus),
                new Fact("y", Long.toString(visibleY), resultStatus),
                new Fact("old-invariant", model.a + " * " + model.oldX + " + " + model.b + " * "
                        + model.oldY + " = " + oldCombination + " (oldR)", SnapshotStatus.DEFAULT),
                new Fact("current-invariant", model.a + " * " + model.x + " + " + model.b + " * "
                        + model.y + " = " + currentCombination + " (r)", SnapshotStatus.DEFAULT),
                new Fact("bezout", model.a + " * " + visibleX + " + " + model.b + " * " + visibleY
                        + " = " + linearCombination(model.a, visibleX, model.b, visibleY), resultStatus),
                new Fact("case", caseName, SnapshotStatus.DEFAULT));
    }

    private static long linearCombination(long a, long x, long b, long y) {
        return a * x + b * y;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("a", 252);
        defaultInput.put("b", 105);
        return new SimulationMetadata(
                TYPE,
                "GCD / Extended Euclidean",
                "O(log min(|a|, |b|))",
                "O(1)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"a\":252,\"b\":105}; a and b must be signed integers in the inclusive range "
                        + MIN_INPUT + ".." + MAX_INPUT
                        + ". The result includes a nonnegative gcd and coefficients x,y with a*x + b*y = gcd;"
                        + " the bothzero case returns gcd = x = y = 0.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final long a;
        private final long b;
        private final boolean bothZero;
        private final EuclidRow initialRow;
        private final List<EuclidRow> history = new ArrayList<>();
        private long oldR;
        private long r;
        private long oldX;
        private long x;
        private long oldY;
        private long y;
        private long resultGcd;
        private long resultX;
        private long resultY;
        private boolean resultReady;
        private String phase = "initialize";
        private Long lastQuotient;
        private Long lastRemainder;
        private int nextStep = 1;
        private EuclidRow currentRow;

        private Model(long a, long b) {
            this.a = a;
            this.b = b;
            this.bothZero = a == 0L && b == 0L;
            if (bothZero) {
                this.oldR = 0L;
                this.r = 0L;
                this.oldX = 0L;
                this.x = 0L;
                this.oldY = 0L;
                this.y = 0L;
            } else {
                this.oldR = a;
                this.r = b;
                this.oldX = 1L;
                this.x = 0L;
                this.oldY = 0L;
                this.y = 1L;
            }
            this.initialRow = EuclidRow.initial(0, oldR, r, oldX, x, oldY, y);
        }
    }

    private static final class EuclidRow {
        private final int step;
        private final long r0;
        private final long r1;
        private final String quotient;
        private final String remainder;
        private final long x0;
        private final long x1;
        private final String nextX;
        private final long y0;
        private final long y1;
        private final String nextY;

        private EuclidRow(
                int step,
                long r0,
                long r1,
                String quotient,
                String remainder,
                long x0,
                long x1,
                String nextX,
                long y0,
                long y1,
                String nextY) {
            this.step = step;
            this.r0 = r0;
            this.r1 = r1;
            this.quotient = quotient;
            this.remainder = remainder;
            this.x0 = x0;
            this.x1 = x1;
            this.nextX = nextX;
            this.y0 = y0;
            this.y1 = y1;
            this.nextY = nextY;
        }

        private static EuclidRow initial(
                int step,
                long r0,
                long r1,
                long x0,
                long x1,
                long y0,
                long y1) {
            return new EuclidRow(step, r0, r1, "-", "-", x0, x1, "-", y0, y1, "-");
        }

        private static EuclidRow pending(
                int step,
                long r0,
                long r1,
                long x0,
                long x1,
                long y0,
                long y1) {
            return new EuclidRow(step, r0, r1, "?", "?", x0, x1, "?", y0, y1, "?");
        }

        private static EuclidRow computed(
                int step,
                long r0,
                long r1,
                long quotient,
                long remainder,
                long x0,
                long x1,
                long nextX,
                long y0,
                long y1,
                long nextY) {
            return new EuclidRow(
                    step,
                    r0,
                    r1,
                    Long.toString(quotient),
                    Long.toString(remainder),
                    x0,
                    x1,
                    Long.toString(nextX),
                    y0,
                    y1,
                    Long.toString(nextY));
        }

        private static EuclidRow result(int step, long gcd, long x, long y) {
            return new EuclidRow(
                    step,
                    gcd,
                    0L,
                    "-",
                    "0",
                    x,
                    0L,
                    "-",
                    y,
                    0L,
                    "-");
        }
    }
}
