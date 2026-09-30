package application.client.learning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * FormulaBlock renders mathematical formulations, symbol breakdowns, and mathematical intuition.
 */
public class FormulaBlock implements LearningBlock {
    public record VariableInfo(String symbol, String name, String intuitionRole) {}

    private final String id;
    private final String title;
    private final String formulaText;
    private final List<VariableInfo> variables;
    private final String intuitionNote;

    public FormulaBlock(String id, String title, String formulaText, List<VariableInfo> variables, String intuitionNote) {
        this.id = id;
        this.title = title;
        this.formulaText = formulaText;
        this.variables = variables != null ? variables : List.of();
        this.intuitionNote = intuitionNote;
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
        VBox card = new VBox(14);
        card.setPadding(new Insets(16, 20, 16, 20));

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#121924" : "#f8fafc") + ";" +
                "-fx-border-color: " + (isDark ? "#1e2c3f" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView mathIconView = application.client.util.LucideIcons.icon("binary", 18, isDark);
        HBox mathIconBadge = new HBox(mathIconView);
        mathIconBadge.setAlignment(Pos.CENTER);
        mathIconBadge.setPadding(new Insets(3, 4, 3, 4));
        mathIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#064e3b" : "#d1fae5") + ";" +
                "-fx-border-color: " + (isDark ? "#059669" : "#10b981") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("MATHEMATICAL INTUITION: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #10b981; -fx-letter-spacing: 1.1px;");
        header.getChildren().addAll(mathIconBadge, heading);

        // Formula Display Card
        VBox formulaBox = new VBox(4);
        formulaBox.setAlignment(Pos.CENTER);
        formulaBox.setPadding(new Insets(12, 16, 12, 16));
        formulaBox.setStyle(
                "-fx-background-color: " + (isDark ? "#0a0f16" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#164e63" : "#bae6fd") + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );

        MathRendererView mathView = new MathRendererView(formulaText, isDark);
        formulaBox.getChildren().add(mathView);

        // Variable Breakdown Table
        VBox varsContainer = new VBox(6);
        Label varHeader = new Label("VARIABLE BREAKDOWN");
        varHeader.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + "; -fx-padding: 4 0 0 0;");
        varsContainer.getChildren().add(varHeader);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(4, 8, 4, 8));

        int row = 0;
        for (VariableInfo v : variables) {
            String symText = formatSymbol(v.symbol());
            Label sym = new Label(symText);
            sym.setStyle("-fx-font-family: 'JetBrains Mono', 'DejaVu Sans Mono', monospace; -fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #f59e0b;");

            Label name = new Label(v.name());
            name.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#e2e8f0" : "#1e293b") + ";");

            Label role = new Label(v.intuitionRole());
            role.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
            role.setWrapText(true);
            GridPane.setHgrow(role, Priority.ALWAYS);

            grid.add(sym, 0, row);
            grid.add(name, 1, row);
            grid.add(role, 2, row);
            row++;
        }
        varsContainer.getChildren().add(grid);

        // Intuition Callout Note
        if (intuitionNote != null && !intuitionNote.isBlank()) {
            VBox noteBox = new VBox(4);
            noteBox.setPadding(new Insets(10, 12, 10, 12));
            noteBox.setStyle(
                    "-fx-background-color: " + (isDark ? "#172554" : "#eff6ff") + ";" +
                    "-fx-border-color: " + (isDark ? "#1e40af" : "#bfdbfe") + ";" +
                    "-fx-border-radius: 6px; -fx-background-radius: 6px;"
            );

            Label noteTitle = new Label("Core Intuition");
            noteTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#93c5fd" : "#1d4ed8") + ";");

            Label noteText = new Label(intuitionNote);
            noteText.setWrapText(true);
            noteText.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#dbeafe" : "#1e3a8a") + "; -fx-line-spacing: 2px;");

            noteBox.getChildren().addAll(noteTitle, noteText);
            card.getChildren().addAll(header, formulaBox, varsContainer, noteBox);
        } else {
            card.getChildren().addAll(header, formulaBox, varsContainer);
        }

        return card;
    }

    public static String formatSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) return "";
        String s = rawSymbol.trim();
        s = s.replace("\\hat{y}_i", "ŷᵢ");
        s = s.replace("\\hat{y}", "ŷ");
        s = s.replace("y_hat_i", "ŷᵢ");
        s = s.replace("y_hat", "ŷ");
        s = s.replace("w_i", "wᵢ");
        s = s.replace("x_i", "xᵢ");
        s = s.replace("y_i", "yᵢ");
        s = s.replace("\\sigma(z)", "σ(z)");
        s = s.replace("\\sigma", "σ");
        s = s.replace("\\text{MSE}", "MSE");
        s = s.replace("\\", "");
        return s.trim();
    }
}
