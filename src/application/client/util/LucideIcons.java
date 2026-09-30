package application.client.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LucideIcons provides centralized access to the official Lucide icon set (lucide.dev).
 * Supports sizing, dark/light theme switching, and semantic status/category mappings.
 */
public final class LucideIcons {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    private static final String BASE_PATH = "/resources/images/lucide/";

    private LucideIcons() {}

    /**
     * Gets a cached JavaFX Image for the specified Lucide icon name and optional variant.
     */
    public static Image getImage(String name, String variant) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String cleanName = name.trim().toLowerCase(Locale.ROOT);
        String fileName = (variant != null && !variant.isBlank())
                ? cleanName + "-" + variant.trim().toLowerCase(Locale.ROOT) + ".png"
                : cleanName + ".png";

        return CACHE.computeIfAbsent(fileName, fn -> {
            try {
                String path = BASE_PATH + fn;
                InputStream is = LucideIcons.class.getResourceAsStream(path);
                if (is == null) {
                    // Fallback to base icon if variant is not found
                    path = BASE_PATH + cleanName + ".png";
                    is = LucideIcons.class.getResourceAsStream(path);
                }
                if (is != null) {
                    return new Image(is);
                }
            } catch (Throwable ignored) {
            }
            return null;
        });
    }

    /**
     * Creates an ImageView for the icon with default styling for dark backgrounds.
     */
    public static ImageView icon(String name, double size) {
        return icon(name, size, true);
    }

    /**
     * Creates an ImageView for the icon adapting to dark or light mode.
     */
    public static ImageView icon(String name, double size, boolean isDark) {
        String variant = isDark ? null : "dark";
        Image img = getImage(name, variant);
        return createImageView(img, size);
    }

    /**
     * Creates an ImageView for a specific colored variant (e.g. "green", "blue", "amber", "brand").
     */
    public static ImageView iconWithVariant(String name, String variant, double size) {
        Image img = getImage(name, variant);
        return createImageView(img, size);
    }

    private static ImageView createImageView(Image img, double size) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    // =========================================================================
    // 1. STATUS ICONS (Lesson Tree Items)
    // Completed: check-circle-2 | Current: play-circle | Not started: circle | Locked: lock | Skipped: circle-dashed
    // =========================================================================
    public static ImageView statusIcon(String status, boolean isDark) {
        if (status == null) status = "";
        String s = status.toLowerCase(Locale.ROOT);

        if (s.contains("complete") || s.contains("finish") || s.contains("done")) {
            return iconWithVariant("check-circle-2", "green", 15);
        } else if (s.contains("current") || s.contains("progress") || s.contains("todo") || s.contains("play")) {
            return iconWithVariant("play-circle", "blue", 15);
        } else if (s.contains("lock")) {
            return iconWithVariant("lock", "amber", 14);
        } else if (s.contains("skip") || s.contains("dash")) {
            return iconWithVariant("circle-dashed", "slate", 14);
        } else {
            return iconWithVariant("circle", "slate", 14);
        }
    }

    // =========================================================================
    // 2. TOP-LEVEL COURSE CARDS (Main Dashboard)
    // Languages: terminal | DSA: binary | Web: globe | App: smartphone | AI: brain-circuit | DS: bar-chart-3 | Game: gamepad-2
    // =========================================================================
    public static ImageView courseIcon(String slugOrTitle, double size) {
        if (slugOrTitle == null) slugOrTitle = "";
        String s = slugOrTitle.toLowerCase(Locale.ROOT);

        if (s.contains("dsa") || s.contains("algo") || s.contains("competitive")) {
            return iconWithVariant("binary", "brand", size);
        } else if (s.contains("web")) {
            return iconWithVariant("globe", "brand", size);
        } else if (s.contains("app") || s.contains("mobile") || s.contains("flutter") || s.contains("ios") || s.contains("android")) {
            return iconWithVariant("smartphone", "brand", size);
        } else if (s.contains("ai") || s.contains("ml") || s.contains("machine") || s.contains("intelligence") || s.contains("neural")) {
            return iconWithVariant("brain-circuit", "brand", size);
        } else if (s.contains("data") || s.contains("science") || s.contains("analytics")) {
            return iconWithVariant("bar-chart-3", "brand", size);
        } else if (s.contains("game")) {
            return iconWithVariant("gamepad-2", "brand", size);
        } else {
            return iconWithVariant("terminal", "brand", size);
        }
    }

    // =========================================================================
    // 3. CATEGORY / TOPIC ICONS (Sidebar Sections)
    // Languages: code-2 | DSA: braces | Data Structures: layers | Linear: rows-3 | Non-linear: network
    // Sorting: arrow-up-down | Searching: search | Graphs: share-2 | Trees: git-branch
    // Array: grid-3x3 | Linked List: link | Stack: layers-3 | Queue: list-ordered
    // =========================================================================
    public static ImageView categoryIcon(String titleOrKey, double size, boolean isDark) {
        if (titleOrKey == null) titleOrKey = "";
        String s = titleOrKey.toLowerCase(Locale.ROOT);

        String iconName;
        if (s.contains("sort")) {
            iconName = "arrow-up-down";
        } else if (s.contains("search")) {
            iconName = "search";
        } else if (s.contains("graph")) {
            iconName = "share-2";
        } else if (s.contains("tree")) {
            iconName = "git-branch";
        } else if (s.contains("array") || s.contains("matrix") || s.contains("grid")) {
            iconName = "grid-3x3";
        } else if (s.contains("link")) {
            iconName = "link";
        } else if (s.contains("stack")) {
            iconName = "layers-3";
        } else if (s.contains("queue")) {
            iconName = "list-ordered";
        } else if (s.contains("linear")) {
            iconName = "rows-3";
        } else if (s.contains("non-linear") || s.contains("network")) {
            iconName = "network";
        } else if (s.contains("structure")) {
            iconName = "layers";
        } else if (s.contains("dsa") || s.contains("algo") || s.contains("competitive")) {
            iconName = "braces";
        } else if (s.contains("web")) {
            iconName = "globe";
        } else if (s.contains("app") || s.contains("mobile")) {
            iconName = "smartphone";
        } else if (s.contains("ai") || s.contains("ml") || s.contains("deep")) {
            iconName = "brain-circuit";
        } else if (s.contains("data") || s.contains("pandas") || s.contains("numpy")) {
            iconName = "bar-chart-3";
        } else if (s.contains("game")) {
            iconName = "gamepad-2";
        } else if (s.contains("language") || s.contains("python") || s.contains("java") || s.contains("c++") || s.contains("rust")) {
            iconName = "code-2";
        } else {
            iconName = "list-tree";
        }

        return icon(iconName, size, isDark);
    }
}
