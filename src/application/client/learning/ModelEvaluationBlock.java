package application.client.learning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * ModelEvaluationBlock renders model quality evaluation benchmarks (MSE, R², MAE, Accuracy).
 */
public class ModelEvaluationBlock implements LearningBlock {
    public record MetricItem(String name, String value, String benchmark, boolean passed) {}

    private final String id;
    private final String title;
    private final List<MetricItem> metrics;
    private final String diagnostics;

    public ModelEvaluationBlock(String id, String title, List<MetricItem> metrics, String diagnostics) {
        this.id = id;
        this.title = title;
        this.metrics = metrics != null ? metrics : List.of();
        this.diagnostics = diagnostics;
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
        return CourseCategory.AI_ML;
    }

    @Override
    public Node render(boolean isDark) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16, 20, 16, 20));

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#121922" : "#f8fafc") + ";" +
                "-fx-border-color: " + (isDark ? "#202e3e" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView actIconView = application.client.util.LucideIcons.icon("activity", 18, isDark);
        HBox actIconBadge = new HBox(actIconView);
        actIconBadge.setAlignment(Pos.CENTER);
        actIconBadge.setPadding(new Insets(3, 4, 3, 4));
        actIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#064e3b" : "#d1fae5") + ";" +
                "-fx-border-color: " + (isDark ? "#059669" : "#10b981") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("MODEL EVALUATION: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #10b981; -fx-letter-spacing: 1.1px;");
        header.getChildren().addAll(actIconBadge, heading);

        // Metrics Grid/Row
        HBox metricsRow = new HBox(12);
        metricsRow.setAlignment(Pos.CENTER_LEFT);

        for (MetricItem m : metrics) {
            VBox mBox = new VBox(4);
            mBox.setAlignment(Pos.CENTER);
            mBox.setPadding(new Insets(10, 14, 10, 14));
            HBox.setHgrow(mBox, Priority.ALWAYS);

            mBox.setStyle(
                    "-fx-background-color: " + (isDark ? "#0d131a" : "#ffffff") + ";" +
                    "-fx-border-color: " + (isDark ? "#1e293b" : "#cbd5e1") + ";" +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px;"
            );

            Label nameLbl = new Label(m.name());
            nameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

            Label valLbl = new Label(m.value());
            valLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: " + (m.passed() ? "#10b981" : "#f59e0b") + ";");

            Label benchLbl = new Label(m.benchmark());
            benchLbl.setStyle(
                    "-fx-font-size: 9.5px; -fx-font-weight: 700; " +
                    "-fx-text-fill: " + (m.passed() ? "#34d399" : "#fbbf24") + "; " +
                    "-fx-background-color: " + (m.passed() ? (isDark ? "#064e3b" : "#d1fae5") : (isDark ? "#78350f" : "#fef3c7")) + "; " +
                    "-fx-padding: 2 6; -fx-background-radius: 4;"
            );

            mBox.getChildren().addAll(nameLbl, valLbl, benchLbl);
            metricsRow.getChildren().add(mBox);
        }

        // Diagnostics
        if (diagnostics != null && !diagnostics.isBlank()) {
            VBox diagBox = new VBox(4);
            diagBox.setPadding(new Insets(8, 12, 8, 12));
            diagBox.setStyle(
                    "-fx-background-color: " + (isDark ? "#0e1726" : "#f1f5f9") + ";" +
                    "-fx-border-color: " + (isDark ? "#1e293b" : "#e2e8f0") + ";" +
                    "-fx-border-radius: 6px; -fx-background-radius: 6px;"
            );

            Label diagTitle = new Label("Evaluation Diagnostics");
            diagTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

            Label diagText = new Label(diagnostics);
            diagText.setWrapText(true);
            diagText.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + "; -fx-line-spacing: 2px;");

            diagBox.getChildren().addAll(diagTitle, diagText);
            card.getChildren().addAll(header, metricsRow, diagBox);
        } else {
            card.getChildren().addAll(header, metricsRow);
        }

        return card;
    }
}
