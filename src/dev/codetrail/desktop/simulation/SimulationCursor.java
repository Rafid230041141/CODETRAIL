package dev.codetrail.desktop.simulation;

import java.util.Objects;

/** Immutable playback position; previous() is a pure index operation. */
public final class SimulationCursor {
    private final SimulationTrace trace;
    private final int index;

    SimulationCursor(SimulationTrace trace, int index) {
        this.trace = Objects.requireNonNull(trace, "trace");
        if (index < 0 || index >= trace.size()) {
            throw new IndexOutOfBoundsException("index: " + index);
        }
        this.index = index;
    }

    public int index() { return index; }
    public int size() { return trace.size(); }
    public boolean atStart() { return index == 0; }
    public boolean atEnd() { return index == trace.size() - 1; }
    public SimulationStep current() { return trace.stepAt(index); }
    public SimulationCursor next() { return atEnd() ? this : new SimulationCursor(trace, index + 1); }
    public SimulationCursor previous() { return atStart() ? this : new SimulationCursor(trace, index - 1); }
    public SimulationCursor stepForward() { return next(); }
    public SimulationCursor stepBackward() { return previous(); }
}
