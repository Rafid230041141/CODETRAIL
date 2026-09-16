package dev.codetrail.desktop.simulation;

import javafx.scene.paint.Color;
import java.util.Objects;

/** Presentation colors; immutable trace statuses do not depend on the app theme. */
public final class SimulationPalette {
    static final SimulationPalette DARK = new SimulationPalette(true);
    static final SimulationPalette LIGHT = new SimulationPalette(false);

    private final boolean dark;
    private final Color background;
    private final Color text;
    private final Color secondaryText;
    private final Color outline;
    private final Color panel;
    private final Color header;
    private final Color nodeText = Color.web("#182235");

    private SimulationPalette(boolean dark) {
        this.dark = dark;
        background = Color.web(dark ? "#161A22" : "#F3F5F9");
        text = Color.web(dark ? "#EDF2FA" : "#182235");
        secondaryText = Color.web(dark ? "#B2BED0" : "#5F6C80");
        outline = Color.web(dark ? "#8798B2" : "#62718A");
        panel = Color.web(dark ? "#202631" : "#E9EEF6");
        header = Color.web(dark ? "#2C3442" : "#DFE5EF");
    }

    /** The visible key and Canvas use one presentation palette; trace data stays unchanged. */
    public static Color statusColor(SnapshotStatus status, boolean darkMode) {
        return (darkMode ? DARK : LIGHT).fill(status);
    }

    Color background() { return background; }
    Color text() { return text; }
    Color secondaryText() { return secondaryText; }
    Color outline() { return outline; }
    Color panel() { return panel; }
    Color header() { return header; }
    Color nodeText() { return nodeText; }

    Color fill(SnapshotStatus status) {
        Objects.requireNonNull(status, "status");
        return Color.web(switch (status) {
            case DEFAULT -> dark ? "#B2BED0" : "#DFE5EF";
            case ACTIVE -> "#F3C96A";
            case DONE -> "#A4D8C7";
            case REJECTED -> "#F0B4B0";
        });
    }

    /** Thin lines need darker semantic colors against the light canvas. */
    Color edge(SnapshotStatus status) {
        Objects.requireNonNull(status, "status");
        return Color.web(switch (status) {
            case DEFAULT -> dark ? "#8798B2" : "#62718A";
            case ACTIVE -> dark ? "#F3C96A" : "#865500";
            case DONE -> dark ? "#69DFC6" : "#087B70";
            case REJECTED -> dark ? "#FFA8A0" : "#B43E39";
        });
    }
}
