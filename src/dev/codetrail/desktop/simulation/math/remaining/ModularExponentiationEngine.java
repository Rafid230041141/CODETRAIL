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

/** Binary square-and-multiply trace for a bounded modular power. */
public final class ModularExponentiationEngine implements SimulationEngine {
    public static final String TYPE = "MODULAR_EXPONENTIATION";
    public static final long MIN_BASE = -1_000_000_000L;
    public static final long MAX_BASE = 1_000_000_000L;
    public static final long MIN_EXPONENT = 0L;
    public static final long MAX_EXPONENT = 1_000_000_000L;
    public static final long MIN_MODULUS = 1L;
    public static final long MAX_MODULUS = 1_000_000_007L;
    public static final int MAX_TRACE_STEPS = RemainingMathSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_NORMALIZE = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_WHILE = 4;
    private static final int LINE_BIT = 5;
    private static final int LINE_MULTIPLY = 6;
    private static final int LINE_SQUARE = 7;
    private static final int LINE_SHIFT = 8;
    private static final int LINE_RETURN = 9;

    private static final List<String> PSEUDOCODE = List.of(
            "modularPow(base, exponent, modulus):",
            "    base = ((base mod modulus) + modulus) mod modulus",
            "    result = 1 mod modulus",
            "    while exponent > 0:",
            "        bit = exponent & 1",
            "        if bit == 1: result = result * base mod modulus",
            "        base = base * base mod modulus",
            "        exponent >>= 1",
            "    return result");

    private static final List<String> TABLE_COLUMNS = List.of(
            "bit",
            "exponent-before",
            "base-before",
            "result-before",
            "multiply",
            "result-after-multiply",
            "base-after-square",
            "exponent-after",
            "state");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        long base = RemainingMathSupport.readLong(
                input, TYPE, "base", MIN_BASE, MAX_BASE);
        long exponent = RemainingMathSupport.readLong(
                input, TYPE, "exponent", MIN_EXPONENT, MAX_EXPONENT);
        long modulus = RemainingMathSupport.readLong(
                input, TYPE, "modulus", MIN_MODULUS, MAX_MODULUS);
        Model model = new Model(base, exponent, modulus);
        List<SimulationStep> steps = new ArrayList<>();

        add(steps, model, 0,
                "Initialize binary modular exponentiation for the requested power",
                StepEventType.INITIALIZE);
        model.phase = "method";
        add(steps, model, LINE_METHOD,
                "Run square-and-multiply while consuming one binary exponent bit at a time",
                StepEventType.EXECUTE_LINE);

        model.phase = "normalize";
        model.currentBase = normalize(base, modulus);
        add(steps, model, LINE_NORMALIZE,
                "Normalize the base into the residue range before multiplying",
                StepEventType.EXECUTE_LINE);
        model.result = 1L % modulus;
        add(steps, model, LINE_INITIALIZE,
                "Initialize result to the multiplicative identity 1 modulo the modulus",
                StepEventType.EXECUTE_LINE);

        if (exponent == 0L) {
            model.phase = "return";
            model.currentExponent = 0L;
            add(steps, model, LINE_RETURN,
                    "Return the identity residue for exponent zero",
                    StepEventType.EXECUTE_LINE);
            model.phase = "complete";
            add(steps, model, 0,
                    "Complete modular exponentiation with result " + model.result,
                    StepEventType.COMPLETE);
            return List.copyOf(steps);
        }

        model.phase = "iterate";
        while (model.currentExponent > 0L) {
            Iteration iteration = new Iteration(
                    model.iterations.size(),
                    model.currentExponent,
                    model.currentBase,
                    model.result);
            model.iterations.add(iteration);
            model.currentIteration = iteration.index;
            add(steps, model, LINE_WHILE,
                    "Process exponent " + iteration.exponentBefore + " from its low bit upward",
                    StepEventType.EXECUTE_LINE);

            iteration.operation = "read bit";
            iteration.bit = (int) (iteration.exponentBefore & 1L);
            add(steps, model, LINE_BIT,
                    "Read low bit " + iteration.bit + " from " + Long.toBinaryString(iteration.exponentBefore)
                            + " (" + iteration.exponentBefore + ")",
                    StepEventType.EXECUTE_LINE);

            if (iteration.bit == 1) {
                model.result = multiplyMod(model.result, model.currentBase, modulus);
                iteration.resultAfterMultiply = model.result;
                iteration.operation = "multiply";
                add(steps, model, LINE_MULTIPLY,
                        iteration.resultBefore + " × " + iteration.baseBefore + " mod " + modulus
                                + " = " + model.result + "; low bit 1 includes this power",
                        StepEventType.EXECUTE_LINE);
            } else {
                iteration.resultAfterMultiply = model.result;
                iteration.operation = "skip multiply";
                add(steps, model, LINE_MULTIPLY,
                        "Keep result unchanged because this exponent bit is zero",
                        StepEventType.EXECUTE_LINE);
            }

            model.currentBase = multiplyMod(model.currentBase, model.currentBase, modulus);
            iteration.baseAfterSquare = model.currentBase;
            iteration.operation = "square";
            add(steps, model, LINE_SQUARE,
                    iteration.baseBefore + "² mod " + modulus + " = " + model.currentBase
                            + "; prepare the next binary power",
                    StepEventType.EXECUTE_LINE);

            model.currentExponent >>>= 1;
            iteration.exponentAfter = model.currentExponent;
            iteration.operation = "shift";
            add(steps, model, LINE_SHIFT,
                    Long.toBinaryString(iteration.exponentBefore) + " >> 1 = "
                            + Long.toBinaryString(model.currentExponent) + "; discard the processed bit",
                    StepEventType.EXECUTE_LINE);
        }

        model.currentIteration = -1;
        model.phase = "return";
        add(steps, model, LINE_RETURN,
                "Return the accumulated residue after all binary bits are consumed",
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(steps, model, 0,
                "Complete modular exponentiation with result " + model.result,
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
                TABLE_COLUMNS, rows(model), facts(model, line), line, narration, eventType));
    }

    private static List<List<TypedCell>> rows(Model model) {
        List<List<TypedCell>> rows = new ArrayList<>();
        for (Iteration iteration : model.iterations) {
            SnapshotStatus status = iteration.index == model.currentIteration
                    ? SnapshotStatus.ACTIVE
                    : SnapshotStatus.DONE;
            String resultAfter = iteration.resultAfterMultiply == null
                    ? "?"
                    : Long.toString(iteration.resultAfterMultiply);
            String square = iteration.baseAfterSquare == null
                    ? "?"
                    : Long.toString(iteration.baseAfterSquare);
            String exponentAfter = iteration.exponentAfter == null
                    ? "?"
                    : Long.toString(iteration.exponentAfter);
            rows.add(RemainingMathSupport.row(
                    RemainingMathSupport.cell("bit-" + iteration.index,
                            iteration.bit < 0 ? "?" : Integer.toString(iteration.bit), status),
                    RemainingMathSupport.cell("exponent-before-" + iteration.index,
                            Long.toString(iteration.exponentBefore), status),
                    RemainingMathSupport.cell("base-before-" + iteration.index,
                            Long.toString(iteration.baseBefore), status),
                    RemainingMathSupport.cell("result-before-" + iteration.index,
                            Long.toString(iteration.resultBefore), status),
                    RemainingMathSupport.cell("multiply-" + iteration.index,
                            iteration.bit < 0 ? "?" : Boolean.toString(iteration.bit == 1), status),
                    RemainingMathSupport.cell("result-after-multiply-" + iteration.index,
                            resultAfter, status),
                    RemainingMathSupport.cell("base-after-square-" + iteration.index,
                            square, status),
                    RemainingMathSupport.cell("exponent-after-" + iteration.index,
                            exponentAfter, status),
                    RemainingMathSupport.cell("state-" + iteration.index,
                            iteration.operation, status)));
        }
        if (rows.isEmpty()) {
            rows.add(RemainingMathSupport.row(
                    RemainingMathSupport.cell("bit-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("exponent-before-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("base-before-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("result-before-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("multiply-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("result-after-multiply-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("base-after-square-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("exponent-after-empty", "-", SnapshotStatus.DEFAULT),
                    RemainingMathSupport.cell("state-empty", "no bits", SnapshotStatus.DEFAULT)));
        }
        return List.copyOf(rows);
    }

    private static List<Fact> facts(Model model, int line) {
        SnapshotStatus resultStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        String binary = Long.toBinaryString(model.exponent);
        String operation = model.iterations.isEmpty()
                ? "none"
                : model.iterations.get(model.iterations.size() - 1).operation;
        Iteration current = model.currentIteration < 0 ? null : model.iterations.get(model.currentIteration);
        String multiplyEquation = current == null || current.resultAfterMultiply == null ? ""
                : current.bit == 0 ? "bit 0: keep result = " + current.resultBefore
                : current.resultBefore + " × " + current.baseBefore + " mod " + model.modulus
                        + " = " + current.resultAfterMultiply;
        String squareEquation = current == null || current.baseAfterSquare == null ? ""
                : current.baseBefore + "² mod " + model.modulus + " = " + current.baseAfterSquare;
        String shiftEquation = current == null || current.exponentAfter == null ? ""
                : Long.toBinaryString(current.exponentBefore) + " >> 1 = " + Long.toBinaryString(current.exponentAfter);
        String teaching = line == LINE_MULTIPLY ? multiplyEquation : line == LINE_SQUARE ? squareEquation
                : line == LINE_SHIFT ? shiftEquation : current == null ? ""
                : current.exponentBefore + " = " + Long.toBinaryString(current.exponentBefore) + "₂" + (current.bit < 0 ? "; inspect its rightmost bit" : "; low bit = " + current.bit);
        return List.of(
                RemainingMathSupport.fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("remaining-binary", Long.toBinaryString(model.currentExponent), resultStatus),
                RemainingMathSupport.fact("processed-bit-index", current == null ? "none" : Integer.toString(current.index), resultStatus),
                RemainingMathSupport.fact("low-bit", current == null || current.bit < 0 ? "none" : Integer.toString(current.bit), resultStatus),
                RemainingMathSupport.fact("multiply-equation", multiplyEquation, resultStatus),
                RemainingMathSupport.fact("square-equation", squareEquation, resultStatus),
                RemainingMathSupport.fact("shift-equation", shiftEquation, resultStatus),
                RemainingMathSupport.fact("teaching-equation", teaching, resultStatus),
                RemainingMathSupport.fact("teaching-detail", "Read right to left: bit 1 multiplies the result; every bit squares the base.", SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("phase", model.phase, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("base", Long.toString(model.base), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("exponent", Long.toString(model.exponent), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("modulus", Long.toString(model.modulus), SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("binary-exponent", binary, SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("remaining-exponent", Long.toString(model.currentExponent), resultStatus),
                RemainingMathSupport.fact("current-base", Long.toString(model.currentBase), resultStatus),
                RemainingMathSupport.fact("result", Long.toString(model.result), resultStatus),
                RemainingMathSupport.fact("iteration", Integer.toString(model.currentIteration),
                        model.currentIteration >= 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DEFAULT),
                RemainingMathSupport.fact("operation", operation, resultStatus),
                RemainingMathSupport.fact(
                        "invariant",
                        "result * current-base^remaining-exponent is constant modulo modulus",
                        resultStatus));
    }

    private static long normalize(long value, long modulus) {
        return Math.floorMod(value, modulus);
    }

    private static long multiplyMod(long left, long right, long modulus) {
        return BigInteger.valueOf(left)
                .multiply(BigInteger.valueOf(right))
                .mod(BigInteger.valueOf(modulus))
                .longValue();
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("base", 3);
        defaultInput.put("exponent", 13);
        defaultInput.put("modulus", 1_000_000_007);
        return new SimulationMetadata(
                TYPE,
                "Fast Modular Exponentiation",
                "O(log exponent)",
                "O(1)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"base\":3,\"exponent\":13,\"modulus\":1000000007}; base must be in "
                        + MIN_BASE + ".." + MAX_BASE + ", exponent in " + MIN_EXPONENT + ".." + MAX_EXPONENT
                        + ", and modulus in " + MIN_MODULUS + ".." + MAX_MODULUS + ".",
                PSEUDOCODE);
    }

    private static final class Model {
        private final long base;
        private final long exponent;
        private final long modulus;
        private final List<Iteration> iterations = new ArrayList<>();
        private long currentExponent;
        private long currentBase;
        private long result;
        private int currentIteration = -1;
        private String phase = "initialize";

        private Model(long base, long exponent, long modulus) {
            this.base = base;
            this.exponent = exponent;
            this.modulus = modulus;
            this.currentExponent = exponent;
            this.currentBase = normalize(base, modulus);
            this.result = 1L % modulus;
        }
    }

    private static final class Iteration {
        private final int index;
        private final long exponentBefore;
        private final long baseBefore;
        private final long resultBefore;
        private int bit = -1;
        private Long resultAfterMultiply;
        private Long baseAfterSquare;
        private Long exponentAfter;
        private String operation = "pending";

        private Iteration(int index, long exponentBefore, long baseBefore, long resultBefore) {
            this.index = index;
            this.exponentBefore = exponentBefore;
            this.baseBefore = baseBefore;
            this.resultBefore = resultBefore;
        }
    }
}
