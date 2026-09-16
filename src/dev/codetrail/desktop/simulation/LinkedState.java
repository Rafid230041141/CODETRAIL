package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Objects;

/** Immutable linked-structure state with explicit nodes and links. */
public final class LinkedState implements SimulationState {
    private final List<Node> nodes;
    private final List<Edge> edges;
    private final String headId;
    private final List<Fact> facts;

    public LinkedState(List<Node> nodes, List<Edge> edges, String headId) {
        this(nodes, edges, headId, List.of());
    }

    public LinkedState(List<Node> nodes, List<Edge> edges, String headId, List<Fact> facts) {
        this.nodes = copyNodes(nodes);
        this.edges = copyEdges(edges);
        this.headId = headId;
        this.facts = TreeState.copyFacts(facts);
    }

    @Override
    public RendererFamily rendererFamily() { return RendererFamily.LINKED; }

    public List<Node> nodes() { return nodes; }
    public List<Edge> edges() { return edges; }
    public String headId() { return headId; }
    public List<Fact> facts() { return facts; }

    @Override
    public LinkedState copy() { return new LinkedState(nodes, edges, headId, facts); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LinkedState state)) return false;
        return nodes.equals(state.nodes) && edges.equals(state.edges) && Objects.equals(headId, state.headId)
                && facts.equals(state.facts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, edges, headId, facts);
    }

    static List<Node> copyNodes(List<Node> nodes) {
        Objects.requireNonNull(nodes, "nodes");
        return nodes.stream().map(node -> Objects.requireNonNull(node, "node").copy()).toList();
    }

    static List<Edge> copyEdges(List<Edge> edges) {
        Objects.requireNonNull(edges, "edges");
        return edges.stream().map(edge -> Objects.requireNonNull(edge, "edge").copy()).toList();
    }
}
