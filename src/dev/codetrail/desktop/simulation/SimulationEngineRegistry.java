package dev.codetrail.desktop.simulation;

import dev.codetrail.desktop.simulation.graphs.TraversalEngines;
import dev.codetrail.desktop.simulation.graphs.structural.StructuralGraphEngines;
import dev.codetrail.desktop.simulation.graphs.weighted.WeightedGraphEngines;
import dev.codetrail.desktop.simulation.math.PriorityMathEngines;
import dev.codetrail.desktop.simulation.math.remaining.RemainingMathEngines;
import dev.codetrail.desktop.simulation.paradigms.ParadigmEngines;
import dev.codetrail.desktop.simulation.range.RangeTreeEngines;
import dev.codetrail.desktop.simulation.range.remaining.RemainingRangeEngines;
import dev.codetrail.desktop.simulation.searching.SearchingEngines;
import dev.codetrail.desktop.simulation.sorting.SortingEngines;
import dev.codetrail.desktop.simulation.strings.PriorityStringEngines;
import dev.codetrail.desktop.simulation.strings.remaining.RemainingStringEngines;
import dev.codetrail.desktop.simulation.structures.DsuHeapEngines;
import dev.codetrail.desktop.simulation.structures.linear.LinearStructureEngines;
import dev.codetrail.desktop.simulation.structures.trees.TreeMapEngines;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit type-to-engine registry. A missing type fails loudly; there is no
 * default or unrelated renderer/algorithm fallback.
 */
public final class SimulationEngineRegistry {
    private final Map<String, SimulationEngine> engines;

    public SimulationEngineRegistry(Collection<? extends SimulationEngine> engines) {
        Objects.requireNonNull(engines, "engines");
        LinkedHashMap<String, SimulationEngine> registered = new LinkedHashMap<>();
        for (SimulationEngine engine : engines) {
            registerInto(registered, engine);
        }
        if (registered.isEmpty()) {
            throw new IllegalArgumentException("at least one simulation engine must be registered");
        }
        this.engines = Map.copyOf(registered);
    }

    public static SimulationEngineRegistry defaultRegistry() {
        ArrayList<SimulationEngine> registered = new ArrayList<>();
        registered.add(new RecursionFactorialEngine());
        registered.addAll(SortingEngines.all());
        registered.addAll(SearchingEngines.all());
        registered.addAll(TraversalEngines.all());
        registered.addAll(RangeTreeEngines.all());
        registered.addAll(DsuHeapEngines.all());
        registered.addAll(PriorityStringEngines.all());
        registered.addAll(PriorityMathEngines.all());
        registered.addAll(WeightedGraphEngines.all());
        registered.addAll(StructuralGraphEngines.all());
        registered.addAll(LinearStructureEngines.all());
        registered.addAll(TreeMapEngines.all());
        registered.addAll(RemainingRangeEngines.all());
        registered.addAll(RemainingStringEngines.all());
        registered.addAll(RemainingMathEngines.all());
        registered.addAll(ParadigmEngines.all());
        return new SimulationEngineRegistry(registered);
    }

    public SimulationEngine require(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("simulation type must be nonblank");
        }
        SimulationEngine engine = engines.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("no simulation engine registered for type: " + type);
        }
        return engine;
    }

    public SimulationEngine get(String type) {
        return require(type);
    }

    public Set<String> registeredTypes() {
        return engines.keySet();
    }

    /** Return a new registry with one explicitly registered engine. */
    public SimulationEngineRegistry withEngine(SimulationEngine engine) {
        LinkedHashMap<String, SimulationEngine> next = new LinkedHashMap<>(engines);
        registerInto(next, engine);
        return new SimulationEngineRegistry(next.values());
    }

    public static Builder builder() {
        return new Builder();
    }

    private static void registerInto(Map<String, SimulationEngine> destination, SimulationEngine engine) {
        Objects.requireNonNull(engine, "engine");
        SimulationMetadata metadata = Objects.requireNonNull(engine.metadata(), "engine.metadata()");
        String type = metadata.type();
        if (destination.putIfAbsent(type, engine) != null) {
            throw new IllegalArgumentException("duplicate simulation type: " + type);
        }
    }

    public static final class Builder {
        private final LinkedHashMap<String, SimulationEngine> engines = new LinkedHashMap<>();

        public Builder register(SimulationEngine engine) {
            registerInto(engines, engine);
            return this;
        }

        public SimulationEngineRegistry build() {
            return new SimulationEngineRegistry(engines.values());
        }
    }
}
