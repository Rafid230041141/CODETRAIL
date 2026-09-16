package dev.codetrail.desktop.simulation.graphs.weighted;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringJoiner;

/** Dijkstra trace with nonnegative relaxation, a binary heap, and stale-entry skips. */
public final class DijkstraEngine implements SimulationEngine {
    public static final String TYPE = "DIJKSTRA";
    public static final int MIN_N = WeightedGraphSupport.MIN_N;
    public static final int MAX_N = WeightedGraphSupport.MAX_N;
    public static final int MAX_EDGES = WeightedGraphSupport.MAX_EDGES;
    public static final long MAX_WEIGHT = WeightedGraphSupport.MAX_WEIGHT;
    public static final int MAX_TRACE_STEPS = WeightedGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_FOR_VERTEX = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_SOURCE = 4;
    private static final int LINE_PUSH_SOURCE = 5;
    private static final int LINE_WHILE = 6;
    private static final int LINE_POP = 7;
    private static final int LINE_STALE = 8;
    private static final int LINE_CHECK_SETTLED = 9;
    private static final int LINE_SETTLE = 10;
    private static final int LINE_SCAN_EDGE = 11;
    private static final int LINE_CHECK_RELAX = 12;
    private static final int LINE_RELAX = 13;
    private static final int LINE_PUSH = 14;
    private static final int LINE_RETURN = 15;

    private static final List<String> PSEUDOCODE = List.of(
            "dijkstra(G, source):",
            "    for each vertex v:",
            "        distance[v] = ∞; parent[v] = NIL; settled[v] = false",
            "    distance[source] = 0",
            "    priorityQueue.push((0, source))",
            "    while priorityQueue is not empty:",
            "        (known, u) = priorityQueue.popMin()",
            "        if known != distance[u]: continue  // stale entry",
            "        if settled[u]: continue",
            "        settled[u] = true",
            "        for each outgoing edge (u, v, weight):",
            "            if distance[u] + weight < distance[v]:",
            "                distance[v] = distance[u] + weight; parent[v] = u",
            "                priorityQueue.push((distance[v], v))",
            "    return distance, parent");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        WeightedGraphSupport.Input graph = WeightedGraphSupport.parse(
                input, TYPE, true, false, false);
        List<List<WeightedGraphSupport.Arc>> adjacency = WeightedGraphSupport.adjacency(graph);
        long[] distance = new long[graph.n()];
        Arrays.fill(distance, WeightedGraphSupport.INF);
        int[] parent = new int[graph.n()];
        Arrays.fill(parent, -1);
        boolean[] settled = new boolean[graph.n()];
        boolean[] acceptedEdges = new boolean[graph.edges().size()];
        int[] parentEdge = new int[graph.n()];
        Arrays.fill(parentEdge, -1);
        PriorityQueue<QueueEntry> pending = new PriorityQueue<>(QueueEntry.ORDER);
        WeightedGraphSupport.GraphTraceBuilder trace = new WeightedGraphSupport.GraphTraceBuilder(graph);
        int currentVertex = -1;
        long currentDistance = WeightedGraphSupport.INF;
        String phase = "initialize";

        trace.add(
                0,
                "Initialize Dijkstra on " + graph.n() + " vertices from source " + graph.source(),
                StepEventType.INITIALIZE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
        trace.add(
                LINE_METHOD,
                "Run Dijkstra with a min-priority queue and nonnegative edge weights",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
        trace.add(
                LINE_FOR_VERTEX,
                "Prepare distance, parent, and settled entries for every vertex",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
        trace.add(
                LINE_INITIALIZE,
                "Set every distance to ∞, every parent to NIL, and every settled flag to false",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));

        distance[graph.source()] = 0L;
        phase = "search";
        trace.nodeStatus(graph.source(), SnapshotStatus.ACTIVE);
        trace.add(
                LINE_SOURCE,
                "Set distance[" + graph.source() + "] = 0 for the source",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
        pending.add(new QueueEntry(0L, graph.source(), 0L));
        trace.add(
                LINE_PUSH_SOURCE,
                "Push (0, " + graph.source() + ") into the priority queue",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));

        long sequence = 1L;
        while (!pending.isEmpty()) {
            trace.add(
                    LINE_WHILE,
                    "Priority queue is not empty; frontier = " + formatQueue(pending),
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of(),
                    facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
            QueueEntry entry = pending.remove();
            currentVertex = entry.vertex();
            currentDistance = entry.distance();
            trace.nodeStatus(currentVertex, SnapshotStatus.ACTIVE);
            trace.add(
                    LINE_POP,
                    "Pop candidate (" + currentDistance + ", " + currentVertex + ")",
                    StepEventType.EXECUTE_LINE,
                    List.of(currentVertex),
                    List.of(),
                    facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
            if (entry.distance() != distance[currentVertex]) {
                trace.nodeStatus(currentVertex, SnapshotStatus.REJECTED);
                trace.add(
                        LINE_STALE,
                        "Skip stale queue entry for vertex " + currentVertex + ": known " + entry.distance()
                                + " != current distance " + WeightedGraphSupport.formatDistance(distance[currentVertex]),
                        StepEventType.EXECUTE_LINE,
                        List.of(currentVertex),
                        List.of(),
                        facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
                continue;
            }
            if (settled[currentVertex]) {
                trace.nodeStatus(currentVertex, SnapshotStatus.REJECTED);
                trace.add(
                        LINE_CHECK_SETTLED,
                        "Skip duplicate settled entry for vertex " + currentVertex,
                        StepEventType.EXECUTE_LINE,
                        List.of(currentVertex),
                        List.of(),
                        facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
                continue;
            }

            settled[currentVertex] = true;
            trace.nodeStatus(currentVertex, SnapshotStatus.DONE);
            trace.add(
                    LINE_SETTLE,
                    "Settle vertex " + currentVertex + " at final distance " + currentDistance,
                    StepEventType.EXECUTE_LINE,
                    List.of(currentVertex),
                    List.of(),
                    facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));

            for (WeightedGraphSupport.Arc arc : adjacency.get(currentVertex)) {
                int edgeIndex = arc.edgeIndex();
                trace.edgeStatus(edgeIndex, SnapshotStatus.ACTIVE);
                trace.add(
                        LINE_SCAN_EDGE,
                        "Inspect edge " + arc.from() + " -> " + arc.to() + " with weight " + arc.weight(),
                        StepEventType.EXECUTE_LINE,
                        List.of(arc.from(), arc.to()),
                        List.of(edgeIndex),
                        facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
                long candidate = safeAdd(distance[currentVertex], arc.weight());
                trace.add(
                        LINE_CHECK_RELAX,
                        WeightedGraphSupport.formatDistance(distance[currentVertex]) + " + " + arc.weight()
                                + " = " + WeightedGraphSupport.formatDistance(candidate) + " < "
                                + WeightedGraphSupport.formatDistance(distance[arc.to()]) + "? "
                                + (candidate < distance[arc.to()] ? "Yes: relax " : "No: keep distance of ") + arc.to(),
                        StepEventType.EXECUTE_LINE,
                        List.of(arc.from(), arc.to()),
                        List.of(edgeIndex),
                        facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
                if (candidate < distance[arc.to()]) {
                    distance[arc.to()] = candidate;
                    parent[arc.to()] = currentVertex;
                    if (parentEdge[arc.to()] >= 0) {
                        int oldEdge = parentEdge[arc.to()];
                        acceptedEdges[oldEdge] = false;
                        trace.edgeStatus(oldEdge, SnapshotStatus.REJECTED);
                    }
                    parentEdge[arc.to()] = edgeIndex;
                    acceptedEdges[edgeIndex] = true;
                    trace.nodeStatus(arc.to(), SnapshotStatus.ACTIVE);
                    trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
                    trace.add(
                            LINE_RELAX,
                            "Relax vertex " + arc.to() + ": distance = " + candidate + ", parent = " + currentVertex,
                            StepEventType.EXECUTE_LINE,
                            List.of(arc.from(), arc.to()),
                            List.of(edgeIndex),
                            facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
                    pending.add(new QueueEntry(candidate, arc.to(), sequence++));
                    trace.add(
                            LINE_PUSH,
                            "Push improved entry (" + candidate + ", " + arc.to() + ")",
                            StepEventType.EXECUTE_LINE,
                            List.of(arc.to()),
                            List.of(edgeIndex),
                            facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, false));
                } else if (!acceptedEdges[edgeIndex]) {
                    trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
                }
            }
            trace.nodeStatus(currentVertex, SnapshotStatus.DONE);
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.nodeStatus(vertex, settled[vertex] ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        Arrays.fill(acceptedEdges, false);
        for (int edgeIndex : parentEdge) {
            if (edgeIndex >= 0) acceptedEdges[edgeIndex] = true;
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            trace.edgeStatus(edgeIndex, acceptedEdges[edgeIndex]
                    ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        currentVertex = -1;
        currentDistance = WeightedGraphSupport.INF;
        phase = "return";
        trace.add(
                LINE_RETURN,
                "Return final shortest distances and parent pointers",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, true));
        phase = "complete";
        trace.add(
                0,
                "Complete: Dijkstra settled " + countTrue(settled) + " reachable vertex(es)",
                StepEventType.COMPLETE,
                List.of(),
                List.of(),
                facts(graph, distance, parent, settled, pending, currentVertex, currentDistance, phase, true));
        return trace.steps();
    }

    private static List<dev.codetrail.desktop.simulation.Fact> facts(
            WeightedGraphSupport.Input graph,
            long[] distance,
            int[] parent,
            boolean[] settled,
            PriorityQueue<QueueEntry> pending,
            int currentVertex,
            long currentDistance,
            String phase,
            boolean complete) {
        SnapshotStatus resultStatus = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        return List.of(
                WeightedGraphSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("source", Integer.toString(graph.source()), SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("current", currentVertex < 0 ? "NIL" : Integer.toString(currentVertex),
                        currentVertex < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("known-distance", WeightedGraphSupport.formatDistance(currentDistance),
                        currentVertex < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("distance", WeightedGraphSupport.formatDistance(distance), resultStatus),
                WeightedGraphSupport.fact("parent", WeightedGraphSupport.formatParents(parent, graph.source()), resultStatus),
                WeightedGraphSupport.fact("settled", formatSettled(settled), resultStatus),
                WeightedGraphSupport.fact("priority-queue", formatQueue(pending), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact(
                        "invariant",
                        "Every settled vertex has its final shortest distance; stale queue entries are skipped",
                        resultStatus),
                WeightedGraphSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static String formatSettled(boolean[] settled) {
        List<Integer> vertices = new ArrayList<>();
        for (int vertex = 0; vertex < settled.length; vertex++) {
            if (settled[vertex]) {
                vertices.add(vertex);
            }
        }
        return WeightedGraphSupport.formatVertices(vertices);
    }

    private static String formatQueue(PriorityQueue<QueueEntry> pending) {
        List<QueueEntry> entries = new ArrayList<>(pending);
        entries.sort(QueueEntry.ORDER);
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (QueueEntry entry : entries) {
            result.add(entry.vertex() + ":" + entry.distance());
        }
        return result.toString();
    }

    private static long safeAdd(long base, long weight) {
        if (base >= WeightedGraphSupport.INF) {
            return WeightedGraphSupport.INF;
        }
        if (weight > 0L && base > WeightedGraphSupport.INF - weight) {
            return WeightedGraphSupport.INF;
        }
        return base + weight;
    }

    private static int countTrue(boolean[] values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
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
                        new WeightedGraphSupport.WeightedEdge(0, 2, 1),
                        new WeightedGraphSupport.WeightedEdge(2, 1, 2),
                        new WeightedGraphSupport.WeightedEdge(1, 3, 1),
                        new WeightedGraphSupport.WeightedEdge(2, 3, 5),
                        new WeightedGraphSupport.WeightedEdge(3, 4, 2)),
                0,
                true);
        return new SimulationMetadata(
                TYPE,
                "Dijkstra's Algorithm",
                "O((V + E) log V)",
                "O(V + E)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"n\":5,\"edges\":[{\"from\":0,\"to\":1,\"weight\":4}],\"source\":0,\"directed\":true}; n must be "
                        + MIN_N + ".." + MAX_N + ", there may be at most " + MAX_EDGES
                        + " unique logical edges, weights are integers from 0 through " + MAX_WEIGHT
                        + ", and directed=false adds each edge in both directions.",
                PSEUDOCODE);
    }

    private record QueueEntry(long distance, int vertex, long sequence) {
        private static final Comparator<QueueEntry> ORDER = Comparator
                .comparingLong(QueueEntry::distance)
                .thenComparingInt(QueueEntry::vertex)
                .thenComparingLong(QueueEntry::sequence);
    }
}
