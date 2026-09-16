package dev.codetrail.desktop.simulation.searching;

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
import java.util.List;

/**
 * Factory Machines minimum-time search. The ARRAY cells show machine times;
 * the bounded facts show the binary time interval and the capped monotonic
 * production predicate rather than materializing a time-sized array.
 */
public final class BinarySearchAnswerEngine implements SimulationEngine {
    public static final String TYPE = "BINARY_SEARCH_ANSWER";
    public static final int MAX_MACHINES = SearchingSupport.MAX_MACHINES;
    public static final long MAX_TARGET = SearchingSupport.MAX_TARGET;
    public static final int MAX_TRACE_STEPS = SearchingSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_BOUNDS = 2;
    private static final int LINE_WHILE = 3;
    private static final int LINE_MIDDLE = 4;
    private static final int LINE_PRODUCTION_INIT = 5;
    private static final int LINE_MACHINE_LOOP = 6;
    private static final int LINE_PRODUCTION = 7;
    private static final int LINE_FEASIBILITY = 8;
    private static final int LINE_IF = 9;
    private static final int LINE_HIGH = 10;
    private static final int LINE_ELSE = 11;
    private static final int LINE_LOW = 12;
    private static final int LINE_RETURN = 13;
    private static final long[] DEFAULT_MACHINES = {2L, 3L, 7L};
    private static final long DEFAULT_TARGET = 10L;
    private static final List<String> PSEUDOCODE = List.of(
            "minimumTime(machines, target):",
            "    low = 0; high = min(machines) * target",
            "    while low < high:",
            "        middle = low + floor((high - low) / 2)",
            "        produced = 0",
            "        for machineTime in machines:",
            "            produced = min(target, produced + min(target - produced, middle / machineTime))",
            "        feasible = produced >= target",
            "        if feasible:",
            "            high = middle",
            "        else:",
            "            low = middle + 1",
            "    return low");
    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        SearchingSupport.MachineInput parsed = SearchingSupport.readMachineInput(input, TYPE);
        long[] machineTimes = parsed.machineTimes();
        long target = parsed.target();
        long low = 0L;
        long high = parsed.upperBound();
        long middle = -1L;

        SearchingSupport.TraceBuilder trace = new SearchingSupport.TraceBuilder(
                machineTimes, TYPE, MAX_TRACE_STEPS);
        trace.add(
                SearchingSupport.statuses(machineTimes.length, SnapshotStatus.DEFAULT),
                -1,
                0,
                "Initialize Factory Machines search for " + target + " product(s)",
                StepEventType.INITIALIZE,
                facts(low, middle, high, target, -1L, null, "pending", SnapshotStatus.ACTIVE));
        trace.add(
                SearchingSupport.statuses(machineTimes.length, SnapshotStatus.DEFAULT),
                -1,
                LINE_METHOD,
                "Binary-search the minimum time accepted by the production predicate",
                StepEventType.EXECUTE_LINE,
                facts(low, middle, high, target, -1L, null, "pending", SnapshotStatus.ACTIVE));
        trace.add(
                SearchingSupport.statuses(machineTimes.length, SnapshotStatus.DEFAULT),
                -1,
                LINE_BOUNDS,
                "Set the safe interval to low = 0 and high = " + high,
                StepEventType.EXECUTE_LINE,
                facts(low, middle, high, target, -1L, null, "pending", SnapshotStatus.ACTIVE));

        while (low < high) {
            SnapshotStatus[] activeMachines = SearchingSupport.statuses(
                    machineTimes.length, SnapshotStatus.ACTIVE);
            trace.add(
                    activeMachines,
                    -1,
                    LINE_WHILE,
                    "Check the shrinking time interval [" + low + ", " + high + "]",
                    StepEventType.EXECUTE_LINE,
                    facts(low, middle, high, target, -1L, null, "pending", SnapshotStatus.ACTIVE));

            middle = low + (high - low) / 2L;
            trace.add(
                    activeMachines,
                    -1,
                    LINE_MIDDLE,
                    "Choose middle = " + middle + " with overflow-safe interval arithmetic",
                    StepEventType.EXECUTE_LINE,
                    facts(low, middle, high, target, -1L, null, "pending", SnapshotStatus.ACTIVE));
            trace.add(
                    activeMachines,
                    -1,
                    LINE_PRODUCTION_INIT,
                    "Reset capped production to 0 before testing middle",
                    StepEventType.EXECUTE_LINE,
                    facts(low, middle, high, target, 0L, null, "pending", SnapshotStatus.ACTIVE));
            trace.add(
                    activeMachines,
                    -1,
                    LINE_MACHINE_LOOP,
                    "Evaluate each machine at time " + middle + "; stop counting after the target",
                    StepEventType.EXECUTE_LINE,
                    facts(low, middle, high, target, 0L, null, "pending", SnapshotStatus.ACTIVE));

            long produced = 0L;
            SnapshotStatus[] evaluatedMachines = SearchingSupport.statuses(
                    machineTimes.length, SnapshotStatus.DEFAULT);
            for (int machine = 0; machine < machineTimes.length && produced < target; machine++) {
                long before = produced;
                long capacity = middle / machineTimes[machine];
                long contribution = Math.min(target - produced, capacity);
                produced += contribution;
                evaluatedMachines[machine] = SnapshotStatus.ACTIVE;
                String calculation = "floor(" + middle + " / " + machineTimes[machine] + ") = " + capacity
                        + "; total " + before + " + " + contribution + " = " + produced;
                if (contribution < capacity) {
                    calculation += " (stop counting at target " + target + ")";
                }
                List<Fact> machineFacts = new ArrayList<>(facts(
                        low, middle, high, target, produced, null, "pending", SnapshotStatus.ACTIVE));
                machineFacts.add(new Fact("machine-index", Integer.toString(machine), SnapshotStatus.ACTIVE));
                machineFacts.add(new Fact("machine-time", Long.toString(machineTimes[machine]), SnapshotStatus.ACTIVE));
                machineFacts.add(new Fact("capacity", Long.toString(capacity), SnapshotStatus.ACTIVE));
                machineFacts.add(new Fact("contribution", Long.toString(contribution), SnapshotStatus.ACTIVE));
                machineFacts.add(new Fact("produced-before", Long.toString(before), SnapshotStatus.ACTIVE));
                machineFacts.add(new Fact("calculation", calculation, SnapshotStatus.ACTIVE));
                trace.add(evaluatedMachines, machine, LINE_PRODUCTION,
                        "Machine " + machine + ": " + calculation,
                        StepEventType.EXECUTE_LINE, machineFacts);
                evaluatedMachines[machine] = SnapshotStatus.DONE;
            }
            boolean feasible = produced >= target;
            trace.add(
                    evaluatedMachines,
                    -1,
                    LINE_FEASIBILITY,
                    "Predicate feasible = " + feasible + " (production is monotonic in time)",
                    StepEventType.EXECUTE_LINE,
                    facts(low, middle, high, target, produced, feasible, "pending", SnapshotStatus.ACTIVE));

            if (feasible) {
                trace.add(
                        evaluatedMachines,
                        -1,
                        LINE_IF,
                        "Feasible: keep middle and search the lower half",
                        StepEventType.EXECUTE_LINE,
                        facts(low, middle, high, target, produced, feasible, "pending", SnapshotStatus.ACTIVE));
                high = middle;
                trace.add(
                        evaluatedMachines,
                        -1,
                        LINE_HIGH,
                        "Move high to " + high,
                        StepEventType.EXECUTE_LINE,
                        facts(low, middle, high, target, produced, feasible, "pending", SnapshotStatus.ACTIVE));
            } else {
                trace.add(
                        evaluatedMachines,
                        -1,
                        LINE_ELSE,
                        "Infeasible: every answer is strictly after middle",
                        StepEventType.EXECUTE_LINE,
                        facts(low, middle, high, target, produced, feasible, "pending", SnapshotStatus.ACTIVE));
                low = middle + 1L;
                trace.add(
                        evaluatedMachines,
                        -1,
                        LINE_LOW,
                        "Move low to " + low,
                        StepEventType.EXECUTE_LINE,
                        facts(low, middle, high, target, produced, feasible, "pending", SnapshotStatus.ACTIVE));
            }
        }

        SnapshotStatus[] completeStatuses = SearchingSupport.statuses(
                machineTimes.length, SnapshotStatus.DONE);
        trace.add(
                completeStatuses,
                -1,
                LINE_RETURN,
                "Return the minimum feasible time " + low,
                StepEventType.EXECUTE_LINE,
                facts(low, middle, high, target, target, true, Long.toString(low), SnapshotStatus.DONE));
        trace.add(
                completeStatuses,
                -1,
                0,
                "Complete: minimum time = " + low + " for " + target + " product(s)",
                StepEventType.COMPLETE,
                facts(low, middle, high, target, target, true, Long.toString(low), SnapshotStatus.DONE));
        return trace.steps();
    }

    private static List<Fact> facts(
            long low,
            long middle,
            long high,
            long target,
            long produced,
            Boolean feasible,
            String answer,
            SnapshotStatus resultStatus) {
        SnapshotStatus progressStatus = resultStatus == SnapshotStatus.DONE
                ? SnapshotStatus.DONE
                : SnapshotStatus.ACTIVE;
        long visibleMiddle = middle >= low && middle <= high ? middle : -1L;
        String production = produced < 0L
                ? "not evaluated; cap = " + target
                : produced + "/" + target + " (capped at target)";
        String predicate = feasible == null ? "unknown" : feasible.toString();
        return List.of(
                new Fact("low", Long.toString(low), progressStatus),
                new Fact("middle", Long.toString(visibleMiddle), progressStatus),
                new Fact("high", Long.toString(high), progressStatus),
                new Fact("production", production, progressStatus),
                new Fact("predicate", predicate, progressStatus),
                new Fact("answer", answer, resultStatus));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = JsonNodeFactory.instance.objectNode();
        ArrayNode machines = defaultInput.putArray("machines");
        for (long machineTime : DEFAULT_MACHINES) {
            machines.add(machineTime);
        }
        defaultInput.put("target", DEFAULT_TARGET);
        return new SimulationMetadata(
                TYPE,
                "Binary Search on Answer",
                "O(m log T)",
                "O(1)",
                RendererFamily.ARRAY,
                defaultInput,
                "Enter JSON as {\"machines\":[2,3,7],\"target\":10}; use 1 through " + MAX_MACHINES
                        + " positive 64-bit machine times and a target count from 1 through " + MAX_TARGET
                        + ". The safe upper bound min(machineTime) * target must fit a signed long. The predicate counts production at a candidate time capped at target; no time-sized array is materialized.",
                PSEUDOCODE);
    }
}
