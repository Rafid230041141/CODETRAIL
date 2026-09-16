package dev.codetrail.desktop.simulation;

import java.util.Objects;

/** A typed node for linked, tree, and graph snapshots. */
public final class Node {
    private final String id;
    private final String label;
    private final SnapshotStatus status;

    public Node(String id, String label, SnapshotStatus status) {
        this.id = requireText(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String id() { return id; }
    public String label() { return label; }
    public SnapshotStatus status() { return status; }
    public Node copy() { return new Node(id, label, status); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Node node)) return false;
        return id.equals(node.id) && label.equals(node.label) && status == node.status;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, label, status);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
