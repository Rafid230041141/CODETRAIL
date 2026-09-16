package dev.codetrail.desktop.simulation.graphs.structural;

import com.fasterxml.jackson.databind.JsonNode;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.RendererFamily;
import dev.codetrail.desktop.simulation.SimulationEngine;
import dev.codetrail.desktop.simulation.SimulationMetadata;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Tarjan's one-pass strongly connected component algorithm. */
public final class TarjanSccEngine implements SimulationEngine {
    public static final String TYPE = "TARJAN_SCC";
    public static final int MIN_N = StructuralGraphSupport.MIN_N;
    public static final int MAX_N = StructuralGraphSupport.MAX_N;
    public static final int MAX_EDGES = StructuralGraphSupport.MAX_EDGES;
    public static final int MAX_TRACE_STEPS = StructuralGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_SKIP = 2;
    private static final int LINE_INDEX = 3;
    private static final int LINE_PUSH = 4;
    private static final int LINE_EDGE = 5;
    private static final int LINE_VISIT = 6;
    private static final int LINE_RECURSE = 7;
    private static final int LINE_LOW_CHILD = 8;
    private static final int LINE_ON_STACK = 9;
    private static final int LINE_LOW_INDEX = 10;
    private static final int LINE_ROOT = 11;
    private static final int LINE_POP = 12;
    private static final int LINE_COMPONENT = 13;
    private static final int LINE_RETURN = 14;

    private static final List<String> PSEUDOCODE = List.of(
            "tarjan(G):",
            "    for each vertex v with index[v] undefined:",
            "        index[v] = low[v] = nextIndex++",
            "        push v onto stack; onStack[v] = true",
            "        for each edge v -> w:",
            "            if index[w] is undefined:",
            "                visit(w)",
            "                low[v] = min(low[v], low[w])",
            "            else if onStack[w]:",
            "                low[v] = min(low[v], index[w])",
            "        if low[v] == index[v]:",
            "            pop through v",
            "            assign one strongly connected component",
            "        return");

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
        TarjanRun run = new TarjanRun(graph, adjacency, trace);
        run.add(
                0,
                "Initialize Tarjan SCC on " + graph.n() + " directed vertices",
                StepEventType.INITIALIZE,
                List.of(),
                List.of());
        run.add(
                LINE_METHOD,
                "Scan every vertex so disconnected directed components are covered",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of());

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            if (run.index[vertex] == -1) {
                run.visit(vertex);
            } else {
                run.add(
                        LINE_SKIP,
                        "Vertex " + vertex + " already has index " + run.index[vertex]
                                + "; keep its existing component state",
                        StepEventType.EXECUTE_LINE,
                        List.of(vertex),
                        List.of());
            }
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            run.trace.nodeStatus(vertex, SnapshotStatus.DONE);
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            run.trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
        }
        run.add(
                LINE_RETURN,
                "Return " + run.components.size() + " strongly connected components",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of());
        run.add(
                0,
                "Complete: Tarjan found " + run.components.size() + " SCCs",
                StepEventType.COMPLETE,
                List.of(),
                List.of());
        return run.trace.steps();
    }

    private static final class TarjanRun {
        private final StructuralGraphSupport.GraphInput graph;
        private final List<List<StructuralGraphSupport.Neighbor>> adjacency;
        private final StructuralGraphSupport.TraceBuilder trace;
        private final int[] index;
        private final int[] low;
        private final int[] component;
        private final boolean[] onStack;
        private final Deque<Integer> stack = new ArrayDeque<>();
        private final List<List<Integer>> components = new ArrayList<>();
        private int nextIndex;

        private TarjanRun(
                StructuralGraphSupport.GraphInput graph,
                List<List<StructuralGraphSupport.Neighbor>> adjacency,
                StructuralGraphSupport.TraceBuilder trace) {
            this.graph = graph;
            this.adjacency = adjacency;
            this.trace = trace;
            index = new int[graph.n()];
            low = new int[graph.n()];
            component = new int[graph.n()];
            onStack = new boolean[graph.n()];
            java.util.Arrays.fill(index, -1);
            java.util.Arrays.fill(low, -1);
            java.util.Arrays.fill(component, -1);
        }

        private void visit(int vertex) {
            index[vertex] = low[vertex] = nextIndex++;
            trace.nodeStatus(vertex, SnapshotStatus.ACTIVE);
            add(
                    LINE_INDEX,
                    "Assign index[" + vertex + "] = " + index[vertex]
                            + " and low[" + vertex + "] = " + low[vertex],
                    StepEventType.EXECUTE_LINE,
                    List.of(vertex),
                    List.of());

            stack.addLast(vertex);
            onStack[vertex] = true;
            add(
                    LINE_PUSH,
                    "Push " + vertex + " onto the active Tarjan stack",
                    StepEventType.PUSH_FRAME,
                    List.copyOf(stack),
                    List.of(),
                    "tarjan-" + vertex);

            for (StructuralGraphSupport.Neighbor neighbor : adjacency.get(vertex)) {
                int next = neighbor.vertex();
                int edgeIndex = neighbor.edgeIndex();
                trace.edgeStatus(edgeIndex, SnapshotStatus.ACTIVE);
                add(
                        LINE_EDGE,
                        "Inspect directed edge " + vertex + "→" + next,
                        StepEventType.EXECUTE_LINE,
                        List.of(vertex, next),
                        List.of(edgeIndex));
                if (index[next] == -1) {
                    add(
                            LINE_VISIT,
                            "Vertex " + next + " is unvisited; recurse into it",
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                    add(
                            LINE_RECURSE,
                            "Recurse into vertex " + next + " with its own active stack frame",
                            StepEventType.EXECUTE_LINE,
                            List.copyOf(stack),
                            List.of(edgeIndex));
                    visit(next);
                    int previousLow = low[vertex];
                    low[vertex] = Math.min(previousLow, low[next]);
                    add(
                            LINE_LOW_CHILD,
                            "low[" + vertex + "] = min(" + previousLow + ", low[" + next + "]=" + low[next]
                                    + ") = " + low[vertex],
                            StepEventType.RETURN_FRAME,
                            List.copyOf(stack),
                            List.of(edgeIndex),
                            "tarjan-" + next);
                } else if (onStack[next]) {
                    add(
                            LINE_ON_STACK,
                            "Edge reaches active vertex " + next + "; use its discovery index",
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                    int previousLow = low[vertex];
                    low[vertex] = Math.min(previousLow, index[next]);
                    add(
                            LINE_LOW_INDEX,
                            "low[" + vertex + "] = min(" + previousLow + ", index[" + next + "]=" + index[next]
                                    + ") = " + low[vertex],
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                } else {
                    add(
                            LINE_ON_STACK,
                            "Vertex " + next + " has left the stack; ignore this finished component",
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                }
                trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
            }

            add(
                    LINE_ROOT,
                    "low[" + vertex + "]=" + low[vertex] + " = index[" + vertex + "]=" + index[vertex]
                            + "? " + (low[vertex] == index[vertex]
                                    ? "Yes: close this component" : "No: keep it on the Tarjan stack"),
                    StepEventType.EXECUTE_LINE,
                    List.of(vertex),
                    List.of());
            if (low[vertex] == index[vertex]) {
                List<Integer> members = new ArrayList<>();
                int member;
                do {
                    member = stack.removeLast();
                    onStack[member] = false;
                    component[member] = components.size();
                    members.add(member);
                    trace.nodeStatus(member, SnapshotStatus.DONE);
                    add(
                            LINE_POP,
                            "Pop " + member + " from the active stack",
                            StepEventType.EXECUTE_LINE,
                            List.copyOf(stack),
                            List.of());
                } while (member != vertex);
                components.add(List.copyOf(members));
                add(
                        LINE_COMPONENT,
                        "Component " + (components.size() - 1) + " = "
                                + StructuralGraphSupport.formatVertices(members),
                        StepEventType.EXECUTE_LINE,
                        members,
                        List.of());
            }
            add(
                    LINE_RETURN,
                    "Finish DFS frame for vertex " + vertex,
                    StepEventType.RETURN_FRAME,
                    stack.isEmpty() ? List.of() : List.copyOf(stack),
                    List.of(),
                    "tarjan-" + vertex);
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges) {
            add(line, narration, event, activeVertices, activeEdges, null);
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                String frameId) {
            labelNodes();
            trace.add(
                    line,
                    narration,
                    event,
                    activeVertices,
                    activeEdges,
                    facts(),
                    frameId);
        }

        private List<Fact> facts() {
            return List.of(
                    StructuralGraphSupport.fact(
                            "index", StructuralGraphSupport.formatIntArray(index, "?"), SnapshotStatus.ACTIVE),
                    StructuralGraphSupport.fact(
                            "low", StructuralGraphSupport.formatIntArray(low, "?"), SnapshotStatus.ACTIVE),
                    StructuralGraphSupport.fact(
                            "stack", StructuralGraphSupport.formatVertices(stack), SnapshotStatus.ACTIVE),
                    StructuralGraphSupport.fact(
                            "component", StructuralGraphSupport.formatIntArray(component, "?"),
                            SnapshotStatus.ACTIVE),
                    StructuralGraphSupport.fact(
                            "components", formatComponents(components), SnapshotStatus.ACTIVE));
        }

        private void labelNodes() {
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                String componentLabel = component[vertex] < 0 ? "?" : Integer.toString(component[vertex]);
                trace.nodeLabel(
                        vertex,
                        vertex + " | i=" + (index[vertex] < 0 ? "?" : index[vertex])
                                + " low=" + (low[vertex] < 0 ? "?" : low[vertex])
                                + " c=" + componentLabel);
            }
        }
    }

    private static String formatComponents(List<List<Integer>> components) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < components.size(); index++) {
            if (index > 0) {
                result.append(", ");
            }
            result.append(StructuralGraphSupport.formatVertices(components.get(index)));
        }
        return result.append(']').toString();
    }

    private static SimulationMetadata createMetadata() {
        return new SimulationMetadata(
                TYPE,
                "Tarjan's Strongly Connected Components",
                "O(V + E)",
                "O(V)",
                RendererFamily.GRAPH,
                StructuralGraphSupport.pairDefault(
                        5,
                        new int[][] {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 3}},
                        true),
                "Input is {n, edges, directed}. Use n in 1..10, at most 24 integer "
                        + "pairs, and directed=true. Duplicate pairs are retained as "
                        + "separate adjacency entries; self-loops remain in their vertex's "
                        + "SCC. The trace exposes discovery index, low-link, active stack, "
                        + "and every component pop.",
                PSEUDOCODE);
    }
}
