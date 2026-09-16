package dev.codetrail.desktop.simulation.strings.remaining;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the remaining bounded string simulations. */
public final class RemainingStringEngines {
    private RemainingStringEngines() {
    }

    /** Preserve the curriculum order: suffix array, suffix automaton, then hashing. */
    public static List<SimulationEngine> all() {
        return List.of(
                new SuffixArrayEngine(),
                new SuffixAutomatonEngine(),
                new StringHashingEngine());
    }
}
