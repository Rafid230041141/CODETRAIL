package dev.codetrail.desktop.simulation.graphs.weighted;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bellman-Ford trace with pass-by-pass relaxation and reachable-cycle detection. */
public final class BellmanFordEngine implements SimulationEngine {
    public static final String TYPE = "BELLMAN_FORD";
    public static final int MIN_N = WeightedGraphSupport.MIN_N;
    public static final int MAX_N = WeightedGraphSupport.MAX_N;
    public static final int MAX_EDGES = WeightedGraphSupport.MAX_EDGES;
    public static final long MAX_ABS_WEIGHT = WeightedGraphSupport.MAX_ABS_WEIGHT;
    public static final int MAX_TRACE_STEPS = WeightedGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_FOR_VERTEX = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_SOURCE = 4;
    private static final int LINE_PASS = 5;
    private static final int LINE_CHANGED = 6;
    private static final int LINE_SCAN_EDGE = 7;
    private static final int LINE_CHECK_RELAX = 8;
    private static final int LINE_RELAX = 9;
    private static final int LINE_RECORD_CHANGE = 10;
    private static final int LINE_BREAK = 11;
    private static final int LINE_CYCLE_SCAN = 12;
    private static final int LINE_CYCLE_FOUND = 13;
    private static final int LINE_RETURN = 14;

    private static final List<String> PSEUDOCODE = List.of(
            "bellmanFord(G, source):",
            "    for each vertex v:",
            "        distance[v] = ∞; parent[v] = NIL",
            "    distance[source] = 0",
            "    for pass = 1 to |V| - 1:",
            "        changed = false",
            "        for each edge (u, v, weight):",
            "            if distance[u] != ∞ and distance[u] + weight < distance[v]:",
            "                distance[v] = distance[u] + weight",
            "                parent[v] = u; changed = true",
            "        if not changed: break",
            "    for each edge (u, v, weight):",
            "        if a reachable edge can still relax: report a negative cycle",
            "    return distance, parent");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        WeightedGraphSupport.Input graph = WeightedGraphSupport.parse(
                input, TYPE, true, true, false);
        List<List<WeightedGraphSupport.Arc>> adjacency = WeightedGraphSupport.adjacency(graph);
        List<WeightedGraphSupport.Arc> arcs = flatten(adjacency);
        long[] distance = new long[graph.n()];
        Arrays.fill(distance, WeightedGraphSupport.INF);
        int[] parent = new int[graph.n()];
        Arrays.fill(parent, -1);
        boolean[] acceptedEdges = new boolean[graph.edges().size()];
        int[] parentEdge = new int[graph.n()];
        Arrays.fill(parentEdge, -1);
        WeightedGraphSupport.GraphTraceBuilder trace = new WeightedGraphSupport.GraphTraceBuilder(graph);
        int currentVertex = -1;
        int currentEdge = -1;
        int currentPass = 0;
        boolean changed = false;
        boolean negativeCycle = false;
        boolean[] cycleVertices = new boolean[graph.n()];
        boolean[] cycleEdges = new boolean[graph.edges().size()];
        String phase = "initialize";

        trace.add(
                0,
                "Initialize Bellman-Ford on " + graph.n() + " vertices from source " + graph.source(),
                StepEventType.INITIALIZE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, false));
        trace.add(
                LINE_METHOD,
                "Run Bellman-Ford so negative edges remain valid",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, false));
        trace.add(
                LINE_FOR_VERTEX,
                "Prepare a distance and parent entry for every vertex",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, false));
        trace.add(
                LINE_INITIALIZE,
                "Set every distance to ∞ and every parent to NIL",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, false));
        distance[graph.source()] = 0L;
        phase = "relax";
        trace.nodeStatus(graph.source(), SnapshotStatus.ACTIVE);
        trace.add(
                LINE_SOURCE,
                "Set distance[" + graph.source() + "] = 0 for the source",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, false));

        for (int pass = 1; pass <= graph.n() - 1; pass++) {
            currentPass = pass;
            currentVertex = -1;
            currentEdge = -1;
            trace.add(
                    LINE_PASS,
                    "Begin relaxation pass " + pass + " of " + Math.max(0, graph.n() - 1),
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of(),
                    facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                            currentVertex, currentEdge, phase, false));
            changed = false;
            trace.add(
                    LINE_CHANGED,
                    "Set changed = false before scanning pass " + pass,
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of(),
                    facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                            currentVertex, currentEdge, phase, false));

            for (WeightedGraphSupport.Arc arc : arcs) {
                currentVertex = arc.from();
                currentEdge = arc.edgeIndex();
                trace.edgeStatus(currentEdge, SnapshotStatus.ACTIVE);
                trace.add(
                        LINE_SCAN_EDGE,
                        "Pass " + pass + ": inspect edge " + arc.from() + " -> " + arc.to()
                                + " with weight " + arc.weight(),
                        StepEventType.EXECUTE_LINE,
                        List.of(arc.from(), arc.to()),
                        List.of(currentEdge),
                        facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                                currentVertex, currentEdge, phase, false));
                long candidate = safeAdd(distance[arc.from()], arc.weight());
                trace.add(
                        LINE_CHECK_RELAX,
                        WeightedGraphSupport.formatDistance(distance[arc.from()]) + " + " + arc.weight()
                                + " = " + WeightedGraphSupport.formatDistance(candidate) + " < "
                                + WeightedGraphSupport.formatDistance(distance[arc.to()]) + "? "
                                + (candidate < distance[arc.to()] ? "Yes: relax " : "No: keep distance of ") + arc.to(),
                        StepEventType.EXECUTE_LINE,
                        List.of(arc.from(), arc.to()),
                        List.of(currentEdge),
                        facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                                currentVertex, currentEdge, phase, false));
                if (candidate < distance[arc.to()]) {
                    distance[arc.to()] = candidate;
                    trace.nodeStatus(arc.to(), SnapshotStatus.ACTIVE);
                    trace.add(
                            LINE_RELAX,
                            "Relax distance[" + arc.to() + "] to " + candidate,
                            StepEventType.EXECUTE_LINE,
                            List.of(arc.from(), arc.to()),
                            List.of(currentEdge),
                            facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                                    currentVertex, currentEdge, phase, false));
                    parent[arc.to()] = arc.from();
                    changed = true;
                    if (parentEdge[arc.to()] >= 0) {
                        int oldEdge = parentEdge[arc.to()];
                        acceptedEdges[oldEdge] = false;
                        trace.edgeStatus(oldEdge, SnapshotStatus.REJECTED);
                    }
                    parentEdge[arc.to()] = currentEdge;
                    acceptedEdges[currentEdge] = true;
                    trace.edgeStatus(currentEdge, SnapshotStatus.DONE);
                    trace.add(
                            LINE_RECORD_CHANGE,
                            "Set parent[" + arc.to() + "] = " + arc.from() + " and changed = true",
                            StepEventType.EXECUTE_LINE,
                            List.of(arc.from(), arc.to()),
                            List.of(currentEdge),
                            facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                                    currentVertex, currentEdge, phase, false));
                } else if (!acceptedEdges[currentEdge]) {
                    trace.edgeStatus(currentEdge, SnapshotStatus.REJECTED);
                }
            }
            currentVertex = -1;
            currentEdge = -1;
            trace.add(
                    LINE_BREAK,
                    changed
                            ? "Pass " + pass + " changed at least one distance; continue"
                            : "Pass " + pass + " changed nothing; stop early",
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of(),
                    facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                            currentVertex, currentEdge, phase, false));
            if (!changed) {
                break;
            }
        }

        phase = "cycle-check";
        currentPass = graph.n();
        currentVertex = -1;
        currentEdge = -1;
        trace.add(
                LINE_CYCLE_SCAN,
                "Scan every edge once more for a reachable relaxation",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, false));
        for (WeightedGraphSupport.Arc arc : arcs) {
            currentVertex = arc.from();
            currentEdge = arc.edgeIndex();
            trace.edgeStatus(currentEdge, SnapshotStatus.ACTIVE);
            long candidate = safeAdd(distance[arc.from()], arc.weight());
            trace.add(
                    LINE_CYCLE_SCAN,
                    "Cycle check " + arc.from() + "→" + arc.to() + ": "
                            + WeightedGraphSupport.formatDistance(candidate) + " < "
                            + WeightedGraphSupport.formatDistance(distance[arc.to()]) + "? "
                            + (candidate < distance[arc.to()] ? "Yes: reachable negative cycle" : "No improvement"),
                    StepEventType.EXECUTE_LINE,
                    List.of(arc.from(), arc.to()),
                    List.of(currentEdge),
                    facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                            currentVertex, currentEdge, phase, false));
            if (candidate < distance[arc.to()]) {
                negativeCycle = true;
                cycleVertices[arc.from()] = true;
                cycleVertices[arc.to()] = true;
                cycleEdges[currentEdge] = true;
                trace.nodeStatus(arc.from(), SnapshotStatus.ACTIVE);
                trace.nodeStatus(arc.to(), SnapshotStatus.ACTIVE);
                trace.edgeStatus(currentEdge, SnapshotStatus.REJECTED);
                trace.add(
                        LINE_CYCLE_FOUND,
                        "Reachable edge " + arc.from() + " -> " + arc.to()
                                + " can still relax; report a reachable negative cycle",
                        StepEventType.EXECUTE_LINE,
                        List.of(arc.from(), arc.to()),
                        List.of(currentEdge),
                        facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                                currentVertex, currentEdge, phase, false));
            } else {
                trace.edgeStatus(currentEdge, SnapshotStatus.REJECTED);
            }
        }
        propagateCycleReachability(adjacency, cycleVertices);
        Arrays.fill(acceptedEdges, false);
        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (!cycleVertices[vertex] && parentEdge[vertex] >= 0) acceptedEdges[parentEdge[vertex]] = true;
        }
        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.nodeStatus(vertex, cycleVertices[vertex]
                    ? SnapshotStatus.REJECTED
                    : distance[vertex] >= WeightedGraphSupport.INF
                            ? SnapshotStatus.REJECTED : SnapshotStatus.DONE);
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            trace.edgeStatus(edgeIndex, cycleEdges[edgeIndex]
                    ? SnapshotStatus.REJECTED
                    : acceptedEdges[edgeIndex] ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        phase = "return";
        currentVertex = -1;
        currentEdge = -1;
        trace.add(
                LINE_RETURN,
                negativeCycle
                        ? "Return distances and report the reachable negative cycle"
                        : "Return final distances and parent pointers",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, true));
        phase = "complete";
        trace.add(
                0,
                negativeCycle
                        ? "Complete: a reachable negative cycle was detected"
                        : "Complete: Bellman-Ford found shortest paths with " + countFinite(distance)
                                + " reachable vertex(es)",
                StepEventType.COMPLETE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, currentPass, changed, negativeCycle, cycleVertices,
                        currentVertex, currentEdge, phase, true));
        return trace.steps();
    }

    private static List<WeightedGraphSupport.Arc> flatten(
            List<List<WeightedGraphSupport.Arc>> adjacency) {
        List<WeightedGraphSupport.Arc> arcs = new ArrayList<>();
        for (List<WeightedGraphSupport.Arc> outgoing : adjacency) {
            arcs.addAll(outgoing);
        }
        return List.copyOf(arcs);
    }

    private static List<Fact> facts(
            WeightedGraphSupport.Input graph,
            long[] distance,
            int[] parent,
            int pass,
            boolean changed,
            boolean negativeCycle,
            boolean[] cycleVertices,
            int currentVertex,
            int currentEdge,
            String phase,
            boolean complete) {
        SnapshotStatus resultStatus = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        return List.of(
                WeightedGraphSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("source", Integer.toString(graph.source()), SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("pass", Integer.toString(pass), pass == 0
                        ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("changed", Boolean.toString(changed), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("current", currentVertex < 0
                        ? (currentEdge < 0 ? "NIL" : "edge-" + currentEdge)
                        : Integer.toString(currentVertex), currentVertex < 0 && currentEdge < 0
                                ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("distance", WeightedGraphSupport.formatDistance(distance), resultStatus),
                WeightedGraphSupport.fact("parent", WeightedGraphSupport.formatParents(parent, graph.source()), resultStatus),
                WeightedGraphSupport.fact("negative-cycle", Boolean.toString(negativeCycle),
                        negativeCycle ? SnapshotStatus.REJECTED : resultStatus),
                WeightedGraphSupport.fact("cycle-vertices", formatCycleVertices(cycleVertices),
                        negativeCycle ? SnapshotStatus.REJECTED : SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact(
                        "invariant",
                        "Only reachable edges relax; a post-pass relaxation proves a reachable negative cycle",
                        resultStatus),
                WeightedGraphSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static String formatCycleVertices(boolean[] cycleVertices) {
        List<Integer> vertices = new ArrayList<>();
        for (int vertex = 0; vertex < cycleVertices.length; vertex++) {
            if (cycleVertices[vertex]) {
                vertices.add(vertex);
            }
        }
        return WeightedGraphSupport.formatVertices(vertices);
    }

    private static void propagateCycleReachability(
            List<List<WeightedGraphSupport.Arc>> adjacency,
            boolean[] cycleVertices) {
        for (int pass = 0; pass < cycleVertices.length; pass++) {
            boolean changed = false;
            for (int from = 0; from < adjacency.size(); from++) {
                if (!cycleVertices[from]) {
                    continue;
                }
                for (WeightedGraphSupport.Arc arc : adjacency.get(from)) {
                    if (!cycleVertices[arc.to()]) {
                        cycleVertices[arc.to()] = true;
                        changed = true;
                    }
                }
            }
            if (!changed) {
                return;
            }
        }
    }

    private static long safeAdd(long base, long weight) {
        if (base >= WeightedGraphSupport.INF) {
            return WeightedGraphSupport.INF;
        }
        if (weight > 0L && base > WeightedGraphSupport.INF - weight) {
            return WeightedGraphSupport.INF;
        }
        if (weight < 0L && base < -WeightedGraphSupport.INF - weight) {
            return -WeightedGraphSupport.INF;
        }
        return base + weight;
    }

    private static int countFinite(long[] distance) {
        int count = 0;
        for (long value : distance) {
            if (value < WeightedGraphSupport.INF) {
                count++;
            }
        }
        return count;
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = WeightedGraphSupport.inputWithEdges(
                5,
                List.of(
                        new WeightedGraphSupport.WeightedEdge(0, 1, 4),
                        new WeightedGraphSupport.WeightedEdge(0, 2, 5),
                        new WeightedGraphSupport.WeightedEdge(1, 2, -2),
                        new WeightedGraphSupport.WeightedEdge(2, 3, 3),
                        new WeightedGraphSupport.WeightedEdge(3, 4, 1)),
                0,
                true);
        return new SimulationMetadata(
                TYPE,
                "Bellman-Ford",
                "O(VE)",
                "O(V)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"n\":5,\"edges\":[{\"from\":0,\"to\":1,\"weight\":4}],\"source\":0,\"directed\":true}; n must be "
                        + MIN_N + ".." + MAX_N + ", there may be at most " + MAX_EDGES
                        + " unique logical edges, weights are integers from -" + MAX_ABS_WEIGHT + " through "
                        + MAX_ABS_WEIGHT + ", and a reachable post-pass relaxation reports a negative cycle.",
                PSEUDOCODE);
    }
}
