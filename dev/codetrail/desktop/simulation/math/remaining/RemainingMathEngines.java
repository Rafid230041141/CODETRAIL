package dev.codetrail.desktop.simulation.math.remaining;

import dev.codetrail.desktop.simulation.SimulationEngine;
import java.util.List;

/** Explicit ordered factory for the remaining mathematics simulations. */
public final class RemainingMathEngines {
    private RemainingMathEngines() {
    }

    /** Keep the order aligned with the mathematics lessons in the curriculum. */
    public static List<SimulationEngine> all() {
        return List.of(
                new ModularExponentiationEngine(),
                new PascalTriangleEngine(),
                new ConvexHullEngine(),
                new GaussianEliminationEngine());
    }
}
