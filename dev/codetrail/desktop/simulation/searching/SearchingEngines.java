package dev.codetrail.desktop.simulation.searching;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit searching-family factory for later registry composition. */
public final class SearchingEngines {
    private SearchingEngines() {
    }

    public static List<SimulationEngine> all() {
        return List.of(
                new LinearSearchEngine(),
                new BinarySearchEngine(),
                new BinarySearchAnswerEngine());
    }
}
