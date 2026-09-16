package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Objects;

/** Immutable rooted tree state with explicit nodes and edges. */
public final class TreeState implements SimulationState {
    private final List<Node> nodes;
    private final List<Edge> edges;
    private final String rootId;
    private final List<Fact> facts;

    public TreeState(List<Node> nodes, List<Edge> edges, String rootId) {
        this(nodes, edges, rootId, List.of());
    }

    public TreeState(List<Node> nodes, List<Edge> edges, String rootId, List<Fact> facts) {
        this.nodes = LinkedState.copyNodes(nodes);
        this.edges = LinkedState.copyEdges(edges);
        this.rootId = rootId;
        this.facts = copyFacts(facts);
    }

    @Override
    public RendererFamily rendererFamily() { return RendererFamily.TREE; }

    public List<Node> nodes() { return nodes; }
    public List<Edge> edges() { return edges; }
    public String rootId() { return rootId; }
    public List<Fact> facts() { return facts; }

    @Override
    public TreeState copy() { return new TreeState(nodes, edges, rootId, facts); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TreeState state)) return false;
        return nodes.equals(state.nodes)
                && edges.equals(state.edges)
                && Objects.equals(rootId, state.rootId)
                && facts.equals(state.facts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, edges, rootId, facts);
    }

    static List<Fact> copyFacts(List<Fact> facts) {
        Objects.requireNonNull(facts, "facts");
        return facts.stream().map(fact -> Objects.requireNonNull(fact, "fact").copy()).toList();
    }
}
