package dev.codetrail.desktop.simulation.range.remaining;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the remaining range-query simulations. */
public final class RemainingRangeEngines {
    private RemainingRangeEngines() {
    }

    /** Return all five remaining engines in the same order as the curriculum. */
    public static List<SimulationEngine> all() {
        return List.of(
                new PrefixSumEngine(),
                new MoRangeQueryEngine(),
                new OnlineRangeQueryEngine(),
                new SparseTableEngine(),
                new SqrtDecompositionEngine());
    }
}
