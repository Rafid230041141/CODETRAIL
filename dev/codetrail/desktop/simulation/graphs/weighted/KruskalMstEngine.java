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
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/** Kruskal trace with deterministic edge sorting, DSU rejection, and forests. */
public final class KruskalMstEngine implements SimulationEngine {
    public static final String TYPE = "KRUSKAL_MST";
    public static final int MIN_N = WeightedGraphSupport.MIN_N;
    public static final int MAX_N = WeightedGraphSupport.MAX_N;
    public static final int MAX_EDGES = WeightedGraphSupport.MAX_EDGES;
    public static final long MAX_ABS_WEIGHT = WeightedGraphSupport.MAX_ABS_WEIGHT;
    public static final int MAX_TRACE_STEPS = WeightedGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_SORT = 2;
    private static final int LINE_MAKE_SET = 3;
    private static final int LINE_INITIALIZE = 4;
    private static final int LINE_SCAN_EDGE = 5;
    private static final int LINE_CHECK_DSU = 6;
    private static final int LINE_REJECT = 7;
    private static final int LINE_UNION = 9;
    private static final int LINE_ADD = 10;
    private static final int LINE_RETURN = 11;

    private static final List<String> PSEUDOCODE = List.of(
            "kruskalMst(G):",
            "    sort edges by (weight, endpoint IDs, input index)",
            "    make-set(v) for every vertex v",
            "    total = 0; components = |V|",
            "    for each edge (u, v, weight) in sorted order:",
            "        if find(u) == find(v):",
            "            reject edge; it would close a cycle",
            "        else:",
            "            union(u, v)",
            "            add edge; total += weight; components--",
            "    return the spanning tree or spanning forest");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        WeightedGraphSupport.Input graph = WeightedGraphSupport.parse(
                input, TYPE, false, true, true);
        int[] parent = new int[graph.n()];
        int[] size = new int[graph.n()];
        Arrays.fill(parent, -1);
        boolean[] selected = new boolean[graph.edges().size()];
        int components = graph.n();
        long totalWeight = 0L;
        List<Integer> order = new ArrayList<>(graph.edges().size());
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            order.add(edgeIndex);
        }
        WeightedGraphSupport.GraphTraceBuilder trace = new WeightedGraphSupport.GraphTraceBuilder(graph);
        int currentEdge = -1;
        String phase = "initialize";

        trace.add(
                0,
                "Initialize Kruskal's minimum spanning forest on " + graph.n() + " vertices",
                StepEventType.INITIALIZE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
        trace.add(
                LINE_METHOD,
                "Run Kruskal on an undirected graph with a disjoint-set union structure",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
        order.sort(edgeComparator(graph));
        trace.add(
                LINE_SORT,
                "Sort edges by weight, then canonical endpoints, then input index",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            parent[vertex] = vertex;
            size[vertex] = 1;
        }
        phase = "scan";
        trace.add(
                LINE_MAKE_SET,
                "Make one DSU set for each vertex",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
        trace.add(
                LINE_INITIALIZE,
                "Set total weight = 0 and components = " + components,
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));

        for (int sortedPosition = 0; sortedPosition < order.size(); sortedPosition++) {
            currentEdge = order.get(sortedPosition);
            WeightedGraphSupport.WeightedEdge edge = graph.edges().get(currentEdge);
            trace.edgeStatus(currentEdge, SnapshotStatus.ACTIVE);
            trace.add(
                    LINE_SCAN_EDGE,
                    "Inspect sorted edge " + currentEdge + ": " + edge.from() + " - " + edge.to()
                            + " with weight " + edge.weight(),
                    StepEventType.EXECUTE_LINE,
                    List.of(edge.from(), edge.to()),
                    List.of(currentEdge),
                    facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
            int firstRoot = find(parent, edge.from());
            int secondRoot = find(parent, edge.to());
            trace.add(
                    LINE_CHECK_DSU,
                    "find(" + edge.from() + ")=" + firstRoot + " = find(" + edge.to() + ")=" + secondRoot
                            + "? " + (firstRoot == secondRoot ? "Yes: this edge closes a cycle" : "No: join the components"),
                    StepEventType.EXECUTE_LINE,
                    List.of(edge.from(), edge.to()),
                    List.of(currentEdge),
                    facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
            if (firstRoot == secondRoot) {
                trace.edgeStatus(currentEdge, SnapshotStatus.REJECTED);
                trace.add(
                        LINE_REJECT,
                        "Reject edge " + currentEdge + "; DSU says both endpoints are already connected",
                        StepEventType.EXECUTE_LINE,
                        List.of(edge.from(), edge.to()),
                        List.of(currentEdge),
                        facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
            } else {
                union(parent, size, firstRoot, secondRoot);
                trace.add(
                        LINE_UNION,
                        "Union the two DSU components through edge " + currentEdge,
                        StepEventType.EXECUTE_LINE,
                        List.of(edge.from(), edge.to()),
                        List.of(currentEdge),
                        facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
                selected[currentEdge] = true;
                totalWeight += edge.weight();
                components--;
                trace.edgeStatus(currentEdge, SnapshotStatus.DONE);
                trace.nodeStatus(edge.from(), SnapshotStatus.DONE);
                trace.nodeStatus(edge.to(), SnapshotStatus.DONE);
                trace.add(
                        LINE_ADD,
                        "Add edge " + currentEdge + "; total weight = " + totalWeight
                                + ", components = " + components,
                        StepEventType.EXECUTE_LINE,
                        List.of(edge.from(), edge.to()),
                        List.of(currentEdge),
                        facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, false));
            }
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.nodeStatus(vertex, SnapshotStatus.DONE);
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            if (!selected[edgeIndex]) {
                trace.edgeStatus(edgeIndex, SnapshotStatus.REJECTED);
            }
        }
        currentEdge = -1;
        phase = "return";
        trace.add(
                LINE_RETURN,
                components == 1
                        ? "Return the minimum spanning tree"
                        : "Return a minimum spanning forest with " + components + " components",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, true));
        phase = "complete";
        trace.add(
                0,
                "Complete: selected " + countSelected(selected) + " edge(s), total weight " + totalWeight
                        + ", components = " + components,
                StepEventType.COMPLETE,
                List.of(),
                List.of(),
                facts(graph, order, parent, selected, components, totalWeight, currentEdge, phase, true));
        return trace.steps();
    }

    private static Comparator<Integer> edgeComparator(WeightedGraphSupport.Input graph) {
        return Comparator
                .comparingLong((Integer index) -> graph.edges().get(index).weight())
                .thenComparingInt(index -> canonicalFrom(graph.edges().get(index)))
                .thenComparingInt(index -> canonicalTo(graph.edges().get(index)))
                .thenComparingInt(Integer::intValue);
    }

    private static int canonicalFrom(WeightedGraphSupport.WeightedEdge edge) {
        return Math.min(edge.from(), edge.to());
    }

    private static int canonicalTo(WeightedGraphSupport.WeightedEdge edge) {
        return Math.max(edge.from(), edge.to());
    }

    private static List<Fact> facts(
            WeightedGraphSupport.Input graph,
            List<Integer> order,
            int[] parent,
            boolean[] selected,
            int components,
            long totalWeight,
            int currentEdge,
            String phase,
            boolean complete) {
        SnapshotStatus resultStatus = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        return List.of(
                WeightedGraphSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("sorted-edges", formatOrder(graph, order),
                        SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("current-edge", currentEdge < 0 ? "NIL" : Integer.toString(currentEdge),
                        currentEdge < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("selected-edges", WeightedGraphSupport.formatSelectedEdges(
                        graph.edges(), selected), resultStatus),
                WeightedGraphSupport.fact("total-weight", Long.toString(totalWeight), resultStatus),
                WeightedGraphSupport.fact("components", Integer.toString(components), resultStatus),
                WeightedGraphSupport.fact("spanning-forest", components == 1 ? "tree" : "forest", resultStatus),
                WeightedGraphSupport.fact("connected", Boolean.toString(components == 1), resultStatus),
                WeightedGraphSupport.fact("parent", formatParents(parent), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("component-roots", formatRoots(parent), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact(
                        "invariant",
                        "Every accepted edge joins different DSU components; every rejected edge closes a cycle",
                        resultStatus),
                WeightedGraphSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static String formatOrder(WeightedGraphSupport.Input graph, List<Integer> order) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (int index : order) {
            result.add(index + ":" + graph.edges().get(index).weight());
        }
        return result.toString();
    }

    private static String formatRoots(int[] parent) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < parent.length; vertex++) {
            int root = vertex;
            while (parent[root] >= 0 && parent[root] != root) root = parent[root];
            result.add(vertex + "=" + (parent[root] < 0 ? "?" : Integer.toString(root)));
        }
        return result.toString();
    }

    private static String formatParents(int[] parent) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < parent.length; vertex++) {
            result.add(vertex + "=" + parent[vertex]);
        }
        return result.toString();
    }

    private static int find(int[] parent, int vertex) {
        int root = vertex;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[vertex] != vertex) {
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    private static void union(int[] parent, int[] size, int firstRoot, int secondRoot) {
        int rootA = find(parent, firstRoot);
        int rootB = find(parent, secondRoot);
        if (rootA == rootB) {
            return;
        }
        if (size[rootA] < size[rootB]
                || (size[rootA] == size[rootB] && rootA > rootB)) {
            int temporary = rootA;
            rootA = rootB;
            rootB = temporary;
        }
        parent[rootB] = rootA;
        size[rootA] += size[rootB];
    }

    private static int countSelected(boolean[] selected) {
        int count = 0;
        for (boolean value : selected) {
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
                        new WeightedGraphSupport.WeightedEdge(0, 1, 1),
                        new WeightedGraphSupport.WeightedEdge(0, 2, 4),
                        new WeightedGraphSupport.WeightedEdge(1, 2, 2),
                        new WeightedGraphSupport.WeightedEdge(1, 3, 5),
                        new WeightedGraphSupport.WeightedEdge(2, 3, 3),
                        new WeightedGraphSupport.WeightedEdge(3, 4, 2)),
                null,
                false);
        return new SimulationMetadata(
                TYPE,
                "Kruskal's Minimum Spanning Tree",
                "O(E log E)",
                "O(V)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"n\":5,\"edges\":[{\"from\":0,\"to\":1,\"weight\":1}],\"directed\":false}; n must be "
                        + MIN_N + ".." + MAX_N + ", there may be at most " + MAX_EDGES
                        + " unique logical edges, weights are integers from -" + MAX_ABS_WEIGHT + " through "
                        + MAX_ABS_WEIGHT + ", and directed=true is rejected because an MST requires an undirected graph.",
                PSEUDOCODE);
    }
}
