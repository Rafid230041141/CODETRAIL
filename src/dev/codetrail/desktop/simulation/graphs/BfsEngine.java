package dev.codetrail.desktop.simulation.graphs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/** Bounded breadth-first traversal with explicit queue, parent, and distances. */
public final class BfsEngine implements SimulationEngine {
    public static final String TYPE = "BFS";
    public static final int MIN_N = GraphInput.MIN_N;
    public static final int MAX_N = GraphInput.MAX_N;
    public static final int MAX_EDGES = GraphInput.MAX_EDGES;
    public static final int MAX_TRACE_STEPS = GraphTraceSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_FOR_VERTEX = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_SOURCE = 4;
    private static final int LINE_ENQUEUE_SOURCE = 5;
    private static final int LINE_WHILE = 6;
    private static final int LINE_DEQUEUE = 7;
    private static final int LINE_FOR_NEIGHBOR = 8;
    private static final int LINE_CHECK_VISITED = 9;
    private static final int LINE_MARK_VISITED = 10;
    private static final int LINE_PARENT_DISTANCE = 11;
    private static final int LINE_ENQUEUE = 12;
    private static final int LINE_RETURN = 13;

    private static final List<String> PSEUDOCODE = List.of(
            "BFS(G, source):",
            "    for each vertex v:",
            "        visited[v] = false; distance[v] = ∞; parent[v] = NIL",
            "    visited[source] = true; distance[source] = 0",
            "    queue.enqueue(source)",
            "    while queue is not empty:",
            "        u = queue.dequeue()",
            "        for each neighbor v of u:",
            "            if not visited[v]:",
            "                visited[v] = true",
            "                parent[v] = u; distance[v] = distance[u] + 1",
            "                queue.enqueue(v)",
            "    return visited, distance, parent");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        GraphInput graph = GraphInput.parse(input);
        GraphTraceSupport.TraceBuilder trace = new GraphTraceSupport.TraceBuilder(graph);
        List<List<GraphTraceSupport.Neighbor>> adjacency = GraphTraceSupport.adjacency(graph);
        boolean[] visited = new boolean[graph.n()];
        boolean[] known = new boolean[graph.n()];
        boolean[] treeEdges = new boolean[graph.edges().size()];
        int[] distances = new int[graph.n()];
        int[] parents = new int[graph.n()];
        Arrays.fill(distances, -1);
        Arrays.fill(parents, -1);
        List<Integer> discoveryOrder = new ArrayList<>();
        Deque<Integer> queue = new ArrayDeque<>();

        trace.add(
                0,
                "Initialize BFS on " + graph.n() + " vertices from source " + graph.source(),
                StepEventType.INITIALIZE,
                List.of(graph.source()),
                facts(graph, queue, discoveryOrder, parents, known, distances, false));
        trace.add(
                LINE_METHOD,
                "Run breadth-first search from source " + graph.source(),
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                facts(graph, queue, discoveryOrder, parents, known, distances, false));

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.add(
                    LINE_FOR_VERTEX,
                    "Initialize vertex " + vertex,
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    facts(graph, queue, discoveryOrder, parents, known, distances, false));
            trace.add(
                    LINE_INITIALIZE,
                    "Set visited[" + vertex + "] = false, distance[" + vertex + "] = ∞, parent["
                            + vertex + "] = NIL",
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    facts(graph, queue, discoveryOrder, parents, known, distances, false));
        }

        int source = graph.source();
        visited[source] = true;
        known[source] = true;
        distances[source] = 0;
        discoveryOrder.add(source);
        trace.nodeStatus(source, SnapshotStatus.ACTIVE);
        trace.add(
                LINE_SOURCE,
                "Mark source " + source + " visited with distance 0",
                StepEventType.EXECUTE_LINE,
                List.of(source),
                facts(graph, queue, discoveryOrder, parents, known, distances, false));

        queue.addLast(source);
        trace.add(
                LINE_ENQUEUE_SOURCE,
                "Enqueue source " + source + "; queue = " + GraphTraceSupport.formatVertices(queue),
                StepEventType.EXECUTE_LINE,
                List.copyOf(queue),
                facts(graph, queue, discoveryOrder, parents, known, distances, false));

        while (!queue.isEmpty()) {
            trace.add(
                    LINE_WHILE,
                    "Queue is not empty; frontier = " + GraphTraceSupport.formatVertices(queue),
                    StepEventType.EXECUTE_LINE,
                    List.copyOf(queue),
                    facts(graph, queue, discoveryOrder, parents, known, distances, false));
            int current = queue.removeFirst();
            trace.nodeStatus(current, SnapshotStatus.ACTIVE);
            trace.add(
                    LINE_DEQUEUE,
                    "Dequeue vertex " + current + " for expansion",
                    StepEventType.EXECUTE_LINE,
                    List.of(current),
                    facts(graph, queue, discoveryOrder, parents, known, distances, false));

            for (GraphTraceSupport.Neighbor neighbor : adjacency.get(current)) {
                int next = neighbor.vertex();
                int edgeIndex = neighbor.edgeIndex();
                trace.edgeStatus(edgeIndex, SnapshotStatus.ACTIVE);
                trace.add(
                        LINE_FOR_NEIGHBOR,
                        "Inspect edge " + current + " -> " + next + " from vertex " + current,
                        StepEventType.EXECUTE_LINE,
                        List.of(current, next),
                        List.of(edgeIndex),
                        facts(graph, queue, discoveryOrder, parents, known, distances, false));
                trace.add(
                        LINE_CHECK_VISITED,
                        "visited[" + next + "] = " + visited[next]
                                + (visited[next] ? "; keep its first discovery" : "; discover it once"),
                        StepEventType.EXECUTE_LINE,
                        List.of(current, next),
                        List.of(edgeIndex),
                        facts(graph, queue, discoveryOrder, parents, known, distances, false));

                if (!visited[next]) {
                    visited[next] = true;
                    discoveryOrder.add(next);
                    treeEdges[edgeIndex] = true;
                    trace.nodeStatus(next, SnapshotStatus.ACTIVE);
                    trace.add(
                            LINE_MARK_VISITED,
                            "Mark vertex " + next + " visited",
                            StepEventType.EXECUTE_LINE,
                            List.of(current, next),
                            List.of(edgeIndex),
                            facts(graph, queue, discoveryOrder, parents, known, distances, false));
                    known[next] = true;
                    distances[next] = distances[current] + 1;
                    parents[next] = current;
                    trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
                    trace.add(
                            LINE_PARENT_DISTANCE,
                            "Set parent[" + next + "] = " + current + " and distance[" + next + "] = "
                                    + distances[next],
                            StepEventType.EXECUTE_LINE,
                            List.of(current, next),
                            List.of(edgeIndex),
                            facts(graph, queue, discoveryOrder, parents, known, distances, false));
                    queue.addLast(next);
                    trace.add(
                            LINE_ENQUEUE,
                            "Enqueue newly discovered vertex " + next + "; queue = "
                                    + GraphTraceSupport.formatVertices(queue),
                            StepEventType.EXECUTE_LINE,
                            List.copyOf(queue),
                            List.of(edgeIndex),
                            facts(graph, queue, discoveryOrder, parents, known, distances, false));
                } else if (!treeEdges[edgeIndex]) {
                    trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
                    String message = current == next
                            ? "Self-loop at " + current + " is already visited; do not enqueue it"
                            : "Vertex " + next + " is already visited; reject this non-tree edge";
                    trace.add(
                            LINE_CHECK_VISITED,
                            message,
                            StepEventType.EXECUTE_LINE,
                            List.of(current, next),
                            List.of(edgeIndex),
                            facts(graph, queue, discoveryOrder, parents, known, distances, false));
                } else {
                    trace.add(
                            LINE_CHECK_VISITED,
                            "Tree edge to " + next + " is already recorded; do not rediscover it",
                            StepEventType.EXECUTE_LINE,
                            List.of(current, next),
                            List.of(edgeIndex),
                            facts(graph, queue, discoveryOrder, parents, known, distances, false));
                }
            }

            trace.nodeStatus(current, SnapshotStatus.DONE);
            trace.add(
                    LINE_FOR_NEIGHBOR,
                    "Finished scanning neighbors of vertex " + current,
                    StepEventType.EXECUTE_LINE,
                    List.of(current),
                    facts(graph, queue, discoveryOrder, parents, known, distances, false));
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (visited[vertex]) {
                trace.nodeStatus(vertex, SnapshotStatus.DONE);
            } else {
                trace.nodeStatus(vertex, SnapshotStatus.REJECTED);
            }
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            if (!treeEdges[edgeIndex]) {
                trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
            }
        }
        trace.add(
                LINE_RETURN,
                "Return visited set, shortest unweighted distances, and parent tree",
                StepEventType.EXECUTE_LINE,
                List.of(),
                facts(graph, queue, discoveryOrder, parents, known, distances, true));
        trace.add(
                0,
                "Complete: BFS reached " + discoveryOrder.size() + " of " + graph.n() + " vertices",
                StepEventType.COMPLETE,
                List.of(),
                facts(graph, queue, discoveryOrder, parents, known, distances, true));
        return trace.steps();
    }

    private static List<Fact> facts(
            GraphInput graph,
            Deque<Integer> queue,
            List<Integer> discoveryOrder,
            int[] parents,
            boolean[] known,
            int[] distances,
            boolean complete) {
        SnapshotStatus evolving = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        String frontier = GraphTraceSupport.formatVertices(queue);
        return List.of(
                GraphTraceSupport.fact("source", Integer.toString(graph.source()), SnapshotStatus.DEFAULT),
                GraphTraceSupport.fact("frontier", frontier, evolving),
                GraphTraceSupport.fact("queue", frontier, evolving),
                GraphTraceSupport.fact(
                        "visited", GraphTraceSupport.formatVertices(discoveryOrder), evolving),
                GraphTraceSupport.fact(
                        "parent", GraphTraceSupport.formatParentMap(parents, known), evolving),
                GraphTraceSupport.fact(
                        "distance", GraphTraceSupport.formatDistanceMap(distances, known), evolving),
                GraphTraceSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = GraphInput.defaultInput();
        return new SimulationMetadata(
                TYPE,
                "Breadth-First Search",
                "O(n + m)",
                "O(n)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"n\":6,\"edges\":[[0,1],[0,2]],\"source\":0,\"directed\":false}; n must be 1.."
                        + MAX_N + ", endpoints must be in range, there may be at most " + MAX_EDGES
                        + " unique logical edges, and self-loops are accepted and examined once.",
                PSEUDOCODE);
    }
}
