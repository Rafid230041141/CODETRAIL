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
import java.util.Arrays;
import java.util.List;

/** Sieve of Eratosthenes trace with explicit p-squared marking passes. */
public final class SieveEngine implements SimulationEngine {
    public static final String TYPE = "SIEVE_OF_ERATOSTHENES";
    public static final int MIN_LIMIT = 0;
    public static final int MAX_LIMIT = 120;
    public static final int DEFAULT_LIMIT = 30;
    public static final int MAX_TRACE_STEPS = MathSimulationSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_NOT_PRIME = 3;
    private static final int LINE_FIRST_CANDIDATE = 4;
    private static final int LINE_WHILE = 5;
    private static final int LINE_IF_PRIME = 6;
    private static final int LINE_START_MULTIPLE = 7;
    private static final int LINE_MARK = 9;
    private static final int LINE_ADVANCE_MULTIPLE = 10;
    private static final int LINE_ADVANCE_PRIME = 11;
    private static final int LINE_RETURN = 12;

    private static final List<String> PSEUDOCODE = List.of(
            "sieve(limit):",
            "    prime[0..limit] = true",
            "    prime[0] = false; prime[1] = false  // 0 and 1 are not prime",
            "    p = 2",
            "    while p * p <= limit:",
            "        if prime[p]:",
            "            multiple = p * p",
            "            while multiple <= limit:",
            "                prime[multiple] = false",
            "                multiple += p",
            "        p += 1",
            "    return values marked prime");

    private static final List<String> TABLE_COLUMNS = List.of(
            "number", "classification", "witness");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        int limit = (int) MathSimulationSupport.readBoundedLong(
                input, TYPE, "limit", MIN_LIMIT, MAX_LIMIT);
        Model model = new Model(limit);
        List<SimulationStep> steps = new ArrayList<>();

        add(
                steps,
                model,
                0,
                "Initialize the sieve table for every integer from 0 through " + limit,
                StepEventType.INITIALIZE);
        add(
                steps,
                model,
                LINE_METHOD,
                "Run Sieve of Eratosthenes through the inclusive limit " + limit,
                StepEventType.EXECUTE_LINE);
        model.phase = "initialize";
        Arrays.fill(model.prime, true);
        add(
                steps,
                model,
                LINE_INITIALIZE,
                "Mark every table entry as a possible prime before crossing out composites",
                StepEventType.EXECUTE_LINE);

        model.prime[0] = false;
        if (limit >= 1) {
            model.prime[1] = false;
        }
        model.phase = "boundary";
        add(
                steps,
                model,
                LINE_NOT_PRIME,
                "Mark 0 and 1 explicitly notprime; neither value is prime",
                StepEventType.EXECUTE_LINE);

        model.p = 2;
        model.phase = "scan";
        add(
                steps,
                model,
                LINE_FIRST_CANDIDATE,
                "Start the candidate scan at p = 2",
                StepEventType.EXECUTE_LINE);

        while ((long) model.p * model.p <= limit) {
            int candidate = model.p;
            model.currentPrime = candidate;
            model.currentMultiple = -1L;
            add(
                    steps,
                    model,
                    LINE_WHILE,
                    "Check p = " + candidate + ": p * p = " + ((long) candidate * candidate)
                            + (model.prime[candidate] ? " is within the limit" : " is already rejected"),
                    StepEventType.EXECUTE_LINE);
            add(
                    steps,
                    model,
                    LINE_IF_PRIME,
                    model.prime[candidate]
                            ? "p = " + candidate + " is still prime, so begin its p-squared marking pass"
                            : "p = " + candidate + " is composite already; skip its marking pass",
                    StepEventType.EXECUTE_LINE);

            if (model.prime[candidate]) {
                model.currentMultiple = (long) candidate * candidate;
                add(
                        steps,
                        model,
                        LINE_START_MULTIPLE,
                        "Start this pass at multiple = p * p = " + model.currentMultiple,
                        StepEventType.EXECUTE_LINE);
                while (model.currentMultiple <= limit) {
                    int multiple = (int) model.currentMultiple;
                    boolean alreadyRejected = model.composite[multiple];
                    model.composite[multiple] = true;
                    model.prime[multiple] = false;
                    if (model.firstFactor[multiple] == 0) {
                        model.firstFactor[multiple] = candidate;
                    }
                    model.lastMarked = multiple;
                    add(
                            steps,
                            model,
                            LINE_MARK,
                            alreadyRejected
                                    ? "Keep " + multiple + " rejected; it is also a multiple of p = " + candidate
                                    : "Reject " + multiple + " = " + candidate + " × " + (multiple / candidate)
                                            + "; it has factor " + candidate,
                            StepEventType.EXECUTE_LINE);
                    model.currentMultiple += candidate;
                    add(
                            steps,
                            model,
                            LINE_ADVANCE_MULTIPLE,
                            model.currentMultiple <= limit
                                    ? "Advance to the next multiple " + model.currentMultiple
                                    : "Advance beyond the limit and finish the p = " + candidate + " pass",
                            StepEventType.EXECUTE_LINE);
                }
                model.confirmedPrime[candidate] = true;
            }

            model.currentPrime = -1;
            model.currentMultiple = -1L;
            model.p++;
            add(
                    steps,
                    model,
                    LINE_ADVANCE_PRIME,
                    "Advance p to " + model.p + " and continue while p * p <= limit",
                    StepEventType.EXECUTE_LINE);
        }

        model.currentPrime = -1;
        model.currentMultiple = -1L;
        for (int value = 2; value <= limit; value++) {
            if (!model.composite[value]) {
                model.prime[value] = true;
                model.confirmedPrime[value] = true;
            }
        }
        model.phase = "return";
        add(
                steps,
                model,
                LINE_RETURN,
                "Return the values still marked prime: " + formatPrimes(model),
                StepEventType.EXECUTE_LINE);
        model.phase = "complete";
        add(
                steps,
                model,
                0,
                "Complete: " + formatPrimes(model).size() + " prime value(s) through " + limit,
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
                facts(model),
                line,
                narration,
                eventType));
    }

    private static List<List<TypedCell>> rows(Model model) {
        List<List<TypedCell>> rows = new ArrayList<>(Math.max(1, model.limit + 1));
        for (int value = 0; value <= model.limit; value++) {
            SnapshotStatus status = status(model, value);
            String classification;
            String witness;
            if (value <= 1) {
                classification = "notprime";
                witness = value + " is not prime";
            } else if (model.currentPrime == value && model.prime[value]) {
                classification = "current prime";
                witness = "p-squared pass";
            } else if (model.currentMultiple == value && !model.composite[value]) {
                classification = "current multiple";
                witness = "next multiple of p = " + model.currentPrime;
            } else if (model.composite[value]) {
                classification = "rejected composite";
                witness = "first factor p = " + model.firstFactor[value];
            } else if (model.confirmedPrime[value]) {
                classification = "prime";
                witness = "no smaller factor remains";
            } else if (model.currentPrime == value) {
                classification = "rejected candidate";
                witness = "already crossed out";
            } else {
                classification = "candidate";
                witness = "not examined";
            }
            rows.add(MathSimulationSupport.row(
                    MathSimulationSupport.cell("number-" + value, Integer.toString(value), status),
                    MathSimulationSupport.cell("classification-" + value, classification, status),
                    MathSimulationSupport.cell("witness-" + value, witness, status)));
        }
        return List.copyOf(rows);
    }

    private static SnapshotStatus status(Model model, int value) {
        if (value <= 1) {
            return SnapshotStatus.REJECTED;
        }
        if (model.currentMultiple == value) {
            return SnapshotStatus.ACTIVE;
        }
        if (model.currentPrime == value && model.prime[value]) {
            return model.currentMultiple < 0 ? SnapshotStatus.ACTIVE : SnapshotStatus.DONE;
        }
        if (model.composite[value]) {
            return SnapshotStatus.REJECTED;
        }
        if (model.confirmedPrime[value]) {
            return SnapshotStatus.DONE;
        }
        if (model.currentPrime == value) {
            return SnapshotStatus.REJECTED;
        }
        return SnapshotStatus.DEFAULT;
    }

    private static List<Fact> facts(Model model) {
        SnapshotStatus resultStatus = model.phase.equals("complete")
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        SnapshotStatus candidateStatus = model.currentPrime >= 0
                ? SnapshotStatus.ACTIVE
                : SnapshotStatus.DEFAULT;
        return List.of(
                new Fact("algorithm", TYPE, SnapshotStatus.DEFAULT),
                new Fact("focus-number", Long.toString(model.currentMultiple >= 0 && model.currentMultiple <= model.limit
                        ? model.currentMultiple : model.currentPrime), candidateStatus),
                new Fact("factor-equation", factorEquation(model), candidateStatus),
                new Fact("teaching-equation", factorEquation(model), candidateStatus),
                new Fact("teaching-detail", "Start at p²: smaller multiples already have a smaller prime factor.", SnapshotStatus.DEFAULT),
                new Fact("phase", model.phase, SnapshotStatus.DEFAULT),
                new Fact("limit", Integer.toString(model.limit), SnapshotStatus.DEFAULT),
                new Fact("p", Integer.toString(model.p >= 2 ? model.p : -1), candidateStatus),
                new Fact("current-prime", Integer.toString(model.currentPrime), candidateStatus),
                new Fact("multiple", model.currentMultiple < 0 ? "-1" : Long.toString(model.currentMultiple),
                        candidateStatus),
                new Fact("last-mark", model.lastMarked < 0 ? "none" : Integer.toString(model.lastMarked),
                        model.lastMarked < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.REJECTED),
                new Fact("primes", formatPrimes(model).toString(), resultStatus),
                new Fact("prime-count", Integer.toString(formatPrimes(model).size()), resultStatus),
                new Fact("notprime-boundary", "0 and 1 are explicitly notprime", SnapshotStatus.REJECTED),
                new Fact("invariant", "Every rejected value n > 1 has a factor witness p with n >= p * p",
                        resultStatus));
    }

    private static String factorEquation(Model model) {
        if (model.currentPrime < 2 || model.currentMultiple < 0 || model.currentMultiple > model.limit) {
            return "";
        }
        return model.currentMultiple + " = " + model.currentPrime + " × "
                + (model.currentMultiple / model.currentPrime);
    }

    private static List<Integer> formatPrimes(Model model) {
        List<Integer> primes = new ArrayList<>();
        for (int value = 2; value <= model.limit; value++) {
            if (model.confirmedPrime[value]) {
                primes.add(value);
            }
        }
        return List.copyOf(primes);
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        defaultInput.put("limit", DEFAULT_LIMIT);
        return new SimulationMetadata(
                TYPE,
                "Sieve of Eratosthenes",
                "O(n log log n)",
                "O(n)",
                RendererFamily.TABLE,
                defaultInput,
                "Enter JSON as {\"limit\":30}; limit must be an integer from "
                        + MIN_LIMIT + " through " + MAX_LIMIT
                        + ". The sieve explicitly treats 0 and 1 as notprime and returns every prime through the limit.",
                PSEUDOCODE);
    }

    private static final class Model {
        private final int limit;
        private final boolean[] prime;
        private final boolean[] composite;
        private final boolean[] confirmedPrime;
        private final int[] firstFactor;
        private String phase = "initialize";
        private int p = -1;
        private int currentPrime = -1;
        private long currentMultiple = -1L;
        private int lastMarked = -1;

        private Model(int limit) {
            this.limit = limit;
            this.prime = new boolean[limit + 1];
            this.composite = new boolean[limit + 1];
            this.confirmedPrime = new boolean[limit + 1];
            this.firstFactor = new int[limit + 1];
        }
    }
}
