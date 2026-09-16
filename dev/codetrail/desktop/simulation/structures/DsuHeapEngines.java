package dev.codetrail.desktop.simulation.structures;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Ordered factory for the DSU and heap structure simulation providers. */
public final class DsuHeapEngines {
    private DsuHeapEngines() {
    }

    /** DSU precedes Heap to preserve the curriculum build order. */
    public static List<SimulationEngine> all() {
        return List.of(new DsuEngine(), new HeapEngine());
    }
}
