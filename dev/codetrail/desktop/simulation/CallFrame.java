package dev.codetrail.desktop.simulation;

import java.util.Objects;

/** A visible runtime call frame; a returned frame is emitted once as DONE. */
public final class CallFrame {
    private final String id;
    private final String functionName;
    private final int argument;
    private final String result;
    private final SnapshotStatus status;

    public CallFrame(String id, String functionName, int argument, String result, SnapshotStatus status) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must be nonblank");
        }
        if (functionName == null || functionName.isBlank()) {
            throw new IllegalArgumentException("functionName must be nonblank");
        }
        this.id = id;
        this.functionName = functionName;
        this.argument = argument;
        this.result = result;
        this.status = Objects.requireNonNull(status, "status");
        if (status == SnapshotStatus.DONE && result == null) {
            throw new IllegalArgumentException("a DONE frame must have a result");
        }
    }

    public CallFrame(String id, String functionName, int argument, SnapshotStatus status) {
        this(id, functionName, argument, null, status);
    }

    public String id() { return id; }
    public String functionName() { return functionName; }
    public int argument() { return argument; }
    public String result() { return result; }
    public SnapshotStatus status() { return status; }
    public boolean hasResult() { return result != null; }
    public CallFrame copy() { return new CallFrame(id, functionName, argument, result, status); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CallFrame frame)) return false;
        return argument == frame.argument
                && id.equals(frame.id)
                && functionName.equals(frame.functionName)
                && Objects.equals(result, frame.result)
                && status == frame.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, functionName, argument, result, status);
    }
}
