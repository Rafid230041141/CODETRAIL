package dev.codetrail.desktop.simulation.graphs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validated, immutable input shared by the graph traversal engines.
 *
 * <p>Vertices are the integers {@code 0..n-1}. Edges retain their input order,
 * which is also the deterministic neighbour order used by both traversals.
 * A self-loop is valid and is examined once; it cannot discover its already
 * visited endpoint. Duplicate logical edges are rejected. For an undirected
 * graph, {@code (u,v)} and {@code (v,u)} are the same logical edge.</p>
 */
public final class GraphInput {
    public static final int MIN_N = 1;
    public static final int MAX_N = 10;
    public static final int MAX_EDGES = 24;

    public static final int DEFAULT_N = 6;
    public static final int DEFAULT_SOURCE = 0;
    public static final boolean DEFAULT_DIRECTED = false;
    public static final List<Edge> DEFAULT_EDGES = List.of(
            new Edge(0, 1),
            new Edge(0, 2),
            new Edge(1, 3),
            new Edge(2, 4),
            new Edge(4, 5));

    private final int n;
    private final List<Edge> edges;
    private final int source;
    private final boolean directed;

    public GraphInput(int n, List<Edge> edges, int source, boolean directed) {
        if (n < MIN_N || n > MAX_N) {
            throw new IllegalArgumentException("graph n must be in the inclusive range 1.." + MAX_N);
        }
        if (edges == null) {
            throw new IllegalArgumentException("graph edges must be a JSON array");
        }
        if (edges.size() > MAX_EDGES) {
            throw new IllegalArgumentException("graph edge count must be at most " + MAX_EDGES);
        }
        if (source < 0 || source >= n) {
            throw new IllegalArgumentException("graph source must be in the range 0.." + (n - 1));
        }

        Set<EdgeKey> seen = new HashSet<>();
        for (Edge edge : edges) {
            if (edge == null) {
                throw new IllegalArgumentException("graph edges cannot contain null entries");
            }
            if (edge.from() < 0 || edge.from() >= n || edge.to() < 0 || edge.to() >= n) {
                throw new IllegalArgumentException("graph edge endpoints must be in the range 0.." + (n - 1));
            }
            EdgeKey key = EdgeKey.of(edge, directed);
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "duplicate graph edge: (" + edge.from() + "," + edge.to() + ")");
            }
        }
        this.n = n;
        this.edges = List.copyOf(edges);
        this.source = source;
        this.directed = directed;
    }

    /** Parse the bounded public JSON shape: {@code {n, edges, source, directed}}. */
    public static GraphInput parse(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("graph input must be a JSON object");
        }
        int n = readInt(input.get("n"), "graph n");
        JsonNode edgesNode = input.get("edges");
        if (edgesNode == null || !edgesNode.isArray()) {
            throw new IllegalArgumentException("graph edges must be a JSON array");
        }
        if (edgesNode.size() > MAX_EDGES) {
            throw new IllegalArgumentException("graph edge count must be at most " + MAX_EDGES);
        }
        int source = readInt(input.get("source"), "graph source");
        JsonNode directedNode = input.get("directed");
        boolean directed = false;
        if (directedNode != null) {
            if (!directedNode.isBoolean()) {
                throw new IllegalArgumentException("graph directed must be boolean when present");
            }
            directed = directedNode.booleanValue();
        }

        List<Edge> edges = new java.util.ArrayList<>(edgesNode.size());
        for (int index = 0; index < edgesNode.size(); index++) {
            JsonNode edgeNode = edgesNode.get(index);
            if (edgeNode == null || !edgeNode.isArray() || edgeNode.size() != 2) {
                throw new IllegalArgumentException(
                        "graph edge " + index + " must be an integer pair [from,to]");
            }
            int from = readInt(edgeNode.get(0), "graph edge " + index + " from");
            int to = readInt(edgeNode.get(1), "graph edge " + index + " to");
            edges.add(new Edge(from, to));
        }
        return new GraphInput(n, edges, source, directed);
    }

    /** Alias kept for callers that use a read-style parser name. */
    public static GraphInput read(JsonNode input) {
        return parse(input);
    }

    /** Alias kept for callers that use a from-json parser name. */
    public static GraphInput fromJson(JsonNode input) {
        return parse(input);
    }

    /** Return a fresh mutable Jackson tree for the standard traversal example. */
    public static ObjectNode defaultInput() {
        ObjectNode input = JsonNodeFactory.instance.objectNode();
        input.put("n", DEFAULT_N);
        ArrayNode edges = input.putArray("edges");
        for (Edge edge : DEFAULT_EDGES) {
            ArrayNode pair = edges.addArray();
            pair.add(edge.from());
            pair.add(edge.to());
        }
        input.put("source", DEFAULT_SOURCE);
        input.put("directed", DEFAULT_DIRECTED);
        return input;
    }

    public int n() {
        return n;
    }

    public List<Edge> edges() {
        return edges;
    }

    public int source() {
        return source;
    }

    public boolean directed() {
        return directed;
    }

    public boolean hasSelfLoops() {
        return edges.stream().anyMatch(edge -> edge.from() == edge.to());
    }

    private static int readInt(JsonNode node, String field) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException(field + " must be an integer");
        }
        return node.intValue();
    }

    /** A pair key whose order follows graph directionality. */
    private record EdgeKey(int from, int to) {
        static EdgeKey of(Edge edge, boolean directed) {
            if (directed || edge.from() <= edge.to()) {
                return new EdgeKey(edge.from(), edge.to());
            }
            return new EdgeKey(edge.to(), edge.from());
        }
    }

    /** A typed endpoint pair in the same vertex numbering as the input. */
    public record Edge(int from, int to) {
        public Edge {
            // Endpoint range depends on n and is checked by GraphInput.
        }

        public int fromNode() {
            return from;
        }

        public int toNode() {
            return to;
        }
    }
}
