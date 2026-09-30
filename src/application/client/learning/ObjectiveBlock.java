package application.client.learning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.util.List;

/**
 * ObjectiveBlock renders a structured learning goal card with target competencies and milestones.
 */
public class ObjectiveBlock implements LearningBlock {
    private final String id;
    private final String title;
    private final CourseCategory category;
    private final String primaryGoal;
    private final List<String> milestones;
    private final String skillLevel;

    public ObjectiveBlock(String id, String title, CourseCategory category, String primaryGoal, List<String> milestones, String skillLevel) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.primaryGoal = primaryGoal;
        this.milestones = milestones != null ? milestones : List.of();
        this.skillLevel = skillLevel != null ? skillLevel : "Core Foundation";
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public CourseCategory category() {
        return category;
    }

    @Override
    public Node render(boolean isDark) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));
        String accent = category.accentColor();

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#141c26" : "#f8fafc") + ";" +
                "-fx-border-color: " + (isDark ? "#233245" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header Row
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        ImageView iconView = application.client.util.LucideIcons.icon("target", 18, isDark);

        HBox iconBadge = new HBox(iconView);
        iconBadge.setAlignment(Pos.CENTER);
        iconBadge.setPadding(new Insets(3, 4, 3, 4));
        iconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#1e293b" : "#e2e8f0") + ";" +
                "-fx-border-color: " + (isDark ? "#334155" : "#cbd5e1") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label tag = new Label("LEARNING OBJECTIVE");
        tag.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + accent + "; -fx-letter-spacing: 1.2px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label levelBadge = new Label(skillLevel);
        levelBadge.setStyle(
                "-fx-font-size: 10.5px; -fx-font-weight: 700; " +
                "-fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + "; " +
                "-fx-background-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        headerRow.getChildren().addAll(iconBadge, tag, spacer, levelBadge);

        // Goal Title / Statement
        Label goalLabel = new Label(primaryGoal);
        goalLabel.setWrapText(true);
        goalLabel.setStyle(
                "-fx-font-size: 14.5px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + ";" +
                "-fx-line-spacing: 3px;"
        );

        // Milestones
        VBox milestonesBox = new VBox(7);
        milestonesBox.setPadding(new Insets(4, 0, 0, 8));

        for (String milestone : milestones) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.TOP_LEFT);

            Label checkIcon = new Label("✓");
            checkIcon.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + accent + "; -fx-padding: 2 0 0 0;");

            Label itemText = new Label(milestone);
            itemText.setWrapText(true);
            itemText.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + "; -fx-line-spacing: 2px;");

            itemRow.getChildren().addAll(checkIcon, itemText);
            milestonesBox.getChildren().add(itemRow);
        }

        card.getChildren().addAll(headerRow, goalLabel, milestonesBox);
        return card;
    }
}
