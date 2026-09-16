package dev.codetrail.desktop.simulation.graphs.weighted;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the bounded weighted graph simulation family. */
public final class WeightedGraphEngines {
    private WeightedGraphEngines() {
    }

    /** Shortest paths precede all-pairs paths, followed by the two MST views. */
    public static List<SimulationEngine> all() {
        return List.of(
                new DijkstraEngine(),
                new BellmanFordEngine(),
                new FloydWarshallEngine(),
                new KruskalMstEngine(),
                new PrimMstEngine());
    }
}
