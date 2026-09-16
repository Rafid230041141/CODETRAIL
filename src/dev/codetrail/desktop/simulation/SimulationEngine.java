package dev.codetrail.desktop.simulation;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Algorithm-specific producer for a complete immutable visualization trace.
 * Implementations validate their own input shape and bounds.
 */
public interface SimulationEngine {
    SimulationMetadata metadata();

    List<SimulationStep> generateSteps(JsonNode input);

    default SimulationTrace generateTrace(JsonNode input) {
        return new SimulationTrace(metadata(), generateSteps(input));
    }

    default String type() {
        return metadata().type();
    }
}
