package dev.codetrail.desktop.simulation.graphs.structural;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit factory for the bounded structural graph family. */
public final class StructuralGraphEngines {
    private StructuralGraphEngines() {
    }

    /** Stable curriculum order: topological sort, SCC, bridges, then max flow. */
    public static List<SimulationEngine> all() {
        return List.of(
                new TopologicalSortEngine(),
                new TarjanSccEngine(),
                new BridgesArticulationEngine(),
                new MaxFlowEngine());
    }
}
