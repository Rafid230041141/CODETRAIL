package dev.codetrail.desktop.simulation.strings;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the priority string simulation family. */
public final class PriorityStringEngines {
    private PriorityStringEngines() {
    }

    /** KMP precedes Z-function to preserve the curriculum order. */
    public static List<SimulationEngine> all() {
        return List.of(new KmpEngine(), new ZFunctionEngine());
    }
}
