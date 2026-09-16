package dev.codetrail.desktop.simulation.graphs.structural;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/** Kahn topological sort with a deterministic smallest-ready-vertex queue. */
public final class TopologicalSortEngine implements SimulationEngine {
    public static final String TYPE = "TOPOLOGICAL_SORT";
    public static final int MIN_N = StructuralGraphSupport.MIN_N;
    public static final int MAX_N = StructuralGraphSupport.MAX_N;
    public static final int MAX_EDGES = StructuralGraphSupport.MAX_EDGES;
    public static final int MAX_TRACE_STEPS = StructuralGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_ZERO_INDEGREE = 2;
    private static final int LINE_COUNT_INDEGREE = 3;
    private static final int LINE_READY = 4;
    private static final int LINE_ORDER = 5;
    private static final int LINE_WHILE = 6;
    private static final int LINE_REMOVE = 7;
    private static final int LINE_APPEND = 8;
    private static final int LINE_FOR_EDGE = 9;
    private static final int LINE_DECREMENT = 10;
    private static final int LINE_ENQUEUE = 11;
    private static final int LINE_CYCLE = 12;
    private static final int LINE_RETURN = 13;

    private static final List<String> PSEUDOCODE = List.of(
            "topologicalSort(G):",
            "    indegree[v] = 0 for each vertex v",
            "    for each edge (u, v): indegree[v]++",
            "    ready = all vertices with indegree 0",
            "    order = []",
            "    while ready is not empty:",
            "        u = remove the smallest vertex from ready",
            "        append u to order",
            "        for each edge u -> v:",
            "            indegree[v]--",
            "            if indegree[v] == 0: add v to ready",
            "    if |order| != |V|: report a directed cycle",
            "    return order");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        StructuralGraphSupport.GraphInput graph =
                StructuralGraphSupport.parsePairGraph(input, true, false);
        List<List<StructuralGraphSupport.Neighbor>> adjacency =
                StructuralGraphSupport.adjacency(graph);
        List<String> labels = new ArrayList<>(graph.edges().size());
        for (StructuralGraphSupport.InputEdge edge : graph.edges()) {
            labels.add(edge.from() + "→" + edge.to());
        }
        StructuralGraphSupport.TraceBuilder trace =
                new StructuralGraphSupport.TraceBuilder(graph, labels);
        int[] indegree = new int[graph.n()];
        boolean[] processedEdges = new boolean[graph.edges().size()];
        boolean[] completedVertices = new boolean[graph.n()];
        List<Integer> order = new ArrayList<>();
        PriorityQueue<Integer> ready = new PriorityQueue<>();

        add(
                trace,
                graph,
                indegree,
                ready,
                order,
                false,
                0,
                "Initialize Kahn's topological sort for " + graph.n() + " directed vertices",
                StepEventType.INITIALIZE,
                List.of(),
                List.of());

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            add(
                    trace,
                    graph,
                    indegree,
                    ready,
                    order,
                    false,
                    LINE_ZERO_INDEGREE,
                    "Set indegree[" + vertex + "] = 0",
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of());
        }

        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
            indegree[edge.to()]++;
            labelNodes(trace, indegree);
            add(
                    trace,
                    graph,
                    indegree,
                    ready,
                    order,
                    false,
                    LINE_COUNT_INDEGREE,
                    "Count edge " + edge.from() + "→" + edge.to()
                            + "; indegree[" + edge.to() + "] = " + indegree[edge.to()],
                    StepEventType.EXECUTE_LINE,
                    List.of(edge.from(), edge.to()),
                    List.of(edgeIndex));
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (indegree[vertex] == 0) {
                ready.add(vertex);
            }
        }
        labelNodes(trace, indegree);
        add(
                trace,
                graph,
                indegree,
                ready,
                order,
                false,
                LINE_READY,
                "Ready queue contains all zero-indegree vertices: " + formatReady(ready),
                StepEventType.EXECUTE_LINE,
                sortedReady(ready),
                List.of());
        add(
                trace,
                graph,
                indegree,
                ready,
                order,
                false,
                LINE_ORDER,
                "Start with an empty topological order",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of());

        while (!ready.isEmpty()) {
            add(
                    trace,
                    graph,
                    indegree,
                    ready,
                    order,
                    false,
                    LINE_WHILE,
                    "Ready queue is not empty: " + formatReady(ready),
                    StepEventType.EXECUTE_LINE,
                    sortedReady(ready),
                    List.of());
            int vertex = ready.remove();
            completedVertices[vertex] = true;
            trace.nodeStatus(vertex, SnapshotStatus.ACTIVE);
            add(
                    trace,
                    graph,
                    indegree,
                    ready,
                    order,
                    false,
                    LINE_REMOVE,
                    "Remove vertex " + vertex + " from the ready queue",
                    StepEventType.EXECUTE_LINE,
                    List.of(vertex),
                    List.of());
            order.add(vertex);
            trace.nodeStatus(vertex, SnapshotStatus.DONE);
            add(
                    trace,
                    graph,
                    indegree,
                    ready,
                    order,
                    false,
                    LINE_APPEND,
                    "Append " + vertex + " to order = " + StructuralGraphSupport.formatVertices(order),
                    StepEventType.EXECUTE_LINE,
                    List.of(vertex),
                    List.of());

            for (StructuralGraphSupport.Neighbor neighbor : adjacency.get(vertex)) {
                int next = neighbor.vertex();
                int edgeIndex = neighbor.edgeIndex();
                trace.edgeStatus(edgeIndex, SnapshotStatus.ACTIVE);
                add(
                        trace,
                        graph,
                        indegree,
                        ready,
                        order,
                        false,
                        LINE_FOR_EDGE,
                        "Inspect outgoing edge " + vertex + "→" + next,
                        StepEventType.EXECUTE_LINE,
                        List.of(vertex, next),
                        List.of(edgeIndex));
                indegree[next]--;
                processedEdges[edgeIndex] = true;
                labelNodes(trace, indegree);
                add(
                        trace,
                        graph,
                        indegree,
                        ready,
                        order,
                        false,
                        LINE_DECREMENT,
                        "Decrement indegree[" + next + "] to " + indegree[next],
                        StepEventType.EXECUTE_LINE,
                        List.of(vertex, next),
                        List.of(edgeIndex));
                trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
                if (indegree[next] == 0) {
                    ready.add(next);
                    add(
                            trace,
                            graph,
                            indegree,
                            ready,
                            order,
                            false,
                            LINE_ENQUEUE,
                            "Add newly ready vertex " + next + "; ready = " + formatReady(ready),
                            StepEventType.EXECUTE_LINE,
                            sortedReady(ready),
                            List.of(edgeIndex));
                }
            }
        }

        boolean cycle = order.size() != graph.n();
        if (cycle) {
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                if (!completedVertices[vertex]) {
                    trace.nodeStatus(vertex, SnapshotStatus.REJECTED);
                }
            }
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                if (!processedEdges[edgeIndex]) {
                    trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
                }
            }
            add(
                    trace,
                    graph,
                    indegree,
                    ready,
                    order,
                    true,
                    LINE_CYCLE,
                    "Cycle outcome: only " + order.size() + " of " + graph.n()
                            + " vertices reached zero indegree",
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of());
        } else {
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                trace.nodeStatus(vertex, SnapshotStatus.DONE);
            }
            for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
                trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
            }
        }

        add(
                trace,
                graph,
                indegree,
                ready,
                order,
                cycle,
                LINE_RETURN,
                cycle
                        ? "Return the partial order and report that no topological order exists"
                        : "Return topological order " + StructuralGraphSupport.formatVertices(order),
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of());
        add(
                trace,
                graph,
                indegree,
                ready,
                order,
                cycle,
                0,
                cycle
                        ? "Complete: directed cycle detected"
                        : "Complete: topological order contains all " + graph.n() + " vertices",
                StepEventType.COMPLETE,
                List.of(),
                List.of());
        return trace.steps();
    }

    private static void add(
            StructuralGraphSupport.TraceBuilder trace,
            StructuralGraphSupport.GraphInput graph,
            int[] indegree,
            PriorityQueue<Integer> ready,
            List<Integer> order,
            boolean cycle,
            int line,
            String narration,
            StepEventType event,
            Iterable<Integer> activeVertices,
            Iterable<Integer> activeEdges) {
        labelNodes(trace, indegree);
        trace.add(
                line,
                narration,
                event,
                activeVertices,
                activeEdges,
                facts(indegree, ready, order, cycle));
    }

    private static List<Fact> facts(
            int[] indegree, PriorityQueue<Integer> ready, List<Integer> order, boolean cycle) {
        SnapshotStatus status = cycle ? SnapshotStatus.REJECTED : SnapshotStatus.ACTIVE;
        return List.of(
                StructuralGraphSupport.fact(
                        "indegree", StructuralGraphSupport.formatIntArray(indegree, "?"), status),
                StructuralGraphSupport.fact("ready", formatReady(ready), status),
                StructuralGraphSupport.fact(
                        "order", StructuralGraphSupport.formatVertices(order), status),
                StructuralGraphSupport.fact("cycle", Boolean.toString(cycle), status),
                StructuralGraphSupport.fact(
                        "remaining", Integer.toString(indegree.length - order.size()), status));
    }

    private static void labelNodes(StructuralGraphSupport.TraceBuilder trace, int[] indegree) {
        for (int vertex = 0; vertex < indegree.length; vertex++) {
            trace.nodeLabel(vertex, vertex + " | indegree=" + indegree[vertex]);
        }
    }

    private static String formatReady(PriorityQueue<Integer> ready) {
        return StructuralGraphSupport.formatVertices(sortedReady(ready));
    }

    private static List<Integer> sortedReady(PriorityQueue<Integer> ready) {
        List<Integer> values = new ArrayList<>(ready);
        Collections.sort(values);
        return values;
    }

    private static SimulationMetadata createMetadata() {
        return new SimulationMetadata(
                TYPE,
                "Topological Sort (Kahn's Algorithm)",
                "O(V + E)",
                "O(V + E)",
                RendererFamily.GRAPH,
                StructuralGraphSupport.pairDefault(
                        5,
                        new int[][] {{0, 2}, {1, 2}, {2, 3}, {1, 4}},
                        true),
                "Input is {n, edges, directed}. Use n in 1..10, at most 24 integer "
                        + "pairs, and directed=true. Duplicate pairs count as separate "
                        + "dependencies; a self-loop is a cycle. The ready queue chooses "
                        + "the smallest available vertex, and an incomplete order reports a cycle.",
                PSEUDOCODE);
    }
}
