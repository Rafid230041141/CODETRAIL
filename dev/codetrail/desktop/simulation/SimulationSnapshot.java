package dev.codetrail.desktop.simulation;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable render boundary for one point in a simulation trace. Active IDs
 * are explicit so graph-like renderers never have to infer activity from
 * presentation details.
 */
public final class SimulationSnapshot {
    private final SimulationState state;
    private final Set<String> activeNodeIds;
    private final Set<String> activeEdgeIds;
    private final Map<SnapshotStatus, String> colorLegend;

    public SimulationSnapshot(
            SimulationState state,
            Set<String> activeNodeIds,
            Set<String> activeEdgeIds) {
        this(state, activeNodeIds, activeEdgeIds, ColorLegend.defaults());
    }

    public SimulationSnapshot(
            SimulationState state,
            Set<String> activeNodeIds,
            Set<String> activeEdgeIds,
            Map<SnapshotStatus, String> colorLegend) {
        this.state = Objects.requireNonNull(state, "state").copy();
        this.activeNodeIds = copyIds(activeNodeIds, "activeNodeIds");
        this.activeEdgeIds = copyIds(activeEdgeIds, "activeEdgeIds");
        Objects.requireNonNull(colorLegend, "colorLegend");
        if (!colorLegend.equals(ColorLegend.defaults())) {
            throw new IllegalArgumentException("colorLegend must use the fixed default status colors");
        }
        this.colorLegend = ColorLegend.defaults();
    }

    public static SimulationSnapshot of(SimulationState state, Set<String> activeNodeIds, Set<String> activeEdgeIds) {
        return new SimulationSnapshot(state, activeNodeIds, activeEdgeIds);
    }

    public SimulationState state() { return state; }
    public SimulationState stateSnapshot() { return state.copy(); }
    public Set<String> activeNodeIds() { return activeNodeIds; }
    public Set<String> activeEdgeIds() { return activeEdgeIds; }
    public Map<SnapshotStatus, String> colorLegend() { return colorLegend; }

    public SimulationSnapshot copy() {
        return new SimulationSnapshot(state, activeNodeIds, activeEdgeIds, colorLegend);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SimulationSnapshot snapshot)) return false;
        return state.equals(snapshot.state)
                && activeNodeIds.equals(snapshot.activeNodeIds)
                && activeEdgeIds.equals(snapshot.activeEdgeIds)
                && colorLegend.equals(snapshot.colorLegend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, activeNodeIds, activeEdgeIds, colorLegend);
    }

    private static Set<String> copyIds(Set<String> ids, String name) {
        Objects.requireNonNull(ids, name);
        if (ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException(name + " must contain nonblank IDs");
        }
        return Set.copyOf(ids);
    }
}
