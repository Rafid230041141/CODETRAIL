package application.client.learning;

import java.util.Locale;

/**
 * PracticeEnvironment defines the specialized interactive learning environment
 * tailored to each course category and lesson type.
 */
public enum PracticeEnvironment {
    COMPILER("Code Compiler & Runner", "terminal", "#38bdf8"),
    ALGORITHM_LAB("Algorithm Coding Lab", "braces", "#818cf8"),
    ML_LAB("ML Lab & Python Studio", "brain-circuit", "#10b981"),
    DATA_SCIENCE_LAB("Data Science Notebook Lab", "bar-chart-3", "#f59e0b"),
    WEB_PREVIEW("Web Studio & Live Preview", "globe", "#06b6d4"),
    APP_PREVIEW("Mobile App Simulator", "smartphone", "#a855f7"),
    GAME_PREVIEW("Game Canvas & Physics Studio", "gamepad-2", "#ec4899"),
    THEORY_ONLY("Conceptual & Intuition Focus", "list-tree", "#64748b");

    private final String displayName;
    private final String icon;
    private final String accentColor;

    PracticeEnvironment(String displayName, String icon, String accentColor) {
        this.displayName = displayName;
        this.icon = icon;
        this.accentColor = accentColor;
    }

    public String displayName() {
        return displayName;
    }

    public String icon() {
        return icon;
    }

    public String iconName() {
        return icon;
    }

    public javafx.scene.Node iconGraphic(double size, boolean isDark) {
        return application.client.util.LucideIcons.icon(icon, size, isDark);
    }

    public String accentColor() {
        return accentColor;
    }

    public boolean isTheoryOnly() {
        return this == THEORY_ONLY;
    }

    public boolean isCompiler() {
        return this == COMPILER;
    }

    public boolean isAlgorithmLab() {
        return this == ALGORITHM_LAB;
    }

    public boolean isMlLab() {
        return this == ML_LAB;
    }

    public boolean isDataScienceLab() {
        return this == DATA_SCIENCE_LAB;
    }

    public boolean isWebPreview() {
        return this == WEB_PREVIEW;
    }

    public boolean isAppPreview() {
        return this == APP_PREVIEW;
    }

    public boolean isGamePreview() {
        return this == GAME_PREVIEW;
    }

    /**
     * Resolves the appropriate PracticeEnvironment given the CourseCategory, lesson title, and code availability.
     */
    public static PracticeEnvironment forCategory(CourseCategory category, String title, String code) {
        if (category == null) {
            return COMPILER;
        }

        // Languages and DSA always use their dedicated compiler/algorithm environments
        if (category == CourseCategory.LANGUAGES) {
            return COMPILER;
        }
        if (category == CourseCategory.DSA) {
            return ALGORITHM_LAB;
        }

        // Check if this is a theory-only lesson without code execution requirements
        boolean hasCode = code != null && !code.trim().isBlank();
        String t = title != null ? title.toLowerCase(Locale.ROOT) : "";

        boolean isTheoryTopic = t.startsWith("what is") || t.startsWith("introduction to")
                || t.startsWith("overview") || t.contains("concept") || t.contains("foundation")
                || t.contains("architecture overview") || t.contains("theory");

        if (!hasCode && isTheoryTopic) {
            return THEORY_ONLY;
        }

        return switch (category) {
            case AI_ML -> ML_LAB;
            case DATA_SCIENCE -> DATA_SCIENCE_LAB;
            case WEB_DEV -> WEB_PREVIEW;
            case APP_DEV -> APP_PREVIEW;
            case GAME_DEV -> GAME_PREVIEW;
            default -> COMPILER;
        };
    }
}
