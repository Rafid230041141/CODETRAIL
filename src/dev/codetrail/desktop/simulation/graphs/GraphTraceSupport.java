package dev.codetrail.desktop.simulation.graphs;

import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.GraphState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Small immutable-state and adjacency helpers shared by BFS and DFS. */
public final class GraphTraceSupport {
    public static final int MAX_TRACE_STEPS = 2048;
    public static final String ROOT_PARENT = "ROOT";
    public static final String UNSEEN_DISTANCE = "∞";

    private GraphTraceSupport() {
    }

    /** Return input-order adjacency arcs, adding the reverse arc only for undirected graphs. */
    public static List<List<Neighbor>> adjacency(GraphInput input) {
        Objects.requireNonNull(input, "input");
        List<List<Neighbor>> adjacency = new ArrayList<>(input.n());
        for (int vertex = 0; vertex < input.n(); vertex++) {
            adjacency.add(new ArrayList<>());
        }
        for (int edgeIndex = 0; edgeIndex < input.edges().size(); edgeIndex++) {
            GraphInput.Edge edge = input.edges().get(edgeIndex);
            adjacency.get(edge.from()).add(new Neighbor(edge.to(), edgeIndex));
            if (!input.directed() && edge.from() != edge.to()) {
                adjacency.get(edge.to()).add(new Neighbor(edge.from(), edgeIndex));
            }
        }
        return adjacency.stream().map(List::copyOf).toList();
    }

    public static String nodeId(int vertex) {
        return Integer.toString(vertex);
    }

    public static String edgeId(int edgeIndex) {
        return "edge-" + edgeIndex;
    }

    public static String formatVertices(Iterable<Integer> vertices) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Integer vertex : vertices) {
            result.add(Integer.toString(Objects.requireNonNull(vertex, "vertex")));
        }
        return result.toString();
    }

    public static String formatParentMap(int[] parents, boolean[] known) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < parents.length; vertex++) {
            if (known[vertex]) {
                result.add(vertex + "=" + (parents[vertex] < 0 ? ROOT_PARENT : parents[vertex]));
            }
        }
        return result.toString();
    }

    public static String formatDistanceMap(int[] distances, boolean[] known) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < distances.length; vertex++) {
            result.add(vertex + "=" + (known[vertex] ? Integer.toString(distances[vertex]) : UNSEEN_DISTANCE));
        }
        return result.toString();
    }

    public static String formatTimeMap(int[] times, boolean[] known) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < times.length; vertex++) {
            if (known[vertex]) {
                result.add(vertex + "=" + times[vertex]);
            }
        }
        return result.toString();
    }

    public static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    /** An adjacency arc retaining the original immutable edge index. */
    public record Neighbor(int vertex, int edgeIndex) {
        public Neighbor {
            if (vertex < 0 || edgeIndex < 0) {
                throw new IllegalArgumentException("graph adjacency indexes must be nonnegative");
            }
        }
    }

    /** Mutable producer facade that snapshots every graph operation immediately. */
    public static final class TraceBuilder {
        private final GraphInput input;
        private final List<Node> baseNodes;
        private final List<Edge> baseEdges;
        private final SnapshotStatus[] nodeStatuses;
        private final SnapshotStatus[] edgeStatuses;
        private final List<SimulationStep> steps = new ArrayList<>();

        public TraceBuilder(GraphInput input) {
            this.input = Objects.requireNonNull(input, "input");
            this.baseNodes = new ArrayList<>(input.n());
            for (int vertex = 0; vertex < input.n(); vertex++) {
                baseNodes.add(new Node(nodeId(vertex), nodeId(vertex), SnapshotStatus.DEFAULT));
            }
            this.baseEdges = new ArrayList<>(input.edges().size());
            for (int edgeIndex = 0; edgeIndex < input.edges().size(); edgeIndex++) {
                GraphInput.Edge edge = input.edges().get(edgeIndex);
                baseEdges.add(new Edge(
                        edgeId(edgeIndex),
                        nodeId(edge.from()),
                        nodeId(edge.to()),
                        SnapshotStatus.DEFAULT));
            }
            this.nodeStatuses = fill(input.n(), SnapshotStatus.DEFAULT);
            this.edgeStatuses = fill(input.edges().size(), SnapshotStatus.DEFAULT);
        }

        public GraphInput input() {
            return input;
        }

        public List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        public void nodeStatus(int vertex, SnapshotStatus status) {
            checkNode(vertex);
            nodeStatuses[vertex] = Objects.requireNonNull(status, "status");
        }

        public void edgeStatus(int edgeIndex, SnapshotStatus status) {
            checkEdge(edgeIndex);
            edgeStatuses[edgeIndex] = Objects.requireNonNull(status, "status");
        }

        public GraphState graphState(List<Fact> facts) {
            Objects.requireNonNull(facts, "facts");
            List<Node> nodes = new ArrayList<>(baseNodes.size());
            for (int vertex = 0; vertex < baseNodes.size(); vertex++) {
                Node base = baseNodes.get(vertex);
                nodes.add(new Node(base.id(), base.label(), nodeStatuses[vertex]));
            }
            List<Edge> edges = new ArrayList<>(baseEdges.size());
            for (int edgeIndex = 0; edgeIndex < baseEdges.size(); edgeIndex++) {
                Edge base = baseEdges.get(edgeIndex);
                edges.add(new Edge(
                        base.id(),
                        base.fromNodeId(),
                        base.toNodeId(),
                        edgeStatuses[edgeIndex],
                        base.labelOrNull()));
            }
            return new GraphState(nodes, edges, input.directed(), facts);
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                List<Fact> facts) {
            add(highlightedLine, narration, eventType, activeVertices, activeEdges, facts, null);
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                List<Fact> facts,
                String frameId) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("graph traversal trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            Objects.requireNonNull(narration, "narration");
            if (narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Set<String> activeNodeIds = idsForVertices(activeVertices, input.n(), false);
            Set<String> activeEdgeIds = idsForEdges(activeEdges, input.edges().size());
            SimulationSnapshot snapshot = new SimulationSnapshot(
                    graphState(Objects.requireNonNull(facts, "facts")),
                    activeNodeIds,
                    activeEdgeIds);
            steps.add(new SimulationStep(snapshot, highlightedLine, narration, eventType, frameId));
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                List<Fact> facts) {
            add(highlightedLine, narration, eventType, activeVertices, List.of(), facts);
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                List<Fact> facts,
                String frameId) {
            add(highlightedLine, narration, eventType, activeVertices, List.of(), facts, frameId);
        }

        private void checkNode(int vertex) {
            if (vertex < 0 || vertex >= input.n()) {
                throw new IllegalArgumentException("graph node index out of range: " + vertex);
            }
        }

        private void checkEdge(int edgeIndex) {
            if (edgeIndex < 0 || edgeIndex >= input.edges().size()) {
                throw new IllegalArgumentException("graph edge index out of range: " + edgeIndex);
            }
        }
    }

    private static SnapshotStatus[] fill(int size, SnapshotStatus status) {
        SnapshotStatus[] result = new SnapshotStatus[size];
        java.util.Arrays.fill(result, status);
        return result;
    }

    private static Set<String> idsForVertices(Iterable<Integer> vertices, int count, boolean allowNull) {
        Objects.requireNonNull(vertices, "activeVertices");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Integer vertex : vertices) {
            if (vertex == null) {
                if (allowNull) {
                    continue;
                }
                throw new IllegalArgumentException("active graph node cannot be null");
            }
            if (vertex < 0 || vertex >= count) {
                throw new IllegalArgumentException("active graph node index out of range: " + vertex);
            }
            ids.add(nodeId(vertex));
        }
        return Set.copyOf(ids);
    }

    private static Set<String> idsForEdges(Iterable<Integer> edges, int count) {
        Objects.requireNonNull(edges, "activeEdges");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Integer edge : edges) {
            if (edge == null || edge < 0 || edge >= count) {
                throw new IllegalArgumentException("active graph edge index out of range: " + edge);
            }
            ids.add(edgeId(edge));
        }
        return Set.copyOf(ids);
    }
}
