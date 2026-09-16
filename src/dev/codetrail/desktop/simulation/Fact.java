package dev.codetrail.desktop.simulation;

import java.util.Objects;

/** An immutable named fact shown with a stack or algorithm state. */
public final class Fact {
    private final String key;
    private final String value;
    private final SnapshotStatus status;

    public Fact(String key, String value, SnapshotStatus status) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must be nonblank");
        }
        this.key = key;
        this.value = Objects.requireNonNull(value, "value");
        this.status = Objects.requireNonNull(status, "status");
    }

    public String key() { return key; }
    public String value() { return value; }
    public SnapshotStatus status() { return status; }
    public Fact copy() { return new Fact(key, value, status); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Fact fact)) return false;
        return key.equals(fact.key) && value.equals(fact.value) && status == fact.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, status);
    }
}
