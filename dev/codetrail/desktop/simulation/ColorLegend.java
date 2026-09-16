package dev.codetrail.desktop.simulation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * One fixed legend for all renderer families. Values are CSS-style colors so
 * the same semantic status can be used by JavaFX and non-canvas consumers.
 */
public final class ColorLegend {
    private static final Map<SnapshotStatus, String> DEFAULT_COLORS = createDefaultColors();

    private ColorLegend() {
    }

    public static Map<SnapshotStatus, String> defaults() {
        return DEFAULT_COLORS;
    }

    public static String colorFor(SnapshotStatus status) {
        return DEFAULT_COLORS.get(Objects.requireNonNull(status, "status"));
    }

    private static Map<SnapshotStatus, String> createDefaultColors() {
        EnumMap<SnapshotStatus, String> colors = new EnumMap<>(SnapshotStatus.class);
        colors.put(SnapshotStatus.DEFAULT, "#94A3B8");
        colors.put(SnapshotStatus.ACTIVE, "#38BDF8");
        colors.put(SnapshotStatus.DONE, "#34D399");
        colors.put(SnapshotStatus.REJECTED, "#F87171");
        return Map.copyOf(colors);
    }
}
