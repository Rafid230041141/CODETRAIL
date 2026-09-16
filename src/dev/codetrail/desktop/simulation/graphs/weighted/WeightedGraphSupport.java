package dev.codetrail.desktop.simulation.graphs.weighted;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.codetrail.desktop.simulation.Edge;
import dev.codetrail.desktop.simulation.Fact;
import dev.codetrail.desktop.simulation.GraphState;
import dev.codetrail.desktop.simulation.Node;
import dev.codetrail.desktop.simulation.SimulationSnapshot;
import dev.codetrail.desktop.simulation.SimulationStep;
import dev.codetrail.desktop.simulation.SnapshotStatus;
import dev.codetrail.desktop.simulation.StepEventType;
import dev.codetrail.desktop.simulation.TableState;
import dev.codetrail.desktop.simulation.TypedCell;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Package-local boundaries shared by the bounded weighted graph engines. */
public final class WeightedGraphSupport {
    public static final int MIN_N = 1;
    public static final int MAX_N = 10;
    public static final int MAX_EDGES = 24;
    public static final long MAX_ABS_WEIGHT = 999L;
    public static final long MAX_WEIGHT = MAX_ABS_WEIGHT;
    public static final long INF = Long.MAX_VALUE / 4L;
    public static final int MAX_TRACE_STEPS = 4096;

    private WeightedGraphSupport() {
    }

    /** Parse the public weighted graph shape and enforce the algorithm boundary. */
    public static Input parse(
            JsonNode input,
            String type,
            boolean requireSource,
            boolean allowNegativeWeights,
            boolean requireUndirected) {
        Objects.requireNonNull(type, "type");
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(type + " input must be a JSON object");
        }
        int n = readInt(input.get("n"), type + " n");
        JsonNode edgesNode = input.get("edges");
        if (edgesNode == null || !edgesNode.isArray()) {
            throw new IllegalArgumentException(type + " edges must be a JSON array");
        }
        if (edgesNode.size() > MAX_EDGES) {
            throw new IllegalArgumentException(type + " edge count must be at most " + MAX_EDGES);
        }

        JsonNode directedNode = input.get("directed");
        boolean directed = false;
        if (directedNode != null) {
            if (!directedNode.isBoolean()) {
                throw new IllegalArgumentException(type + " directed must be boolean when present");
            }
            directed = directedNode.booleanValue();
        }
        if (requireUndirected && directed) {
            throw new IllegalArgumentException(type + " requires directed=false for an undirected MST");
        }

        int source = -1;
        JsonNode sourceNode = input.get("source");
        if (sourceNode != null) {
            source = readInt(sourceNode, type + " source");
        } else if (requireSource) {
            throw new IllegalArgumentException(type + " source is required");
        }
        if (sourceNode != null && (source < 0 || source >= n)) {
            throw new IllegalArgumentException(type + " source must be in the range 0.." + (n - 1));
        }

        List<WeightedEdge> edges = new ArrayList<>(edgesNode.size());
        Set<EdgeKey> seen = new HashSet<>();
        for (int index = 0; index < edgesNode.size(); index++) {
            JsonNode edgeNode = edgesNode.get(index);
            if (edgeNode == null || !edgeNode.isObject()) {
                throw new IllegalArgumentException(
                        type + " edge " + index + " must be an object {from,to,weight}");
            }
            int from = readInt(edgeNode.get("from"), type + " edge " + index + " from");
            int to = readInt(edgeNode.get("to"), type + " edge " + index + " to");
            if (from < 0 || from >= n || to < 0 || to >= n) {
                throw new IllegalArgumentException(
                        type + " edge endpoints must be in the range 0.." + (n - 1));
            }
            JsonNode weightNode = edgeNode.get("weight");
            long weight = readLong(weightNode, type + " edge " + index + " weight");
            if (weight > MAX_ABS_WEIGHT || weight < -MAX_ABS_WEIGHT) {
                throw new IllegalArgumentException(
                        type + " edge weights must have absolute value at most " + MAX_ABS_WEIGHT);
            }
            if (!allowNegativeWeights && weight < 0L) {
                throw new IllegalArgumentException(type + " requires nonnegative edge weights");
            }
            EdgeKey key = EdgeKey.of(from, to, directed);
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        type + " duplicate logical edge: (" + from + "," + to + ")");
            }
            edges.add(new WeightedEdge(from, to, weight));
        }
        return new Input(n, edges, source, directed);
    }

    public static int readInt(JsonNode node, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return node.intValue();
    }

    public static long readLong(JsonNode node, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return node.longValue();
    }

    /** Build input-order adjacency arcs, adding reverse arcs only for undirected graphs. */
    public static List<List<Arc>> adjacency(Input input) {
        Objects.requireNonNull(input, "input");
        List<List<Arc>> adjacency = new ArrayList<>(input.n());
        for (int vertex = 0; vertex < input.n(); vertex++) {
            adjacency.add(new ArrayList<>());
        }
        for (int edgeIndex = 0; edgeIndex < input.edges().size(); edgeIndex++) {
            WeightedEdge edge = input.edges().get(edgeIndex);
            adjacency.get(edge.from()).add(new Arc(edge.from(), edge.to(), edge.weight(), edgeIndex));
            if (!input.directed() && edge.from() != edge.to()) {
                adjacency.get(edge.to()).add(new Arc(edge.to(), edge.from(), edge.weight(), edgeIndex));
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

    public static String formatVertices(Collection<Integer> vertices) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Integer vertex : vertices) {
            result.add(Integer.toString(Objects.requireNonNull(vertex, "vertex")));
        }
        return result.toString();
    }

    public static String formatDistance(long[] distances) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < distances.length; vertex++) {
            result.add(vertex + "=" + formatDistance(distances[vertex]));
        }
        return result.toString();
    }

    public static String formatDistance(long distance) {
        return distance >= INF ? "∞" : Long.toString(distance);
    }

    public static String formatParents(int[] parents, int source) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < parents.length; vertex++) {
            String value = parents[vertex] < 0
                    ? (vertex == source ? "ROOT" : "NIL")
                    : Integer.toString(parents[vertex]);
            result.add(vertex + "=" + value);
        }
        return result.toString();
    }

    public static String formatSelectedEdges(List<WeightedEdge> edges, boolean[] selected) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (int edgeIndex = 0; edgeIndex < selected.length; edgeIndex++) {
            if (selected[edgeIndex]) {
                WeightedEdge edge = edges.get(edgeIndex);
                result.add(edgeIndex + ":" + edge.from() + "-" + edge.to() + "(" + edge.weight() + ")");
            }
        }
        return result.toString();
    }

    public static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    public static TypedCell cell(String key, String value, SnapshotStatus status) {
        return new TypedCell(key, value, status);
    }

    public static List<List<TypedCell>> matrixRows(long[][] matrix, int currentK, int currentI, int currentJ, boolean complete) {
        int n = matrix.length;
        List<List<TypedCell>> rows = new ArrayList<>(n);
        for (int from = 0; from < n; from++) {
            List<TypedCell> row = new ArrayList<>(n + 1);
            SnapshotStatus rowStatus = currentI >= 0 && from == currentI
                    ? SnapshotStatus.ACTIVE
                    : complete ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
            row.add(cell("row-" + from, Integer.toString(from), rowStatus));
            for (int to = 0; to < n; to++) {
                SnapshotStatus status;
                if (currentI >= 0 && currentJ >= 0 && from == currentI && to == currentJ) {
                    status = SnapshotStatus.ACTIVE;
                } else if (currentK >= 0 && currentI >= 0 && currentJ >= 0
                        && ((from == currentI && to == currentK) || (from == currentK && to == currentJ))) {
                    status = SnapshotStatus.DONE;
                } else if (matrix[from][to] >= INF) {
                    status = complete ? SnapshotStatus.REJECTED : SnapshotStatus.DEFAULT;
                } else {
                    status = complete ? SnapshotStatus.DONE : SnapshotStatus.DEFAULT;
                }
                row.add(cell(
                        "distance-" + from + "-" + to,
                        formatDistance(matrix[from][to]),
                        status));
            }
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

    /** Mutable graph-state producer that copies every operation into a snapshot. */
    public static final class GraphTraceBuilder {
        private final Input input;
        private final List<Node> baseNodes;
        private final List<Edge> baseEdges;
        private final SnapshotStatus[] nodeStatuses;
        private final SnapshotStatus[] edgeStatuses;
        private final List<SimulationStep> steps = new ArrayList<>();

        public GraphTraceBuilder(Input input) {
            this.input = Objects.requireNonNull(input, "input");
            this.baseNodes = new ArrayList<>(input.n());
            for (int vertex = 0; vertex < input.n(); vertex++) {
                baseNodes.add(new Node(nodeId(vertex), nodeId(vertex), SnapshotStatus.DEFAULT));
            }
            this.baseEdges = new ArrayList<>(input.edges().size());
            for (int edgeIndex = 0; edgeIndex < input.edges().size(); edgeIndex++) {
                WeightedEdge edge = input.edges().get(edgeIndex);
                baseEdges.add(new Edge(
                        edgeId(edgeIndex),
                        nodeId(edge.from()),
                        nodeId(edge.to()),
                        SnapshotStatus.DEFAULT,
                        Long.toString(edge.weight())));
            }
            this.nodeStatuses = statuses(input.n(), SnapshotStatus.DEFAULT);
            this.edgeStatuses = statuses(input.edges().size(), SnapshotStatus.DEFAULT);
        }

        public void nodeStatus(int vertex, SnapshotStatus status) {
            checkNode(vertex);
            nodeStatuses[vertex] = Objects.requireNonNull(status, "status");
        }

        public void edgeStatus(int edgeIndex, SnapshotStatus status) {
            checkEdge(edgeIndex);
            edgeStatuses[edgeIndex] = Objects.requireNonNull(status, "status");
        }

        public void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                List<Fact> facts) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("weighted graph trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            Objects.requireNonNull(narration, "narration");
            if (narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            Set<String> activeNodeIds = idsForVertices(activeVertices, input.n());
            Set<String> activeEdgeIds = idsForEdges(activeEdges, input.edges().size());
            List<Node> nodes = new ArrayList<>(baseNodes.size());
            for (int vertex = 0; vertex < baseNodes.size(); vertex++) {
                Node base = baseNodes.get(vertex);
                nodes.add(new Node(base.id(), base.label(), nodeStatuses[vertex]));
            }
            List<Edge> edges = new ArrayList<>(baseEdges.size());
            for (int edgeIndex = 0; edgeIndex < baseEdges.size(); edgeIndex++) {
                Edge base = baseEdges.get(edgeIndex);
                edges.add(new Edge(
                        base.id(), base.fromNodeId(), base.toNodeId(), edgeStatuses[edgeIndex], base.labelOrNull()));
            }
            GraphState state = new GraphState(nodes, edges, input.directed(), facts);
            steps.add(new SimulationStep(
                    new SimulationSnapshot(state, activeNodeIds, activeEdgeIds),
                    highlightedLine,
                    narration,
                    eventType,
                    null));
        }

        public List<SimulationStep> steps() {
            return List.copyOf(steps);
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

    /** Mutable TABLE producer used by Floyd-Warshall's real distance matrix. */
    public static final class TableTraceBuilder {
        private final List<String> columns;
        private final List<SimulationStep> steps = new ArrayList<>();

        public TableTraceBuilder(List<String> columns) {
            this.columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
        }

        public void add(
                List<List<TypedCell>> rows,
                List<Fact> facts,
                int highlightedLine,
                String narration,
                StepEventType eventType) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("weighted graph table trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            steps.add(new SimulationStep(
                    new SimulationSnapshot(
                            new TableState(columns, rows, facts),
                            Set.of(),
                            Set.of()),
                    highlightedLine,
                    Objects.requireNonNull(narration, "narration"),
                    Objects.requireNonNull(eventType, "eventType"),
                    null));
        }

        public List<SimulationStep> steps() {
            return List.copyOf(steps);
        }
    }

    public static SnapshotStatus[] statuses(int size, SnapshotStatus status) {
        SnapshotStatus[] result = new SnapshotStatus[size];
        Arrays.fill(result, Objects.requireNonNull(status, "status"));
        return result;
    }

    private static Set<String> idsForVertices(Iterable<Integer> vertices, int n) {
        Objects.requireNonNull(vertices, "activeVertices");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Integer vertex : vertices) {
            if (vertex == null || vertex < 0 || vertex >= n) {
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

    public record WeightedEdge(int from, int to, long weight) {
        public WeightedEdge {
            if (from < 0 || to < 0) {
                throw new IllegalArgumentException("weighted edge endpoints must be nonnegative");
            }
        }
    }

    public record Arc(int from, int to, long weight, int edgeIndex) {
        public Arc {
            if (from < 0 || to < 0 || edgeIndex < 0) {
                throw new IllegalArgumentException("weighted graph arc indexes must be nonnegative");
            }
        }
    }

    public static final class Input {
        private final int n;
        private final List<WeightedEdge> edges;
        private final int source;
        private final boolean directed;

        private Input(int n, List<WeightedEdge> edges, int source, boolean directed) {
            if (n < MIN_N || n > MAX_N) {
                throw new IllegalArgumentException("weighted graph n must be in the inclusive range 1.." + MAX_N);
            }
            this.n = n;
            this.edges = List.copyOf(edges);
            this.source = source;
            this.directed = directed;
        }

        public int n() {
            return n;
        }

        public List<WeightedEdge> edges() {
            return edges;
        }

        public int source() {
            return source;
        }

        public boolean hasSource() {
            return source >= 0;
        }

        public boolean directed() {
            return directed;
        }
    }

    private record EdgeKey(int from, int to) {
        static EdgeKey of(int from, int to, boolean directed) {
            return directed || from <= to ? new EdgeKey(from, to) : new EdgeKey(to, from);
        }
    }

    public static ObjectNode inputWithEdges(
            int n,
            List<WeightedEdge> edges,
            Integer source,
            boolean directed) {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("n", n);
        ArrayNode edgeArray = input.putArray("edges");
        for (WeightedEdge edge : edges) {
            ObjectNode edgeNode = edgeArray.addObject();
            edgeNode.put("from", edge.from());
            edgeNode.put("to", edge.to());
            edgeNode.put("weight", edge.weight());
        }
        if (source != null) {
            input.put("source", source);
        }
        input.put("directed", directed);
        return input;
    }
}
