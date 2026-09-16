package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable graph state with explicit nodes, edges, and directionality. */
public final class GraphState implements SimulationState {
    private final java.util.List<Node> nodes;
    private final java.util.List<Edge> edges;
    private final boolean directed;
    private final Map<String, NodeCoordinate> nodeCoordinates;
    private final List<Fact> facts;

    public GraphState(List<Node> nodes, List<Edge> edges, boolean directed) {
        this(nodes, edges, directed, Map.of(), List.of());
    }

    /** Graph state with real algorithm-space coordinates when available. */
    public GraphState(
            List<Node> nodes,
            List<Edge> edges,
            boolean directed,
            Map<String, NodeCoordinate> nodeCoordinates) {
        this(nodes, edges, directed, nodeCoordinates, List.of());
    }

    public GraphState(List<Node> nodes, List<Edge> edges, boolean directed, List<Fact> facts) {
        this(nodes, edges, directed, Map.of(), facts);
    }

    public GraphState(
            List<Node> nodes,
            List<Edge> edges,
            boolean directed,
            Map<String, NodeCoordinate> nodeCoordinates,
            List<Fact> facts) {
        this.nodes = LinkedState.copyNodes(nodes);
        this.edges = LinkedState.copyEdges(edges);
        this.directed = directed;
        Objects.requireNonNull(nodeCoordinates, "nodeCoordinates");
        Map<String, NodeCoordinate> copiedCoordinates = nodeCoordinates.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> requireNodeId(entry.getKey()),
                        entry -> Objects.requireNonNull(entry.getValue(), "node coordinate").copy()));
        java.util.Set<String> nodeIds = this.nodes.stream().map(Node::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!copiedCoordinates.keySet().isEmpty() && !copiedCoordinates.keySet().equals(nodeIds)) {
            throw new IllegalArgumentException("coordinates must be provided for every graph node");
        }
        this.nodeCoordinates = copiedCoordinates;
        this.facts = TreeState.copyFacts(facts);
    }

    @Override
    public RendererFamily rendererFamily() { return RendererFamily.GRAPH; }

    public List<Node> nodes() { return nodes; }
    public List<Edge> edges() { return edges; }
    public boolean directed() { return directed; }
    public Map<String, NodeCoordinate> nodeCoordinates() { return nodeCoordinates; }
    public List<Fact> facts() { return facts; }

    @Override
    public GraphState copy() { return new GraphState(nodes, edges, directed, nodeCoordinates, facts); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GraphState state)) return false;
        return directed == state.directed
                && nodes.equals(state.nodes)
                && edges.equals(state.edges)
                && nodeCoordinates.equals(state.nodeCoordinates)
                && facts.equals(state.facts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, edges, directed, nodeCoordinates, facts);
    }

    private static String requireNodeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("coordinate node ID must be nonblank");
        }
        return id;
    }
}
