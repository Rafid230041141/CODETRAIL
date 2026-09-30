package application.client.learning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * UnderstandingCheckBlock provides interactive conceptual checks with instant feedback and explanations.
 */
public class UnderstandingCheckBlock implements LearningBlock {
    public record Option(String letter, String text, boolean isCorrect, String explanation) {}

    private final String id;
    private final String title;
    private final CourseCategory category;
    private final String questionText;
    private final List<Option> options;

    public UnderstandingCheckBlock(String id, String title, CourseCategory category, String questionText, List<Option> options) {
        this.id = id;
        this.title = title;
        this.category = category != null ? category : CourseCategory.AI_ML;
        this.questionText = questionText;
        this.options = options != null ? options : List.of();
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

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#131b26" : "#f8fafc") + ";" +
                "-fx-border-color: " + (isDark ? "#243346" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView helpIconView = application.client.util.LucideIcons.icon("help-circle", 18, isDark);
        HBox helpIconBadge = new HBox(helpIconView);
        helpIconBadge.setAlignment(Pos.CENTER);
        helpIconBadge.setPadding(new Insets(3, 4, 3, 4));
        helpIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#2e1065" : "#f3e8ff") + ";" +
                "-fx-border-color: " + (isDark ? "#7c3aed" : "#a855f7") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("CHECK YOUR UNDERSTANDING");
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #a855f7; -fx-letter-spacing: 1.1px;");
        header.getChildren().addAll(helpIconBadge, heading);

        // Question Prompt
        Label qLabel = new Label(questionText);
        qLabel.setWrapText(true);
        qLabel.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#f1f5f9" : "#0f172a") + "; -fx-line-spacing: 3px;");

        // Options List
        VBox optionsContainer = new VBox(8);
        VBox feedbackBox = new VBox(6);
        feedbackBox.setVisible(false);
        feedbackBox.setManaged(false);

        Label feedbackStatus = new Label();
        feedbackStatus.setStyle("-fx-font-weight: 800; -fx-font-size: 12px;");

        Label feedbackDetail = new Label();
        feedbackDetail.setWrapText(true);
        feedbackDetail.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2px;");

        feedbackBox.getChildren().addAll(feedbackStatus, feedbackDetail);

        List<Button> optionButtons = new ArrayList<>();

        for (Option opt : options) {
            Button btn = new Button(opt.letter() + ".  " + opt.text());
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setWrapText(true);

            String baseStyle =
                    "-fx-background-color: " + (isDark ? "#1a2332" : "#ffffff") + ";" +
                    "-fx-border-color: " + (isDark ? "#2b394f" : "#cbd5e1") + ";" +
                    "-fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                    "-fx-padding: 9 14; -fx-font-size: 12.5px; -fx-text-fill: " + (isDark ? "#d5deeb" : "#334155") + ";" +
                    "-fx-cursor: hand;";
            btn.setStyle(baseStyle);

            btn.setOnAction(e -> {
                // Reset styling on all buttons
                for (Button b : optionButtons) {
                    b.setStyle(baseStyle);
                }

                feedbackBox.setVisible(true);
                feedbackBox.setManaged(true);

                if (opt.isCorrect()) {
                    btn.setStyle(
                            "-fx-background-color: " + (isDark ? "#064e3b" : "#ecfdf5") + ";" +
                            "-fx-border-color: #10b981; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                            "-fx-padding: 9 14; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#6ee7b7" : "#065f46") + ";"
                    );
                    feedbackBox.setStyle(
                            "-fx-background-color: " + (isDark ? "#064e3b" : "#ecfdf5") + ";" +
                            "-fx-border-color: #10b981; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 14;"
                    );
                    feedbackStatus.setText("✓ Correct!");
                    feedbackStatus.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #10b981;");
                    feedbackDetail.setText(opt.explanation());
                    feedbackDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#a7f3d0" : "#047857") + ";");
                } else {
                    btn.setStyle(
                            "-fx-background-color: " + (isDark ? "#450a0a" : "#fef2f2") + ";" +
                            "-fx-border-color: #ef4444; -fx-border-width: 1.5px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                            "-fx-padding: 9 14; -fx-font-size: 12.5px; -fx-text-fill: " + (isDark ? "#fca5a5" : "#991b1b") + ";"
                    );
                    feedbackBox.setStyle(
                            "-fx-background-color: " + (isDark ? "#450a0a" : "#fef2f2") + ";" +
                            "-fx-border-color: #ef4444; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 10 14;"
                    );
                    feedbackStatus.setText("✗ Not quite — Let's think carefully:");
                    feedbackStatus.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #ef4444;");
                    feedbackDetail.setText(opt.explanation());
                    feedbackDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#fecaca" : "#b91c1c") + ";");
                }
            });

            optionButtons.add(btn);
            optionsContainer.getChildren().add(btn);
        }

        card.getChildren().addAll(header, qLabel, optionsContainer, feedbackBox);
        return card;
    }
}
