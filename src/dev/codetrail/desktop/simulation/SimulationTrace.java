package dev.codetrail.desktop.simulation;

import java.util.List;
import java.util.Objects;

/** Immutable, bounded, precomputed trace owned by shared playback controls. */
public final class SimulationTrace {
    private final SimulationMetadata metadata;
    private final List<SimulationStep> steps;

    public SimulationTrace(SimulationMetadata metadata, List<SimulationStep> steps) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(steps, "steps");
        this.steps = steps.stream().map(step -> {
            SimulationStep checked = Objects.requireNonNull(step, "step");
            int line = checked.highlightedLine();
            if (line > metadata.pseudocode().size()) {
                throw new IllegalArgumentException("highlightedLine exceeds metadata pseudocode");
            }
            if (checked.stateSnapshot().rendererFamily() != metadata.rendererFamily()) {
                throw new IllegalArgumentException("step state renderer family does not match metadata");
            }
            return checked.copy();
        }).toList();
        if (this.steps.isEmpty()) {
            throw new IllegalArgumentException("a simulation trace must contain at least one step");
        }
    }

    public SimulationMetadata metadata() { return metadata; }
    public List<SimulationStep> steps() { return steps; }
    public int size() { return steps.size(); }
    public boolean isEmpty() { return steps.isEmpty(); }
    public SimulationStep stepAt(int index) { return steps.get(index).copy(); }
    public SimulationStep firstStep() { return stepAt(0); }
    public SimulationStep lastStep() { return stepAt(steps.size() - 1); }
    public SimulationCursor cursor() { return new SimulationCursor(this, 0); }
}
