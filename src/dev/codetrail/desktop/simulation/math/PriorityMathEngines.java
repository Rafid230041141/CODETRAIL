package dev.codetrail.desktop.simulation.math;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the curriculum's mathematics simulations. */
public final class PriorityMathEngines {
    private PriorityMathEngines() {
    }

    /** Sieve precedes extended Euclid to preserve the curriculum order. */
    public static List<SimulationEngine> all() {
        return List.of(new SieveEngine(), new ExtendedGcdEngine());
    }
}
