package dev.codetrail.desktop.simulation;

import java.util.Objects;

/** A display cell with explicit text and semantic status. */
public final class TypedCell {
    private final String key;
    private final String value;
    private final SnapshotStatus status;

    public TypedCell(String key, String value, SnapshotStatus status) {
        this.key = requireText(key, "key");
        this.value = Objects.requireNonNull(value, "value");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String key() { return key; }
    public String value() { return value; }
    public SnapshotStatus status() { return status; }
    public TypedCell copy() { return new TypedCell(key, value, status); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TypedCell cell)) return false;
        return key.equals(cell.key) && value.equals(cell.value) && status == cell.status;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, value, status);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must be nonblank");
        }
        return value;
    }
}
