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
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.StringJoiner;

/** Prim trace with a deterministic frontier and spanning-forest continuation. */
public final class PrimMstEngine implements SimulationEngine {
    public static final String TYPE = "PRIM_MST";
    public static final int MIN_N = WeightedGraphSupport.MIN_N;
    public static final int MAX_N = WeightedGraphSupport.MAX_N;
    public static final int MAX_EDGES = WeightedGraphSupport.MAX_EDGES;
    public static final long MAX_ABS_WEIGHT = WeightedGraphSupport.MAX_ABS_WEIGHT;
    public static final int MAX_TRACE_STEPS = WeightedGraphSupport.MAX_TRACE_STEPS;

    private static final int LINE_METHOD = 1;
    private static final int LINE_INITIALIZE = 2;
    private static final int LINE_TOTAL = 3;
    private static final int LINE_COMPONENT = 4;
    private static final int LINE_ROOT = 5;
    private static final int LINE_MARK_ROOT = 6;
    private static final int LINE_PUSH_ROOT = 7;
    private static final int LINE_FRONTIER = 8;
    private static final int LINE_POP = 9;
    private static final int LINE_CHECK = 10;
    private static final int LINE_REJECT = 11;
    private static final int LINE_ACCEPT = 13;
    private static final int LINE_ADD = 14;
    private static final int LINE_SCAN = 15;
    private static final int LINE_PUSH = 16;
    private static final int LINE_RETURN = 17;

    private static final List<String> PSEUDOCODE = List.of(
            "primForest(G, source):",
            "    for each vertex v: inForest[v] = false",
            "    total = 0; components = 0",
            "    while some vertex is not inForest:",
            "        root = source for the first component, otherwise the smallest unvisited vertex",
            "        inForest[root] = true; components++",
            "        for each edge (root, v, weight): frontier.push(edge)",
            "        while frontier is not empty:",
            "            (u, v, weight) = frontier.popMin()",
            "            if inForest[v]:",
            "                reject edge",
            "            else:",
            "                inForest[v] = true",
            "                add edge; total += weight",
            "                for each edge (v, x, weight):",
            "                    frontier.push(edge)",
            "    return the spanning tree or spanning forest");

    private static final SimulationMetadata METADATA = createMetadata();

    @Override
    public SimulationMetadata metadata() {
        return METADATA;
    }

    @Override
    public List<SimulationStep> generateSteps(JsonNode input) {
        WeightedGraphSupport.Input graph = WeightedGraphSupport.parse(
                input, TYPE, true, true, true);
        List<List<WeightedGraphSupport.Arc>> adjacency = WeightedGraphSupport.adjacency(graph);
        boolean[] inForest = new boolean[graph.n()];
        int[] parent = new int[graph.n()];
        java.util.Arrays.fill(parent, -1);
        boolean[] selected = new boolean[graph.edges().size()];
        PriorityQueue<Candidate> frontier = new PriorityQueue<>(Candidate.ORDER);
        WeightedGraphSupport.GraphTraceBuilder trace = new WeightedGraphSupport.GraphTraceBuilder(graph);
        long totalWeight = 0L;
        int components = 0;
        int currentRoot = -1;
        int currentEdge = -1;
        String phase = "initialize";

        trace.add(
                0,
                "Initialize Prim's spanning-forest search on " + graph.n() + " vertices from source "
                        + graph.source(),
                StepEventType.INITIALIZE,
                List.of(graph.source()),
                List.of(),
                facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                        currentRoot, currentEdge, phase, false));
        trace.add(
                LINE_METHOD,
                "Run Prim with a minimum frontier; restart at the smallest unvisited root after a disconnect",
                StepEventType.EXECUTE_LINE,
                List.of(graph.source()),
                List.of(),
                facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                        currentRoot, currentEdge, phase, false));
        trace.add(
                LINE_INITIALIZE,
                "Mark every vertex outside the forest initially",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                        currentRoot, currentEdge, phase, false));
        trace.add(
                LINE_TOTAL,
                "Set total weight = 0 and components = 0",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                        currentRoot, currentEdge, phase, false));

        boolean firstComponent = true;
        phase = "frontier";
        while (hasUnvisited(inForest)) {
            trace.add(
                    LINE_COMPONENT,
                    "An unvisited vertex remains; start or continue a spanning-forest component",
                    StepEventType.EXECUTE_LINE,
                    List.of(),
                    List.of(),
                    facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                            currentRoot, currentEdge, phase, false));
            currentRoot = firstComponent ? graph.source() : smallestUnvisited(inForest);
            firstComponent = false;
            trace.nodeStatus(currentRoot, SnapshotStatus.ACTIVE);
            trace.add(
                    LINE_ROOT,
                    "Choose root " + currentRoot + (components == 0 ? " as the source component" : " for a new disconnected component"),
                    StepEventType.EXECUTE_LINE,
                    List.of(currentRoot),
                    List.of(),
                    facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                            currentRoot, currentEdge, phase, false));
            inForest[currentRoot] = true;
            parent[currentRoot] = -1;
            components++;
            trace.nodeStatus(currentRoot, SnapshotStatus.DONE);
            trace.add(
                    LINE_MARK_ROOT,
                    "Add root " + currentRoot + " to the forest; components = " + components,
                    StepEventType.EXECUTE_LINE,
                    List.of(currentRoot),
                    List.of(),
                    facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                            currentRoot, currentEdge, phase, false));
            for (WeightedGraphSupport.Arc arc : adjacency.get(currentRoot)) {
                frontier.add(new Candidate(arc.from(), arc.to(), arc.weight(), arc.edgeIndex()));
                trace.edgeStatus(arc.edgeIndex(), SnapshotStatus.ACTIVE);
                trace.add(
                        LINE_PUSH_ROOT,
                        "Add edge " + arc.edgeIndex() + " to the frontier from root " + currentRoot,
                        StepEventType.EXECUTE_LINE,
                        List.of(currentRoot, arc.to()),
                        List.of(arc.edgeIndex()),
                        facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                currentRoot, currentEdge, phase, false));
            }

            while (!frontier.isEmpty()) {
                trace.add(
                        LINE_FRONTIER,
                        "Frontier is not empty; next candidates = " + formatFrontier(frontier),
                        StepEventType.EXECUTE_LINE,
                        List.of(currentRoot),
                        List.of(),
                        facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                currentRoot, currentEdge, phase, false));
                Candidate candidate = frontier.remove();
                currentEdge = candidate.edgeIndex();
                trace.edgeStatus(currentEdge, SnapshotStatus.ACTIVE);
                trace.add(
                        LINE_POP,
                        "Pop frontier edge " + currentEdge + ": " + candidate.from() + " - " + candidate.to()
                                + " with weight " + candidate.weight(),
                        StepEventType.EXECUTE_LINE,
                        List.of(candidate.from(), candidate.to()),
                        List.of(currentEdge),
                        facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                currentRoot, currentEdge, phase, false));
                trace.add(
                        LINE_CHECK,
                        "Check whether vertex " + candidate.to() + " is already in the forest",
                        StepEventType.EXECUTE_LINE,
                        List.of(candidate.from(), candidate.to()),
                        List.of(currentEdge),
                        facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                currentRoot, currentEdge, phase, false));
                if (inForest[candidate.to()]) {
                    trace.edgeStatus(currentEdge, selected[currentEdge]
                            ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
                    trace.add(
                            LINE_REJECT,
                            "Reject frontier edge " + currentEdge + "; its destination is already in the forest",
                            StepEventType.EXECUTE_LINE,
                            List.of(candidate.from(), candidate.to()),
                            List.of(currentEdge),
                            facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                    currentRoot, currentEdge, phase, false));
                    continue;
                }

                inForest[candidate.to()] = true;
                trace.nodeStatus(candidate.to(), SnapshotStatus.DONE);
                trace.add(
                        LINE_ACCEPT,
                        "Accept vertex " + candidate.to() + " through frontier edge " + currentEdge,
                        StepEventType.EXECUTE_LINE,
                        List.of(candidate.from(), candidate.to()),
                        List.of(currentEdge),
                        facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                currentRoot, currentEdge, phase, false));
                parent[candidate.to()] = candidate.from();
                selected[currentEdge] = true;
                trace.edgeStatus(currentEdge, SnapshotStatus.DONE);
                totalWeight += candidate.weight();
                trace.add(
                        LINE_ADD,
                        "Add edge " + currentEdge + "; total weight = " + totalWeight,
                        StepEventType.EXECUTE_LINE,
                        List.of(candidate.from(), candidate.to()),
                        List.of(currentEdge),
                        facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                currentRoot, currentEdge, phase, false));
                for (WeightedGraphSupport.Arc arc : adjacency.get(candidate.to())) {
                    trace.edgeStatus(arc.edgeIndex(), selected[arc.edgeIndex()]
                            ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE);
                    trace.add(
                            LINE_SCAN,
                            "Scan edge " + arc.edgeIndex() + " from newly added vertex " + candidate.to(),
                            StepEventType.EXECUTE_LINE,
                            List.of(candidate.to(), arc.to()),
                            List.of(arc.edgeIndex()),
                            facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                    currentRoot, currentEdge, phase, false));
                    frontier.add(new Candidate(arc.from(), arc.to(), arc.weight(), arc.edgeIndex()));
                    trace.add(
                            LINE_PUSH,
                            "Push edge " + arc.edgeIndex() + " onto the frontier",
                            StepEventType.EXECUTE_LINE,
                            List.of(candidate.to(), arc.to()),
                            List.of(arc.edgeIndex()),
                            facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                                    currentRoot, currentEdge, phase, false));
                }
            }
            currentEdge = -1;
        }

        for (int vertex = 0; vertex < graph.n(); vertex++) {
            trace.nodeStatus(vertex, SnapshotStatus.DONE);
        }
        for (int edgeIndex = 0; edgeIndex < graph.edges().size(); edgeIndex++) {
            trace.edgeStatus(edgeIndex, selected[edgeIndex]
                    ? SnapshotStatus.DONE : SnapshotStatus.REJECTED);
        }
        currentRoot = -1;
        phase = "return";
        trace.add(
                LINE_RETURN,
                components == 1
                        ? "Return the minimum spanning tree"
                        : "Return a minimum spanning forest with " + components + " components",
                StepEventType.EXECUTE_LINE,
                List.of(),
                List.of(),
                facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                        currentRoot, currentEdge, phase, true));
        phase = "complete";
        trace.add(
                0,
                "Complete: selected " + countSelected(selected) + " edge(s), total weight " + totalWeight
                        + ", components = " + components,
                StepEventType.COMPLETE,
                List.of(),
                List.of(),
                facts(graph, frontier, inForest, parent, selected, totalWeight, components,
                        currentRoot, currentEdge, phase, true));
        return trace.steps();
    }

    private static List<Fact> facts(
            WeightedGraphSupport.Input graph,
            PriorityQueue<Candidate> frontier,
            boolean[] inForest,
            int[] parent,
            boolean[] selected,
            long totalWeight,
            int components,
            int currentRoot,
            int currentEdge,
            String phase,
            boolean complete) {
        SnapshotStatus resultStatus = complete ? SnapshotStatus.DONE : SnapshotStatus.ACTIVE;
        return List.of(
                WeightedGraphSupport.fact("phase", phase, SnapshotStatus.DEFAULT),
                WeightedGraphSupport.fact("root", currentRoot < 0 ? "NIL" : Integer.toString(currentRoot),
                        currentRoot < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("current-edge", currentEdge < 0 ? "NIL" : Integer.toString(currentEdge),
                        currentEdge < 0 ? SnapshotStatus.DEFAULT : SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("frontier", formatFrontier(frontier), SnapshotStatus.ACTIVE),
                WeightedGraphSupport.fact("visited", formatVisited(inForest), resultStatus),
                WeightedGraphSupport.fact("parent", formatParents(parent, inForest), resultStatus),
                WeightedGraphSupport.fact("selected-edges", WeightedGraphSupport.formatSelectedEdges(
                        graph.edges(), selected), resultStatus),
                WeightedGraphSupport.fact("total-weight", Long.toString(totalWeight), resultStatus),
                WeightedGraphSupport.fact("components", Integer.toString(components), resultStatus),
                WeightedGraphSupport.fact("spanning-forest", components == 1 ? "tree" : "forest", resultStatus),
                WeightedGraphSupport.fact("connected", Boolean.toString(components == 1), resultStatus),
                WeightedGraphSupport.fact(
                        "invariant",
                        "The frontier is ordered by weight; discard stale edges whose destination has joined the forest",
                        resultStatus),
                WeightedGraphSupport.fact("directed", Boolean.toString(graph.directed()), SnapshotStatus.DEFAULT));
    }

    private static String formatFrontier(PriorityQueue<Candidate> frontier) {
        List<Candidate> entries = new ArrayList<>(frontier);
        entries.sort(Candidate.ORDER);
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Candidate candidate : entries) {
            result.add(candidate.edgeIndex() + ":" + candidate.from() + "-" + candidate.to()
                    + "(" + candidate.weight() + ")");
        }
        return result.toString();
    }

    private static String formatVisited(boolean[] inForest) {
        List<Integer> visited = new ArrayList<>();
        for (int vertex = 0; vertex < inForest.length; vertex++) {
            if (inForest[vertex]) {
                visited.add(vertex);
            }
        }
        return WeightedGraphSupport.formatVertices(visited);
    }

    private static String formatParents(int[] parent, boolean[] inForest) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < parent.length; vertex++) {
            String value = !inForest[vertex] ? "NIL" : parent[vertex] < 0 ? "ROOT" : Integer.toString(parent[vertex]);
            result.add(vertex + "=" + value);
        }
        return result.toString();
    }

    private static boolean hasUnvisited(boolean[] values) {
        for (boolean value : values) {
            if (!value) {
                return true;
            }
        }
        return false;
    }

    private static int smallestUnvisited(boolean[] values) {
        for (int vertex = 0; vertex < values.length; vertex++) {
            if (!values[vertex]) {
                return vertex;
            }
        }
        throw new IllegalStateException("there is no unvisited vertex");
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
                0,
                false);
        return new SimulationMetadata(
                TYPE,
                "Prim's Minimum Spanning Tree",
                "O(E log V)",
                "O(V + E)",
                RendererFamily.GRAPH,
                defaultInput,
                "Enter JSON as {\"n\":5,\"edges\":[{\"from\":0,\"to\":1,\"weight\":1}],\"source\":0,\"directed\":false}; n must be "
                        + MIN_N + ".." + MAX_N + ", there may be at most " + MAX_EDGES
                        + " unique logical edges, weights are integers from -" + MAX_ABS_WEIGHT + " through "
                        + MAX_ABS_WEIGHT + ", and directed=true is rejected because a spanning tree requires an undirected graph.",
                PSEUDOCODE);
    }

    private record Candidate(int from, int to, long weight, int edgeIndex) {
        private static final Comparator<Candidate> ORDER = Comparator
                .comparingLong(Candidate::weight)
                .thenComparingInt(candidate -> Math.min(candidate.from(), candidate.to()))
                .thenComparingInt(candidate -> Math.max(candidate.from(), candidate.to()))
                .thenComparingInt(Candidate::from)
                .thenComparingInt(Candidate::to)
                .thenComparingInt(Candidate::edgeIndex);
    }
}
