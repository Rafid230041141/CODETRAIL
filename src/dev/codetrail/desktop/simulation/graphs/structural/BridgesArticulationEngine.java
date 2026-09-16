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

/** Undirected low-link DFS for bridges and articulation points. */
public final class BridgesArticulationEngine implements SimulationEngine {
    public static final String TYPE = "BRIDGES_ARTICULATION";
    public static final int MIN_N = StructuralGraphSupport.MIN_N;
    public static final int MAX_N = StructuralGraphSupport.MAX_N;
    public static final int MAX_EDGES = StructuralGraphSupport.MAX_EDGES;
    public static final int MAX_TRACE_STEPS = StructuralGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_COMPONENT = 2;
    private static final int LINE_DISCOVER = 3;
    private static final int LINE_CHILDREN = 4;
    private static final int LINE_EDGE = 5;
    private static final int LINE_SKIP_PARENT = 6;
    private static final int LINE_TREE = 7;
    private static final int LINE_VISIT = 8;
    private static final int LINE_LOW_CHILD = 9;
    private static final int LINE_BRIDGE = 10;
    private static final int LINE_ARTICULATION = 11;
    private static final int LINE_BACK = 12;
    private static final int LINE_ROOT = 13;
    private static final int LINE_RETURN = 14;

    private static final List<String> PSEUDOCODE = List.of(
            "criticalConnections(G):",
            "    for each unvisited component root r:",
            "        discovery[u] = low[u] = time++",
            "        children = 0",
            "        for each incident edge (u, v) with id:",
            "            if id is the exact parent edge: continue",
            "            if discovery[v] is undefined:",
            "                children++; visit(v)",
            "                low[u] = min(low[u], low[v])",
            "                if low[v] > discovery[u]: mark bridge",
            "                if u is not a root and low[v] >= discovery[u]: mark articulation",
            "            else: low[u] = min(low[u], discovery[v])",
            "        root is articulation only when children > 1",
            "    return bridges and articulation points");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        StructuralGraphSupport.GraphInput graph =
                StructuralGraphSupport.parsePairGraph(input, false, true);
        List<List<StructuralGraphSupport.Neighbor>> adjacency =
                StructuralGraphSupport.adjacency(graph);
        List<String> labels = new ArrayList<>(graph.edges().size());
        for (StructuralGraphSupport.InputEdge edge : graph.edges()) {
            labels.add(edge.from() + "—" + edge.to());
        }
        StructuralGraphSupport.TraceBuilder trace =
                new StructuralGraphSupport.TraceBuilder(graph, labels);
        BridgesRun run = new BridgesRun(graph, adjacency, trace);
        run.add(
                0,
                "Initialize bridge and articulation analysis on " + graph.n()
                        + " undirected vertices",
                StepEventType.INITIALIZE,
                List.of(),
                List.of());
        run.add(
                LINE_METHOD,
                "Run low-link DFS from source " + graph.source()
                        + " and then every remaining component",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of());

        for (int vertex : run.rootOrder()) {
            if (run.discovery[vertex] == -1) {
                run.componentCount++;
                run.add(
                        LINE_COMPONENT,
                        "Start connected component " + run.componentCount + " at root " + vertex,
                        StepEventType.EXECUTE_LINE,
                        List.of(vertex),
                        List.of());
                run.visit(vertex, -1);
            }
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.nodeStatus(
                    vertex,
                    run.articulation[vertex] ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            trace.edgeStatus(
                    edgeIndex,
                    run.bridge[edgeIndex] ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        run.add(
                LINE_RETURN,
                "Return " + run.bridgeCount() + " bridges and "
                        + run.articulationCount() + " articulation points",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                true);
        run.add(
                0,
                "Complete: analyzed " + run.componentCount + " connected components",
                StepEventType.COMPLETE,
                List.of(),
                List.of(),
                true);
        return trace.steps();
    }

    private static final class BridgesRun {
        private final StructuralGraphSupport.GraphInput graph;
        private final List<List<StructuralGraphSupport.Neighbor>> adjacency;
        private final StructuralGraphSupport.TraceBuilder trace;
        private final int[] discovery;
        private final int[] low;
        private final int[] parent;
        private final boolean[] articulation;
        private final boolean[] bridge;
        private final Deque<Integer> dfsStack = new ArrayDeque<>();
        private int time;
        private int componentCount;

        private BridgesRun(
                StructuralGraphSupport.GraphInput graph,
                List<List<StructuralGraphSupport.Neighbor>> adjacency,
                StructuralGraphSupport.TraceBuilder trace) {
            this.graph = graph;
            this.adjacency = adjacency;
            this.trace = trace;
            discovery = new int[graph.n()];
            low = new int[graph.n()];
            parent = new int[graph.n()];
            articulation = new boolean[graph.n()];
            bridge = new boolean[graph.edges().size()];
            java.util.Arrays.fill(discovery, -1);
            java.util.Arrays.fill(low, -1);
            java.util.Arrays.fill(parent, -1);
        }

        private List<Integer> rootOrder() {
            List<Integer> roots = new ArrayList<>(graph.n());
            roots.add(graph.source());
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                if (vertex != graph.source()) {
                    roots.add(vertex);
                }
            }
            return roots;
        }

        private void visit(int vertex, int parentEdge) {
            discovery[vertex] = low[vertex] = time++;
            dfsStack.addLast(vertex);
            trace.nodeStatus(vertex, SnapshotStatus.ACTIVE);
            add(
                    LINE_DISCOVER,
                    "Discover vertex " + vertex + "; discovery=" + discovery[vertex]
                            + ", low=" + low[vertex],
                    StepEventType.EXECUTE_LINE,
                    List.of(vertex),
                    List.of());
            add(
                    LINE_CHILDREN,
                    "Set DFS child count for vertex " + vertex + " to zero",
                    StepEventType.PUSH_FRAME,
                    List.copyOf(dfsStack),
                    List.of(),
                    "bridges-" + vertex);

            int children = 0;
            for (StructuralGraphSupport.Neighbor neighbor : adjacency.get(vertex)) {
                int next = neighbor.vertex();
                int edgeIndex = neighbor.edgeIndex();
                trace.edgeStatus(edgeIndex, SnapshotStatus.ACTIVE);
                add(
                        LINE_EDGE,
                        "Inspect undirected edge " + vertex + "—" + next
                                + " (id " + edgeIndex + ")",
                        StepEventType.EXECUTE_LINE,
                        List.of(vertex, next),
                        List.of(edgeIndex));
                if (edgeIndex == parentEdge) {
                    add(
                            LINE_SKIP_PARENT,
                            "Skip only the exact parent edge id " + edgeIndex,
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                } else if (discovery[next] == -1) {
                    children++;
                    parent[next] = vertex;
                    add(
                            LINE_TREE,
                            "Edge " + vertex + "—" + next + " discovers a DFS child",
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                    add(
                            LINE_VISIT,
                            "Recurse to child " + next,
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                    visit(next, edgeIndex);
                    int previousLow = low[vertex];
                    low[vertex] = Math.min(previousLow, low[next]);
                    add(
                            LINE_LOW_CHILD,
                            "low[" + vertex + "] = min(" + previousLow + ", low[" + next + "]=" + low[next]
                                    + ") = " + low[vertex],
                            StepEventType.RETURN_FRAME,
                            List.copyOf(dfsStack),
                            List.of(edgeIndex),
                            "bridges-" + next);
                    boolean isBridge = low[next] > discovery[vertex];
                    bridge[edgeIndex] = isBridge;
                    add(
                            LINE_BRIDGE,
                            "low[" + next + "]=" + low[next] + " > discovery[" + vertex + "]="
                                    + discovery[vertex] + "? " + (isBridge
                                            ? "Yes: edge " + vertex + "—" + next + " is a bridge"
                                            : "No: an alternate route reaches this ancestor"),
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                    boolean separatesChild = parentEdge != -1 && low[next] >= discovery[vertex];
                    if (separatesChild) articulation[vertex] = true;
                    add(
                            LINE_ARTICULATION,
                            parentEdge == -1
                                    ? "Vertex " + vertex + " is a root: use its DFS child count, not this inequality"
                                    : "low[" + next + "]=" + low[next] + " ≥ discovery[" + vertex + "]="
                                            + discovery[vertex] + "? " + (separatesChild
                                                    ? "Yes: " + vertex + " separates this child subtree"
                                                    : "No: this child has a route above " + vertex),
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                } else {
                    int previousLow = low[vertex];
                    low[vertex] = Math.min(previousLow, discovery[next]);
                    add(
                            LINE_BACK,
                            "low[" + vertex + "] = min(" + previousLow + ", discovery[" + next + "]="
                                    + discovery[next] + ") = " + low[vertex],
                            StepEventType.EXECUTE_LINE,
                            List.of(vertex, next),
                            List.of(edgeIndex));
                }
                trace.edgeStatus(edgeIndex, SnapshotStatus.DONE);
            }

            if (parentEdge == -1 && children > 1) {
                articulation[vertex] = true;
            }
            add(
                    LINE_ROOT,
                    parentEdge == -1
                            ? "Root " + vertex + " has " + children
                                    + " DFS children; articulation=" + articulation[vertex]
                            : "Non-root " + vertex + " has " + children + " DFS children",
                    StepEventType.EXECUTE_LINE,
                    List.of(vertex),
                    List.of());
            dfsStack.removeLast();
            trace.nodeStatus(vertex, SnapshotStatus.DONE);
            add(
                    LINE_RETURN,
                    "Finish DFS frame for vertex " + vertex,
                    StepEventType.RETURN_FRAME,
                    List.copyOf(dfsStack),
                    List.of(),
                    "bridges-" + vertex);
        }

        private int bridgeCount() {
            int count = 0;
            for (boolean value : bridge) {
                if (value) {
                    count++;
                }
            }
            return count;
        }

        private int articulationCount() {
            int count = 0;
            for (boolean value : articulation) {
                if (value) {
                    count++;
                }
            }
            return count;
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges) {
            add(line, narration, event, activeVertices, activeEdges, false, null);
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                String frameId) {
            add(line, narration, event, activeVertices, activeEdges, false, frameId);
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                boolean terminal) {
            add(line, narration, event, activeVertices, activeEdges, terminal, null);
        }

        private void add(
                int line,
                String narration,
                StepEventType event,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                boolean terminal,
                String frameId) {
            labelNodes();
            trace.add(
                    line,
                    narration,
                    event,
                    activeVertices,
                    activeEdges,
                    facts(terminal),
                    frameId);
        }

        private List<Fact> facts(boolean terminal) {
            SnapshotStatus status = terminal ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
            return List.of(
                    StructuralGraphSupport.fact(
                            "discovery", StructuralGraphSupport.formatIntArray(discovery, "?"), status),
                    StructuralGraphSupport.fact(
                            "low", StructuralGraphSupport.formatIntArray(low, "?"), status),
                    StructuralGraphSupport.fact(
                            "parent", StructuralGraphSupport.formatIntArray(parent, "NIL"), status),
                    StructuralGraphSupport.fact("dfsStack", StructuralGraphSupport.formatVertices(dfsStack), status),
                    StructuralGraphSupport.fact("bridges", formatBridges(), status),
                    StructuralGraphSupport.fact("articulation", formatArticulation(), status),
                    StructuralGraphSupport.fact("components", Integer.toString(componentCount), status));
        }

        private String formatBridges() {
            List<int[]> pairs = new ArrayList<>();
            for (int edgeIndex = 0; edgeIndex < bridge.length; edgeIndex++) {
                if (bridge[edgeIndex]) {
                    StructuralGraphSupport.InputEdge edge = graph.edges().get(edgeIndex);
                    pairs.add(new int[] {Math.min(edge.from(), edge.to()), Math.max(edge.from(), edge.to())});
                }
            }
            return StructuralGraphSupport.formatPairList(pairs, false);
        }

        private String formatArticulation() {
            List<Integer> points = new ArrayList<>();
            for (int vertex = 0; vertex < articulation.length; vertex++) {
                if (articulation[vertex]) {
                    points.add(vertex);
                }
            }
            return StructuralGraphSupport.formatVertices(points);
        }

        private void labelNodes() {
            for (int vertex = 0; vertex < graph.n(); vertex++) {
                trace.nodeLabel(
                        vertex,
                        vertex + " | d=" + (discovery[vertex] < 0 ? "?" : discovery[vertex])
                                + " low=" + (low[vertex] < 0 ? "?" : low[vertex]));
            }
        }
    }

    private static SimulationMetadata createMetadata() {
        return new SimulationMetadata(
                TYPE,
                "Bridges and Articulation Points",
                "O(V + E)",
                "O(V + E)",
                RendererFamily.GRAPH,
                StructuralGraphSupport.pairDefault(
                        6,
                        new int[][] {{0, 1}, {1, 2}, {2, 0}, {2, 3}, {3, 4}, {4, 5}},
                        0,
                        false),
                "Input is {n, edges, source, directed}. Use n in 1..10, at most 24 "
                        + "integer pairs, source in range, and directed=false. DFS starts "
                        + "at source then covers every disconnected component. Duplicate "
                        + "pairs keep distinct edge IDs, so parallel edges are not false "
                        + "bridges; self-loops are never bridges. The root articulation "
                        + "rule requires more than one DFS child.",
                PSEUDOCODE);
    }
}
