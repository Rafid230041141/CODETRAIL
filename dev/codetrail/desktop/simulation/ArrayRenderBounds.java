package dev.codetrail.desktop.simulation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Fixed slot counts and original duplicate identities for a complete array trace. */
public final class ArrayRenderBounds {
    private final String simulationType;
    private final int maximumCellCount;
    private final Set<String> duplicateValues;
    private final Map<Integer, Integer> bucketCapacities;

    private ArrayRenderBounds(String simulationType, int maximumCellCount, Set<String> duplicateValues,
            Map<Integer, Integer> bucketCapacities) {
        this.simulationType = simulationType;
        this.maximumCellCount = maximumCellCount;
        this.duplicateValues = Set.copyOf(duplicateValues);
        this.bucketCapacities = Map.copyOf(bucketCapacities);
    }

    public static ArrayRenderBounds fromTrace(SimulationTrace trace) {
        Objects.requireNonNull(trace, "trace");
        int maximum = 0;
        Map<String, Set<String>> identities = new LinkedHashMap<>();
        Map<Integer, Integer> capacities = new LinkedHashMap<>();
        for (SimulationStep step : trace.steps()) {
            if (!(step.stateSnapshot() instanceof ArrayState array)) continue;
            maximum = Math.max(maximum, array.cells().size());
            for (TypedCell cell : array.cells())
                identities.computeIfAbsent(cell.value(), ignored -> new LinkedHashSet<>()).add(cell.key());
            for (Fact fact : array.facts()) if (fact.key().equals("buckets")) {
                for (String entry : fact.value().split(";\\s*")) {
                    String[] parts = entry.split(":", 2);
                    if (parts.length != 2) continue;
                    String values = parts[1].trim();
                    int count = values.equals("[]") ? 0 : values.split(",").length;
                    capacities.merge(Integer.parseInt(parts[0]), count, Math::max);
                }
            }
        }
        Set<String> duplicates = new LinkedHashSet<>();
        identities.forEach((value, keys) -> { if (keys.size() > 1) duplicates.add(value); });
        return new ArrayRenderBounds(trace.metadata().type(), maximum, duplicates, capacities);
    }

    public String simulationType() { return simulationType; }
    public int maximumCellCount() { return maximumCellCount; }
    public boolean hasDuplicateValue(String value) { return duplicateValues.contains(value); }
    public int bucketCapacity(int index) { return bucketCapacities.getOrDefault(index, 0); }
}
