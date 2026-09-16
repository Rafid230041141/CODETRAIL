package dev.codetrail.desktop.simulation.graphs;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit factory for the ordered graph traversal family. */
public final class TraversalEngines {
    private TraversalEngines() {
    }

    /** BFS is intentionally first so callers can validate the shared boundary before DFS. */
    public static List<SimulationEngine> all() {
        return List.of(new BfsEngine(), new DfsEngine());
    }
}
