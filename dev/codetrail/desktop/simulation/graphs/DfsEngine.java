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

/** Depth-first traversal with real recursive call frames and discovery/finish times. */
public final class DfsEngine implements SimulationEngine {
    public static final String TYPE = "DFS";
    public static final int MIN_N = GraphInput.MIN_N;
    public static final int MAX_N = GraphInput.MAX_N;
    public static final int MAX_EDGES = GraphInput.MAX_EDGES;
    public static final int MAX_TRACE_STEPS = GraphTraceSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_FOR_VERTEX = 2;
    private static final int LINE_INITIALIZE = 3;
    private static final int LINE_TIME = 4;
    private static final int LINE_CALL_SOURCE = 5;
    private static final int LINE_VISIT = 6;
    private static final int LINE_DISCOVER = 7;
    private static final int LINE_FOR_NEIGHBOR = 8;
    private static final int LINE_CHECK_VISITED = 9;
    private static final int LINE_PARENT = 10;
    private static final int LINE_RECURSE = 11;
    private static final int LINE_FINISH = 12;
    private static final int LINE_RETURN = 13;

    private static final List<String> PSEUDOCODE = List.of(
            "DFS(G, source):",
            "    for each vertex v:",
            "        visited[v] = false; parent[v] = NIL",
            "    time = 0",
            "    visit(source)",
            "visit(u):",
            "    visited[u] = true; discovery[u] = ++time",
            "    for each neighbor v of u:",
            "        if not visited[v]:",
            "            parent[v] = u",
            "            visit(v)",
            "    finish[u] = ++time",
            "return visited, discovery, finish, parent");

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
        boolean[] discovered = new boolean[graph.n()];
        boolean[] finished = new boolean[graph.n()];
        boolean[] treeEdges = new boolean[graph.edges().size()];
        int[] discoveryTimes = new int[graph.n()];
        int[] finishTimes = new int[graph.n()];
        int[] parents = new int[graph.n()];
        Arrays.fill(parents, -1);
        List<Integer> discoveryOrder = new ArrayList<>();
        Deque<Integer> callStack = new ArrayDeque<>();
        int[] time = {0};
        int[] nextFrame = {0};

        trace.add(
                0,
                "Initialize DFS on " + graph.n() + " vertices from source " + graph.source(),
                StepEventType.INITIALIZE,
                List.of(graph.source()),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false));
        trace.add(
                LINE_METHOD,
                "Run depth-first search from source " + graph.source(),
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false));

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.add(
                    LINE_FOR_VERTEX,
                    "Initialize vertex " + vertex,
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                            finished, time[0], false));
            trace.add(
                    LINE_INITIALIZE,
                    "Set visited[" + vertex + "] = false and parent[" + vertex + "] = NIL",
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                            finished, time[0], false));
        }

        trace.add(
                LINE_TIME,
                "Set traversal time = 0",
                StepEventType.EXECUTE_LINE,
                List.of(),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false));
        trace.add(
                LINE_CALL_SOURCE,
                "Call visit(" + graph.source() + ")",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false));

        visit(
                graph,
                adjacency,
                trace,
                graph.source(),
                visited,
                discovered,
                finished,
                treeEdges,
                discoveryTimes,
                finishTimes,
                parents,
                discoveryOrder,
                callStack,
                time,
                nextFrame);

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.nodeStatus(vertex, discovered[vertex] ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            if (!treeEdges[edgeIndex]) {
                trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
            }
        }
        trace.add(
                LINE_RETURN,
                "Return visited set, discovery/finish times, and parent tree",
                StepEventType.EXECUTE_LINE,
                List.of(),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], true));
        trace.add(
                0,
                "Complete: DFS reached " + discoveryOrder.size() + " of " + graph.n() + " vertices",
                StepEventType.COMPLETE,
                List.of(),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], true));
        return trace.steps();
    }

    private static void visit(
            GraphInput graph,
            List<List<GraphTraceSupport.Neighbor>> adjacency,
            GraphTraceSupport.TraceBuilder trace,
            int current,
            boolean[] visited,
            boolean[] discovered,
            boolean[] finished,
            boolean[] treeEdges,
            int[] discoveryTimes,
            int[] finishTimes,
            int[] parents,
            List<Integer> discoveryOrder,
            Deque<Integer> callStack,
            int[] time,
            int[] nextFrame) {
        String frameId = "dfs-" + nextFrame[0]++;
        if (!callStack.isEmpty()) {
            trace.nodeStatus(callStack.peekLast(), SnapshotStatus.DEFAULT);
        }
        callStack.addLast(current);
        trace.nodeStatus(current, SnapshotStatus.ACTIVE);
        trace.add(
                LINE_VISIT,
                "Push visit(" + current + ") onto the DFS call stack",
                StepEventType.PUSH_FRAME,
                List.copyOf(callStack),
                List.of(),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false),
                frameId);

        visited[current] = true;
        discovered[current] = true;
        discoveryTimes[current] = ++time[0];
        discoveryOrder.add(current);
        trace.add(
                LINE_DISCOVER,
                "Discover vertex " + current + " at time " + discoveryTimes[current],
                StepEventType.EXECUTE_LINE,
                List.copyOf(callStack),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false));

        for (GraphTraceSupport.Neighbor neighbor : adjacency.get(current)) {
            int next = neighbor.vertex();
            int edgeIndex = neighbor.edgeIndex();
            trace.edgeStatus(edgeIndex, SnapshotStatus.ACTIVE);
            trace.add(
                    LINE_FOR_NEIGHBOR,
                    "Inspect edge " + current + " -> " + next + " from vertex " + current,
                    StepEventType.EXECUTE_LINE,
                    List.copyOf(callStack),
                    List.of(edgeIndex),
                    facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                            finished, time[0], false));
            trace.add(
                    LINE_CHECK_VISITED,
                    "Check whether vertex " + next + " is already visited",
                    StepEventType.EXECUTE_LINE,
                    List.copyOf(callStack),
                    List.of(edgeIndex),
                    facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                            finished, time[0], false));

            if (!visited[next]) {
                parents[next] = current;
                treeEdges[edgeIndex] = true;
                trace.add(
                        LINE_PARENT,
                        "Set parent[" + next + "] = " + current,
                        StepEventType.EXECUTE_LINE,
                        List.copyOf(callStack),
                        List.of(edgeIndex),
                        facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                                finished, time[0], false));
                trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
                trace.add(
                        LINE_RECURSE,
                        "Recursively call visit(" + next + ")",
                        StepEventType.EXECUTE_LINE,
                        List.of(current, next),
                        List.of(edgeIndex),
                        facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                                finished, time[0], false));
                visit(
                        graph,
                        adjacency,
                        trace,
                        next,
                        visited,
                        discovered,
                        finished,
                        treeEdges,
                        discoveryTimes,
                        finishTimes,
                        parents,
                        discoveryOrder,
                        callStack,
                        time,
                        nextFrame);
                trace.nodeStatus(current, SnapshotStatus.ACTIVE);
            } else if (!treeEdges[edgeIndex]) {
                trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
                String message = current == next
                        ? "Self-loop at " + current + " is already visited; do not recurse"
                        : "Vertex " + next + " is already visited; reject this non-tree edge";
                trace.add(
                        LINE_CHECK_VISITED,
                        message,
                        StepEventType.EXECUTE_LINE,
                        List.copyOf(callStack),
                        List.of(edgeIndex),
                        facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                                finished, time[0], false));
            } else {
                trace.add(
                        LINE_CHECK_VISITED,
                        "Tree edge to " + next + " is already recorded; do not recurse again",
                        StepEventType.EXECUTE_LINE,
                        List.copyOf(callStack),
                        List.of(edgeIndex),
                        facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                                finished, time[0], false));
            }
        }

        finishTimes[current] = ++time[0];
        finished[current] = true;
        trace.nodeStatus(current, SnapshotStatus.DONE);
        trace.add(
                LINE_FINISH,
                "Finish vertex " + current + " at time " + finishTimes[current],
                StepEventType.EXECUTE_LINE,
                List.copyOf(callStack),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false));
        callStack.removeLast();
        if (!callStack.isEmpty()) {
            trace.nodeStatus(callStack.peekLast(), SnapshotStatus.ACTIVE);
        }
        trace.add(
                LINE_FINISH,
                "Return from visit(" + current + ") and pop its call frame",
                StepEventType.RETURN_FRAME,
                List.copyOf(callStack),
                facts(graph, callStack, discoveryOrder, parents, discovered, discoveryTimes, finishTimes,
                        finished, time[0], false),
                frameId);
    }

    private static List<Fact> facts(
            GraphInput graph,
            Deque<Integer> callStack,
            List<Integer> discoveryOrder,
            int[] parents,
            boolean[] discovered,
            int[] discoveryTimes,
            int[] finishTimes,
            boolean[] finished,
            int time,
            boolean complete) {
        SnapshotStatus evolving = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        return List.of(
                GraphTraceSupport.fact("source", Integer.toString(graph.source()), SnapshotStatus.DEFAULT),
                GraphTraceSupport.fact("stack", GraphTraceSupport.formatVertices(callStack), evolving),
                GraphTraceSupport.fact("callStack", GraphTraceSupport.formatVertices(callStack), evolving),
                GraphTraceSupport.fact(
                        "visited", GraphTraceSupport.formatVertices(discoveryOrder), evolving),
                GraphTraceSupport.fact(
                        "parent", GraphTraceSupport.formatParentMap(parents, discovered), evolving),
                GraphTraceSupport.fact(
                        "discovery", GraphTraceSupport.formatTimeMap(discoveryTimes, discovered), evolving),
                GraphTraceSupport.fact(
                        "finish", GraphTraceSupport.formatTimeMap(finishTimes, finished), evolving),
                GraphTraceSupport.fact("time", Integer.toString(time), evolving),
                GraphTraceSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static SimulationMetadata createMetadata() {
        ObjectNode defaultInput = GraphInput.defaultInput();
        return new SimulationMetadata(
                TYPE,
                "Depth-First Search",
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
