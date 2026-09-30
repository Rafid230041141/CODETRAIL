package application.client.learning;

import javafx.scene.Node;

/**
 * Common contract for modular learning blocks composed into course-specific workflows.
 */
public interface LearningBlock {
    /**
     * Unique identifier of this block instance.
     */
    String id();

    /**
     * Display title of this learning block.
     */
    String title();

    /**
     * Educational category this block belongs to.
     */
    CourseCategory category();

    /**
     * Renders this block into a JavaFX Node styled for dark/light theme.
     */
    Node render(boolean isDark);

    /**
     * Called when the block is being unloaded or replaced to clean up resources.
     */
    default void dispose() {}
}
