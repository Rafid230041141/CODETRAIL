package dev.codetrail.desktop.simulation;

import java.util.Objects;

/** One immutable pseudocode/event/state entry in a precomputed trace. */
public final class SimulationStep {
    private final SimulationSnapshot snapshot;
    private final int highlightedLine;
    private final String narration;
    private final StepEventType eventType;
    private final String frameId;

    public SimulationStep(
            SimulationSnapshot snapshot,
            int highlightedLine,
            String narration,
            StepEventType eventType,
            String frameId) {
        if (highlightedLine < 0) {
            throw new IllegalArgumentException("highlightedLine cannot be negative");
        }
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot").copy();
        this.highlightedLine = highlightedLine;
        this.narration = Objects.requireNonNull(narration, "narration");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        if (frameId != null && frameId.isBlank()) {
            throw new IllegalArgumentException("frameId must be nonblank when present");
        }
        this.frameId = frameId;
    }

    public SimulationStep(
            SimulationState state,
            int highlightedLine,
            String narration,
            StepEventType eventType,
            String frameId,
            java.util.Set<String> activeNodeIds,
            java.util.Set<String> activeEdgeIds) {
        this(new SimulationSnapshot(state, activeNodeIds, activeEdgeIds), highlightedLine, narration, eventType, frameId);
    }

    public SimulationSnapshot snapshot() { return snapshot.copy(); }
    public SimulationState stateSnapshot() { return snapshot.stateSnapshot(); }
    public int highlightedLine() { return highlightedLine; }
    public int highlightedPseudocodeLine() { return highlightedLine; }
    public String narration() { return narration; }
    public StepEventType eventType() { return eventType; }
    public StepEventType event() { return eventType; }
    public String frameId() { return frameId; }

    public SimulationStep copy() {
        return new SimulationStep(snapshot, highlightedLine, narration, eventType, frameId);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SimulationStep step)) return false;
        return highlightedLine == step.highlightedLine
                && snapshot.equals(step.snapshot)
                && narration.equals(step.narration)
                && eventType == step.eventType
                && Objects.equals(frameId, step.frameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshot, highlightedLine, narration, eventType, frameId);
    }
}
