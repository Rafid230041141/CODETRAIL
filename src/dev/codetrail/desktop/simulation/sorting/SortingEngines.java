package dev.codetrail.desktop.simulation.sorting;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit sorting-engine catalog for the parent simulation registry. */
public final class SortingEngines {
    private SortingEngines() {
    }

    public static List<SimulationEngine> all() {
        return List.of(
                new MergeSortEngine(),
                new QuickSortEngine(),
                new HeapSortEngine(),
                new CountingSortEngine(),
                new RadixSortEngine(),
                new BucketSortEngine());
    }
}
