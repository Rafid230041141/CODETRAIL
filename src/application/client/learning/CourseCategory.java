package application.client.learning;

import java.util.Locale;

/**
 * CourseCategory represents the educational domain of a topic or curriculum module.
 * Enables course-type aware learning flows tailored to specific educational domains
 * while preserving the standard coding/competitive flow for Languages and DSA.
 */
public enum CourseCategory {
    LANGUAGES("Languages & Core Runtimes", "", "#38bdf8"),
    DSA("Data Structures & Algorithms", "", "#818cf8"),
    AI_ML("Artificial Intelligence & Machine Learning", "", "#10b981"),
    DATA_SCIENCE("Data Science & Analytics", "", "#f59e0b"),
    WEB_DEV("Web Development & Architecture", "", "#06b6d4"),
    APP_DEV("Mobile Application Development", "", "#a855f7"),
    GAME_DEV("Game Development & Graphics", "", "#ec4899");

    private final String displayName;
    private final String icon;
    private final String accentColor;

    CourseCategory(String displayName, String icon, String accentColor) {
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

    public String accentColor() {
        return accentColor;
    }

    public boolean isLanguages() {
        return this == LANGUAGES;
    }

    public boolean isDsa() {
        return this == DSA;
    }

    public boolean isLanguagesOrDsa() {
        return this == LANGUAGES || this == DSA;
    }

    public boolean isAiMl() {
        return this == AI_ML;
    }

    public boolean isDataScience() {
        return this == DATA_SCIENCE;
    }

    public boolean isWebDev() {
        return this == WEB_DEV;
    }

    public boolean isAppDev() {
        return this == APP_DEV;
    }

    public boolean isGameDev() {
        return this == GAME_DEV;
    }

    /**
     * Identifies the CourseCategory from a canonical topic key.
     */
    public static CourseCategory fromTopicKey(String topicKey) {
        if (topicKey == null || topicKey.isBlank()) {
            return LANGUAGES;
        }

        String k = topicKey.trim().toLowerCase(Locale.ROOT);

        // 1. AI / ML Topics
        switch (k) {
            case "ml_foundations":
            case "math_ai":
            case "scikit":
            case "deep_learning":
            case "vision":
            case "nlp":
            case "genai":
            case "mlops":
                return AI_ML;
        }

        // 2. Data Science Topics
        switch (k) {
            case "numpy":
            case "pandas":
            case "eda":
            case "statistics":
            case "feature_eng":
            case "bigdata":
            case "sql_analytics":
            case "bi_dashboards":
                return DATA_SCIENCE;
        }

        // 3. Web Dev Topics
        switch (k) {
            case "html5":
            case "css3":
            case "react":
            case "node":
            case "database":
            case "auth":
            case "deploy":
                return WEB_DEV;
        }

        // 4. App Dev Topics
        switch (k) {
            case "flutter":
            case "reactnative":
            case "kotlin":
            case "swift":
            case "statemgmt":
            case "mobileapi":
            case "sqlite":
            case "publish":
                return APP_DEV;
        }

        // 5. Game Dev Topics
        switch (k) {
            case "math_games":
            case "pygame":
            case "unity_basics":
            case "unity_3d":
            case "unreal":
            case "game_physics":
            case "audio_vfx":
            case "game_publish":
                return GAME_DEV;
        }

        // 6. DSA Topics
        switch (k) {
            case "arrays":
            case "linked lists":
            case "stacks":
            case "queues":
            case "hash maps":
            case "heaps":
            case "trees":
            case "dsu":
            case "trie":
            case "sorting algorithms":
            case "searching":
            case "graphs":
            case "range queries":
            case "algorithmic paradigms":
            case "string algorithms":
            case "mathematics":
                return DSA;
        }

        // Special case for javascript: check if it's the web dev topic or language topic
        if (k.equals("javascript")) {
            return WEB_DEV;
        }

        // Fallback for languages
        return LANGUAGES;
    }

    /**
     * Resolves CourseCategory from lesson navigation path, title, and topic ID.
     */
    public static CourseCategory fromPathAndTitle(String path, String title, Long topicId) {
        String p = (path == null ? "" : path).toLowerCase(Locale.ROOT);
        String t = (title == null ? "" : title).toLowerCase(Locale.ROOT);

        // Language course paths
        if (p.contains("language") || (topicId != null && topicId >= 1 && topicId <= 10)) {
            return LANGUAGES;
        }

        // DSA course paths
        if (p.contains("dsa") || p.contains("data structure") || p.contains("algorithm") || (topicId != null && topicId >= 11 && topicId <= 26)) {
            return DSA;
        }

        // AI / ML paths
        if (p.contains("artificial intelligence") || p.contains("ai") || p.contains("machine learning") || p.contains("deep learning") || (topicId != null && topicId >= 35 && topicId <= 42)) {
            return AI_ML;
        }

        // Data Science paths
        if (p.contains("data science") || p.contains("science") || p.contains("analytics") || (topicId != null && topicId >= 43 && topicId <= 50)) {
            return DATA_SCIENCE;
        }

        // Web Dev paths
        if (p.contains("web development") || p.contains("web") || p.contains("frontend") || p.contains("backend") || (topicId != null && topicId >= 27 && topicId <= 34)) {
            return WEB_DEV;
        }

        // App Dev paths
        if (p.contains("app development") || p.contains("app") || p.contains("mobile") || (topicId != null && topicId >= 51 && topicId <= 58)) {
            return APP_DEV;
        }

        // Game Dev paths
        if (p.contains("game development") || p.contains("game") || (topicId != null && topicId >= 59 && topicId <= 66)) {
            return GAME_DEV;
        }

        // Default to LANGUAGES if undetermined
        return LANGUAGES;
    }
}
