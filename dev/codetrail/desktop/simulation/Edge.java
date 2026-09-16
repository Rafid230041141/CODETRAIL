package dev.codetrail.desktop.simulation;

import java.util.Objects;
import java.util.Optional;

/** A typed edge for linked, tree, and graph snapshots. */
public final class Edge {
    private final String id;
    private final String fromNodeId;
    private final String toNodeId;
    private final SnapshotStatus status;
    private final String label;

    public Edge(String id, String fromNodeId, String toNodeId, SnapshotStatus status) {
        this(id, fromNodeId, toNodeId, status, null);
    }

    /** Backward-compatible weighted/capacity edge constructor. */
    public Edge(String id, String fromNodeId, String toNodeId, SnapshotStatus status, String label) {
        this.id = requireText(id, "id");
        this.fromNodeId = requireText(fromNodeId, "fromNodeId");
        this.toNodeId = requireText(toNodeId, "toNodeId");
        this.status = Objects.requireNonNull(status, "status");
        if (label != null && label.isBlank()) {
            throw new IllegalArgumentException("label must be nonblank when present");
        }
        this.label = label;
    }

    /** Convenience overload for callers that place the optional label first. */
    public Edge(String id, String fromNodeId, String toNodeId, String label, SnapshotStatus status) {
        this(id, fromNodeId, toNodeId, status, label);
    }

    public String id() { return id; }
    public String fromNodeId() { return fromNodeId; }
    public String toNodeId() { return toNodeId; }
    public SnapshotStatus status() { return status; }
    public Optional<String> label() { return Optional.ofNullable(label); }
    public String labelOrNull() { return label; }
    public Edge copy() { return new Edge(id, fromNodeId, toNodeId, status, label); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Edge edge)) return false;
        return id.equals(edge.id)
                && fromNodeId.equals(edge.fromNodeId)
                && toNodeId.equals(edge.toNodeId)
                && status == edge.status
                && Objects.equals(label, edge.label);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, fromNodeId, toNodeId, status, label);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
