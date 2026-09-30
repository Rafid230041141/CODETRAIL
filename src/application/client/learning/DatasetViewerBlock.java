package application.client.learning;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * DatasetViewerBlock provides an interactive tabular view of real sample datasets,
 * column schema definitions, missing value alerts, and summary statistics.
 */
public class DatasetViewerBlock implements LearningBlock {
    public record ColumnSchema(String name, String dtype, int nullCount, String notes) {}

    private final String id;
    private final String title;
    private final String datasetName;
    private final int totalRows;
    private final List<String> columnHeaders;
    private final List<List<String>> sampleRows;
    private final List<ColumnSchema> schemas;
    private final String cleaningGuide;

    public DatasetViewerBlock(
            String id,
            String title,
            String datasetName,
            int totalRows,
            List<String> columnHeaders,
            List<List<String>> sampleRows,
            List<ColumnSchema> schemas,
            String cleaningGuide
    ) {
        this.id = id;
        this.title = title;
        this.datasetName = datasetName;
        this.totalRows = totalRows;
        this.columnHeaders = columnHeaders != null ? columnHeaders : List.of();
        this.sampleRows = sampleRows != null ? sampleRows : List.of();
        this.schemas = schemas != null ? schemas : List.of();
        this.cleaningGuide = cleaningGuide;
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
        return CourseCategory.DATA_SCIENCE;
    }

    @Override
    public Node render(boolean isDark) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(16, 20, 16, 20));

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#121922" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#223142" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView dsIconView = application.client.util.LucideIcons.icon("grid-3x3", 18, isDark);
        HBox dsIconBadge = new HBox(dsIconView);
        dsIconBadge.setAlignment(Pos.CENTER);
        dsIconBadge.setPadding(new Insets(3, 4, 3, 4));
        dsIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#451a03" : "#fef3c7") + ";" +
                "-fx-border-color: " + (isDark ? "#d97706" : "#f59e0b") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("DATASET EXPLORATION: " + datasetName.toUpperCase());
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #f59e0b; -fx-letter-spacing: 1.1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label metaBadge = new Label(totalRows + " rows × " + columnHeaders.size() + " cols");
        metaBadge.setStyle(
                "-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #f59e0b; " +
                "-fx-background-color: " + (isDark ? "#451a03" : "#fef3c7") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        header.getChildren().addAll(dsIconBadge, heading, spacer, metaBadge);

        // Dataset TableView
        TableView<List<String>> table = new TableView<>();
        table.setPrefHeight(205);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        for (int i = 0; i < columnHeaders.size(); i++) {
            final int colIndex = i;
            String headerText = columnHeaders.get(i);
            TableColumn<List<String>, String> col = new TableColumn<>();
            Label headerLabel = new Label(headerText);
            headerLabel.setMouseTransparent(true);
            headerLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0f172a") + ";");
            col.setGraphic(headerLabel);

            col.setCellValueFactory(data -> {
                if (data.getValue() != null && colIndex < data.getValue().size()) {
                    return new SimpleStringProperty(data.getValue().get(colIndex));
                }
                return new SimpleStringProperty("");
            });
            String align = ("Name".equalsIgnoreCase(headerText) || "Title".equalsIgnoreCase(headerText) || "Description".equalsIgnoreCase(headerText))
                    ? "CENTER-LEFT" : "CENTER";
            col.setStyle("-fx-alignment: " + align + "; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#1e293b") + ";");
            table.getColumns().add(col);
        }

        for (List<String> row : sampleRows) {
            table.getItems().add(row);
        }

        // Schema & Types Grid
        VBox schemaBox = new VBox(6);
        Label schemaHeader = new Label("COLUMN TYPES & MISSING VALUES");
        schemaHeader.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#94a3b8" : "#64748b") + "; -fx-padding: 2 0 0 0;");
        schemaBox.getChildren().add(schemaHeader);

        GridPane schemaGrid = new GridPane();
        schemaGrid.setHgap(14);
        schemaGrid.setVgap(4);
        schemaGrid.setPadding(new Insets(2, 6, 2, 6));

        int sRow = 0;
        for (ColumnSchema cs : schemas) {
            Label nameLbl = new Label(cs.name());
            nameLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 11.5px; -fx-text-fill: #38bdf8;");

            Label dtypeLbl = new Label(cs.dtype());
            dtypeLbl.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: 700; " +
                    "-fx-text-fill: " + (isDark ? "#a7f3d0" : "#065f46") + "; " +
                    "-fx-background-color: " + (isDark ? "#064e3b" : "#d1fae5") + "; " +
                    "-fx-padding: 1 5; -fx-background-radius: 4;"
            );

            Label nullLbl = new Label(cs.nullCount() > 0 ? cs.nullCount() + " missing (" + String.format("%.1f", (cs.nullCount() * 100.0 / totalRows)) + "%)" : "0 missing");
            nullLbl.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: 700; " +
                    "-fx-text-fill: " + (cs.nullCount() > 0 ? "#f87171" : "#94a3b8") + "; " +
                    (cs.nullCount() > 0 ? "-fx-background-color: " + (isDark ? "#450a0a" : "#fee2e2") + "; -fx-padding: 1 5; -fx-background-radius: 4;" : "")
            );

            Label notesLbl = new Label(cs.notes());
            notesLbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");
            GridPane.setHgrow(notesLbl, Priority.ALWAYS);

            schemaGrid.add(nameLbl, 0, sRow);
            schemaGrid.add(dtypeLbl, 1, sRow);
            schemaGrid.add(nullLbl, 2, sRow);
            schemaGrid.add(notesLbl, 3, sRow);
            sRow++;
        }
        schemaBox.getChildren().add(schemaGrid);

        // Data Cleaning Callout
        if (cleaningGuide != null && !cleaningGuide.isBlank()) {
            VBox cleaningBox = new VBox(4);
            cleaningBox.setPadding(new Insets(10, 12, 10, 12));
            cleaningBox.setStyle(
                    "-fx-background-color: " + (isDark ? "#291b00" : "#fefce8") + ";" +
                    "-fx-border-color: " + (isDark ? "#78350f" : "#fef08a") + ";" +
                    "-fx-border-radius: 6px; -fx-background-radius: 6px;"
            );

            Label cTitle = new Label("Recommended Data Cleaning Steps");
            cTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#fde047" : "#854d0e") + ";");

            Label cText = new Label(cleaningGuide);
            cText.setWrapText(true);
            cText.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#fef08a" : "#713f12") + "; -fx-line-spacing: 2px;");

            cleaningBox.getChildren().addAll(cTitle, cText);
            card.getChildren().addAll(header, table, schemaBox, cleaningBox);
        } else {
            card.getChildren().addAll(header, table, schemaBox);
        }

        return card;
    }
}
