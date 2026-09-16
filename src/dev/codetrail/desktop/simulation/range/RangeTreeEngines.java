package dev.codetrail.desktop.simulation.range;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the range-query simulation family. */
public final class RangeTreeEngines {
    private RangeTreeEngines() {
    }

    /** Segment tree is the representative boundary, followed by BIT and lazy propagation. */
    public static List<SimulationEngine> all() {
        return List.of(
                new SegmentTreeEngine(),
                new FenwickTreeEngine(),
                new LazySegmentTreeEngine());
    }
}
