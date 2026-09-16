package dev.codetrail.desktop.simulation.structures.linear;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Ordered factory for the bounded linear data-structure providers. */
public final class LinearStructureEngines {
    private LinearStructureEngines() {
    }

    public static List<SimulationEngine> all() {
        return List.of(
                new ArrayEngine(),
                new LinkedListEngine(),
                new StackEngine(),
                new QueueEngine(),
                new GraphRepresentationEngine());
    }
}
