package dev.codetrail.desktop.simulation.graphs.structural;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/** Small bounded parser, adjacency, and immutable graph-trace helpers. */
final class StructuralGraphSupport {
    static final int MIN_N = 1;
    static final int MAX_N = 10;
    static final int MAX_EDGES = 24;
    static final int MIN_CAPACITY = 0;
    static final int MAX_CAPACITY = 99;
    static final int MAX_TRACE_STEPS = 4096;

    private StructuralGraphSupport() {
    }

    /** An input edge. Pair-graph edges use capacity -1; flow edges use 0..99. */
    record InputEdge(int from, int to, int capacity) {
        InputEdge(int from, int to) {
            this(from, to, -1);
        }
    }

    /** Validated structural graph input. Duplicate edges remain distinct by index. */
    record GraphInput(int n, List<InputEdge> edges, boolean directed, int source, int sink) {
        GraphInput {
            if (n < MIN_N || n > MAX_N) {
                throw new IllegalArgumentException("graph n must be in the inclusive range 1.." + MAX_N);
            }
            edges = List.copyOf(Objects.requireNonNull(edges, "edges"));
            if (edges.size() > MAX_EDGES) {
                throw new IllegalArgumentException("graph edge count must be at most " + MAX_EDGES);
            }
            for (InputEdge edge : edges) {
                Objects.requireNonNull(edge, "edge");
                if (edge.from() < 0 || edge.from() >= n || edge.to() < 0 || edge.to() >= n) {
                    throw new IllegalArgumentException("graph edge endpoints must be in the range 0.." + (n - 1));
                }
                if (edge.capacity() < -1 || edge.capacity() > MAX_CAPACITY) {
                    throw new IllegalArgumentException("graph edge capacity must be in the range 0.." + MAX_CAPACITY);
                }
            }
            if (source >= n || source < -1 || sink >= n || sink < -1) {
                throw new IllegalArgumentException("graph source/sink must be in the range 0.." + (n - 1));
            }
        }
    }

    /** Adjacency arc that retains the stable original edge index. */
    record Neighbor(int vertex, int edgeIndex) {
    }

    static GraphInput parsePairGraph(JsonNode input, boolean expectedDirected, boolean requireSource) {
        ObjectNode object = requireObject(input, "graph input");
        int n = readInt(object.get("n"), "graph n");
        JsonNode edgeNode = object.get("edges");
        if (edgeNode == null || !edgeNode.isArray()) {
            throw new IllegalArgumentException("graph edges must be a JSON array of integer pairs");
        }
        if (edgeNode.size() > MAX_EDGES) {
            throw new IllegalArgumentException("graph edge count must be at most " + MAX_EDGES);
        }
        JsonNode directedNode = object.get("directed");
        if (directedNode == null || !directedNode.isBoolean()) {
            throw new IllegalArgumentException("graph directed must be a boolean");
        }
        boolean directed = directedNode.booleanValue();
        if (directed != expectedDirected) {
            throw new IllegalArgumentException(
                    "this structural graph requires directed=" + expectedDirected);
        }
        int source = -1;
        if (requireSource) {
            source = readInt(object.get("source"), "graph source");
        }
        List<InputEdge> edges = new ArrayList<>(edgeNode.size());
        for (int index = 0; index < edgeNode.size(); index++) {
            JsonNode pair = edgeNode.get(index);
            if (pair == null || !pair.isArray() || pair.size() != 2) {
                throw new IllegalArgumentException(
                        "graph edge " + index + " must be an integer pair [from,to]");
            }
            edges.add(new InputEdge(
                    readInt(pair.get(0), "graph edge " + index + " from"),
                    readInt(pair.get(1), "graph edge " + index + " to")));
        }
        return new GraphInput(n, edges, directed, source, -1);
    }

    static GraphInput parseFlowGraph(JsonNode input) {
        ObjectNode object = requireObject(input, "max-flow input");
        int n = readInt(object.get("n"), "graph n");
        JsonNode edgeNode = object.get("edges");
        if (edgeNode == null || !edgeNode.isArray()) {
            throw new IllegalArgumentException(
                    "max-flow edges must be a JSON array of {from,to,capacity} objects");
        }
        if (edgeNode.size() > MAX_EDGES) {
            throw new IllegalArgumentException("graph edge count must be at most " + MAX_EDGES);
        }
        JsonNode directedNode = object.get("directed");
        if (directedNode == null || !directedNode.isBoolean()) {
            throw new IllegalArgumentException("graph directed must be a boolean");
        }
        if (!directedNode.booleanValue()) {
            throw new IllegalArgumentException("maximum flow requires directed=true");
        }
        int source = readInt(object.get("source"), "graph source");
        int sink = readInt(object.get("sink"), "graph sink");
        if (source == sink) {
            throw new IllegalArgumentException("maximum-flow source and sink must be distinct");
        }
        List<InputEdge> edges = new ArrayList<>(edgeNode.size());
        for (int index = 0; index < edgeNode.size(); index++) {
            JsonNode edge = edgeNode.get(index);
            if (edge == null || !edge.isObject()) {
                throw new IllegalArgumentException(
                        "max-flow edge " + index + " must be an object {from,to,capacity}");
            }
            int capacity = readInt(edge.get("capacity"), "max-flow edge " + index + " capacity");
            if (capacity < MIN_CAPACITY || capacity > MAX_CAPACITY) {
                throw new IllegalArgumentException(
                        "max-flow edge capacity must be in the range " + MIN_CAPACITY + ".." + MAX_CAPACITY);
            }
            edges.add(new InputEdge(
                    readInt(edge.get("from"), "max-flow edge " + index + " from"),
                    readInt(edge.get("to"), "max-flow edge " + index + " to"),
                    capacity));
        }
        return new GraphInput(n, edges, true, source, sink);
    }

    static ObjectNode pairDefault(int n, int[][] edges, boolean directed) {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("n", n);
        ArrayNode edgeArray = input.putArray("edges");
        for (int[] edge : edges) {
            ArrayNode pair = edgeArray.addArray();
            pair.add(edge[0]);
            pair.add(edge[1]);
        }
        input.put("directed", directed);
        return input;
    }

    static ObjectNode pairDefault(int n, int[][] edges, int source, boolean directed) {
        ObjectNode input = pairDefault(n, edges, directed);
        input.put("source", source);
        // Keep curriculum field order (n, edges, source, directed) for readability.
        JsonNode directedNode = input.remove("directed");
        input.set("directed", directedNode);
        return input;
    }

    static ObjectNode flowDefault(int n, int[][] edges, int source, int sink, boolean directed) {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("n", n);
        ArrayNode edgeArray = input.putArray("edges");
        for (int[] edge : edges) {
            ObjectNode object = edgeArray.addObject();
            object.put("from", edge[0]);
            object.put("to", edge[1]);
            object.put("capacity", edge[2]);
        }
        input.put("source", source);
        input.put("sink", sink);
        input.put("directed", directed);
        return input;
    }

    static List<List<Neighbor>> adjacency(GraphInput input) {
        List<List<Neighbor>> adjacency = new ArrayList<>(input.n());
        for (int vertex = 0; vertex < input.n(); vertex++) {
            adjacency.add(new ArrayList<>());
        }
        for (int edgeIndex = 0; edgeIndex < input.edges().size(); edgeIndex++) {
            InputEdge edge = input.edges().get(edgeIndex);
            adjacency.get(edge.from()).add(new Neighbor(edge.to(), edgeIndex));
            if (!input.directed() && edge.from() != edge.to()) {
                adjacency.get(edge.to()).add(new Neighbor(edge.from(), edgeIndex));
            }
        }
        return adjacency.stream().map(List::copyOf).toList();
    }

    static String nodeId(int vertex) {
        return Integer.toString(vertex);
    }

    static String edgeId(int edgeIndex) {
        return "edge-" + edgeIndex;
    }

    static String formatVertices(Iterable<Integer> vertices) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (Integer vertex : vertices) {
            result.add(Integer.toString(Objects.requireNonNull(vertex, "vertex")));
        }
        return result.toString();
    }

    static String formatIntArray(int[] values, String unknown) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (int value : values) {
            result.add(value < 0 ? unknown : Integer.toString(value));
        }
        return result.toString();
    }

    static String formatIntMap(int[] values, int unknown, String unknownText) {
        StringJoiner result = new StringJoiner(", ", "{", "}");
        for (int vertex = 0; vertex < values.length; vertex++) {
            result.add(vertex + "=" + (values[vertex] == unknown ? unknownText : values[vertex]));
        }
        return result.toString();
    }

    static String formatBooleanArray(boolean[] values) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        for (boolean value : values) {
            result.add(Boolean.toString(value));
        }
        return result.toString();
    }

    static String formatPairList(List<int[]> pairs, boolean directed) {
        StringJoiner result = new StringJoiner(", ", "[", "]");
        String arrow = directed ? "→" : "—";
        for (int[] pair : pairs) {
            result.add(pair[0] + arrow + pair[1]);
        }
        return result.toString();
    }

    static Fact fact(String key, String value, SnapshotStatus status) {
        return new Fact(key, value, status);
    }

    /** Mutable producer facade that deep-snapshots every graph event. */
    static final class TraceBuilder {
        private final boolean directed;
        private final List<Node> baseNodes;
        private final List<Edge> baseEdges;
        private final SnapshotStatus[] nodeStatuses;
        private final SnapshotStatus[] edgeStatuses;
        private final String[] nodeLabels;
        private final String[] edgeLabels;
        private final List<SimulationStep> steps = new ArrayList<>();

        TraceBuilder(GraphInput input, List<String> labels) {
            Objects.requireNonNull(input, "input");
            Objects.requireNonNull(labels, "labels");
            if (labels.size() != input.edges().size()) {
                throw new IllegalArgumentException("edge label count must match graph edge count");
            }
            directed = input.directed();
            baseNodes = new ArrayList<>(input.n());
            nodeLabels = new String[input.n()];
            for (int vertex = 0; vertex < input.n(); vertex++) {
                nodeLabels[vertex] = nodeId(vertex);
                baseNodes.add(new Node(nodeId(vertex), nodeLabels[vertex], SnapshotStatus.DEFAULT));
            }
            baseEdges = new ArrayList<>(input.edges().size());
            edgeLabels = labels.toArray(String[]::new);
            for (int edgeIndex = 0; edgeIndex < input.edges().size(); edgeIndex++) {
                InputEdge edge = input.edges().get(edgeIndex);
                String label = requireLabel(edgeLabels[edgeIndex]);
                edgeLabels[edgeIndex] = label;
                baseEdges.add(new Edge(
                        edgeId(edgeIndex),
                        nodeId(edge.from()),
                        nodeId(edge.to()),
                        SnapshotStatus.DEFAULT,
                        label));
            }
            nodeStatuses = fill(input.n(), SnapshotStatus.DEFAULT);
            edgeStatuses = fill(input.edges().size(), SnapshotStatus.DEFAULT);
        }

        void nodeStatus(int vertex, SnapshotStatus status) {
            checkNode(vertex);
            nodeStatuses[vertex] = Objects.requireNonNull(status, "status");
        }

        void edgeStatus(int edgeIndex, SnapshotStatus status) {
            checkEdge(edgeIndex);
            edgeStatuses[edgeIndex] = Objects.requireNonNull(status, "status");
        }

        void nodeLabel(int vertex, String label) {
            checkNode(vertex);
            nodeLabels[vertex] = requireLabel(label);
        }

        void edgeLabel(int edgeIndex, String label) {
            checkEdge(edgeIndex);
            edgeLabels[edgeIndex] = requireLabel(label);
        }

        List<SimulationStep> steps() {
            return List.copyOf(steps);
        }

        void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                List<Fact> facts) {
            add(highlightedLine, narration, eventType, activeVertices, activeEdges, facts, null);
        }

        void add(
                int highlightedLine,
                String narration,
                StepEventType eventType,
                Iterable<Integer> activeVertices,
                Iterable<Integer> activeEdges,
                List<Fact> facts,
                String frameId) {
            if (steps.size() >= MAX_TRACE_STEPS) {
                throw new IllegalStateException("structural graph trace exceeded " + MAX_TRACE_STEPS + " steps");
            }
            if (highlightedLine < 0) {
                throw new IllegalArgumentException("highlightedLine cannot be negative");
            }
            if (narration == null || narration.isBlank()) {
                throw new IllegalArgumentException("narration must be nonblank");
            }
            Objects.requireNonNull(eventType, "eventType");
            List<Fact> checkedFacts = List.copyOf(Objects.requireNonNull(facts, "facts"));
            Set<String> activeNodeIds = nodeIds(activeVertices, nodeStatuses.length);
            Set<String> activeEdgeIds = edgeIds(activeEdges, edgeStatuses.length);
            SimulationSnapshot snapshot = new SimulationSnapshot(
                    graphState(checkedFacts), activeNodeIds, activeEdgeIds);
            steps.add(new SimulationStep(snapshot, highlightedLine, narration, eventType, frameId));
        }

        private GraphState graphState(List<Fact> facts) {
            List<Node> nodes = new ArrayList<>(baseNodes.size());
            for (int vertex = 0; vertex < baseNodes.size(); vertex++) {
                nodes.add(new Node(
                        baseNodes.get(vertex).id(),
                        nodeLabels[vertex],
                        nodeStatuses[vertex]));
            }
            List<Edge> edges = new ArrayList<>(baseEdges.size());
            for (int edgeIndex = 0; edgeIndex < baseEdges.size(); edgeIndex++) {
                Edge base = baseEdges.get(edgeIndex);
                edges.add(new Edge(
                        base.id(),
                        base.fromNodeId(),
                        base.toNodeId(),
                        edgeStatuses[edgeIndex],
                        edgeLabels[edgeIndex]));
            }
            return new GraphState(nodes, edges, directed, facts);
        }

        private void checkNode(int vertex) {
            if (vertex < 0 || vertex >= nodeStatuses.length) {
                throw new IllegalArgumentException("graph node index out of range: " + vertex);
            }
        }

        private void checkEdge(int edgeIndex) {
            if (edgeIndex < 0 || edgeIndex >= edgeStatuses.length) {
                throw new IllegalArgumentException("graph edge index out of range: " + edgeIndex);
            }
        }
    }

    private static ObjectNode requireObject(JsonNode input, String name) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException(name + " must be a JSON object");
        }
        return (ObjectNode) input;
    }

    private static int readInt(JsonNode node, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return node.intValue();
    }

    private static String requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("graph labels must be nonblank");
        }
        return label;
    }

    private static SnapshotStatus[] fill(int size, SnapshotStatus status) {
        SnapshotStatus[] result = new SnapshotStatus[size];
        Arrays.fill(result, status);
        return result;
    }

    private static Set<String> nodeIds(Iterable<Integer> vertices, int count) {
        Objects.requireNonNull(vertices, "activeVertices");
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Integer vertex : vertices) {
            if (vertex == null || vertex < 0 || vertex >= count) {
                throw new IllegalArgumentException("active graph node index out of range: " + vertex);
            }
            ids.add(nodeId(vertex));
        }
        return Set.copyOf(ids);
    }

    private static Set<String> edgeIds(Iterable<Integer> edges, int count) {
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
