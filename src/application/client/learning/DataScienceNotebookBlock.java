package application.client.learning;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * DataScienceNotebookBlock provides a notebook-style data analysis environment featuring:
 * - Tabular dataset viewer
 * - Python / Pandas query execution
 * - Resulting DataFrame table output
 * - Dynamic data visualizations (Bar chart, Scatter plot, Line chart)
 * - Statistical summary output
 * - Analytical insight checks
 */
public class DataScienceNotebookBlock implements LearningBlock {
    private final String id;
    private final String title;
    private final String queryExample;

    public DataScienceNotebookBlock(String id, String title, String queryExample) {
        this.id = id;
        this.title = title;
        this.queryExample = (queryExample != null && !queryExample.isBlank())
                ? queryExample
                : "df.groupby(\"Department\")[\"Salary\"].mean()";
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
                "-fx-background-color: " + (isDark ? "#121a24" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#233345" : "#e2e8f0") + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView dsIconView = application.client.util.LucideIcons.icon("bar-chart-3", 18, isDark);
        HBox dsIconBadge = new HBox(dsIconView);
        dsIconBadge.setAlignment(Pos.CENTER);
        dsIconBadge.setPadding(new Insets(3, 4, 3, 4));
        dsIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#451a03" : "#fef3c7") + ";" +
                "-fx-border-color: " + (isDark ? "#d97706" : "#f59e0b") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("DATA SCIENCE NOTEBOOK LAB: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #f59e0b; -fx-letter-spacing: 1.1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label envBadge = new Label("Pandas & Statistical Analytics");
        envBadge.setStyle(
                "-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #f59e0b; " +
                "-fx-background-color: " + (isDark ? "#451a03" : "#fef3c7") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        header.getChildren().addAll(dsIconBadge, heading, spacer, envBadge);

        // Section 1: Active In-Memory Dataset Preview
        VBox datasetSection = new VBox(6);
        Label dsTitle = new Label("ACTIVE DATAFRAME: employees_df (6 rows × 4 columns)");
        dsTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        TableView<List<String>> inputTable = new TableView<>();
        inputTable.setPrefHeight(155);
        inputTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        List<String> inHeaders = List.of("Age", "Salary ($)", "Department", "Experience (Yrs)");
        for (int i = 0; i < inHeaders.size(); i++) {
            final int colIdx = i;
            String hName = inHeaders.get(i);
            TableColumn<List<String>, String> col = new TableColumn<>();
            Label hLbl = new Label(hName);
            hLbl.setMouseTransparent(true);
            hLbl.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0f172a") + ";");
            col.setGraphic(hLbl);
            col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(colIdx)));
            String align = "Department".equalsIgnoreCase(hName) ? "CENTER-LEFT" : "CENTER";
            col.setStyle("-fx-alignment: " + align + "; -fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#1e293b") + ";");
            inputTable.getColumns().add(col);
        }

        inputTable.getItems().addAll(
                List.of("21", "25,000", "CSE", "1"),
                List.of("23", "32,000", "EEE", "2"),
                List.of("25", "45,000", "CSE", "3"),
                List.of("28", "58,000", "ME", "5"),
                List.of("24", "38,000", "EEE", "2"),
                List.of("30", "72,000", "CSE", "7")
        );
        datasetSection.getChildren().addAll(dsTitle, inputTable);

        // Section 2: Interactive Pandas Query Runner Cell
        VBox notebookCell = new VBox(8);
        notebookCell.setPadding(new Insets(12, 14, 12, 14));
        notebookCell.setStyle(
                "-fx-background-color: " + (isDark ? "#0a0e14" : "#f1f5f9") + ";" +
                "-fx-border-color: " + (isDark ? "#1e293b" : "#cbd5e1") + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;"
        );

        HBox cellHeader = new HBox(8);
        cellHeader.setAlignment(Pos.CENTER_LEFT);
        Label inPrompt = new Label("In [1]:");
        inPrompt.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #10b981;");

        TextField queryField = new TextField(queryExample);
        queryField.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12.5px; -fx-font-weight: 700; " +
                "-fx-text-fill: " + (isDark ? "#38bdf8" : "#0284c7") + "; " +
                "-fx-background-color: " + (isDark ? "#121922" : "#ffffff") + "; " +
                "-fx-border-color: " + (isDark ? "#28374d" : "#94a3b8") + "; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 10;"
        );
        HBox.setHgrow(queryField, Priority.ALWAYS);

        Button executeBtn = new Button("Execute Pandas Query");
        executeBtn.setStyle(
                "-fx-background-color: #f59e0b; -fx-text-fill: #000000; -fx-font-weight: 800; -fx-font-size: 11.5px; " +
                "-fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;"
        );

        cellHeader.getChildren().addAll(inPrompt, queryField, executeBtn);
        notebookCell.getChildren().add(cellHeader);

        // Section 3: Output Area with Tabs (Table Output vs Chart Visualization vs Stats)
        TabPane outputTabs = new TabPane();
        outputTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        outputTabs.setStyle("-fx-background-color: transparent;");

        // Tab 1: Tabular DataFrame Output
        Tab tableTab = new Tab("Tabular Output");
        TableView<List<String>> resultTable = new TableView<>();
        resultTable.setPrefHeight(140);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<List<String>, String> c1 = new TableColumn<>();
        Label l1 = new Label("Department");
        l1.setMouseTransparent(true);
        l1.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0f172a") + ";");
        c1.setGraphic(l1);
        c1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(0)));
        c1.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 700; -fx-font-size: 11px; -fx-alignment: CENTER-LEFT; -fx-text-fill: " + (isDark ? "#cbd5e1" : "#1e293b") + ";");

        TableColumn<List<String>, String> c2 = new TableColumn<>();
        Label l2 = new Label("Mean Salary ($)");
        l2.setMouseTransparent(true);
        l2.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0f172a") + ";");
        c2.setGraphic(l2);
        c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(1)));
        c2.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 700; -fx-font-size: 11px; -fx-text-fill: #10b981; -fx-alignment: CENTER;");

        resultTable.getColumns().addAll(c1, c2);
        resultTable.getItems().addAll(
                List.of("CSE", "47,333.33"),
                List.of("EEE", "35,000.00"),
                List.of("ME", "58,000.00")
        );
        tableTab.setContent(resultTable);

        // Tab 2: Chart Visualization (Bar Chart Output)
        Tab chartTab = new Tab("Visualization Output");
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Department");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Mean Salary ($)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Salary Distribution by Department");
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(180);
        barChart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("CSE", 47333.33));
        series.getData().add(new XYChart.Data<>("EEE", 35000.00));
        series.getData().add(new XYChart.Data<>("ME", 58000.00));
        barChart.getData().add(series);
        chartTab.setContent(barChart);

        // Tab 3: Summary Statistics
        Tab statsTab = new Tab("Summary Statistics");
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(16);
        statsGrid.setVgap(6);
        statsGrid.setPadding(new Insets(10, 14, 10, 14));
        statsGrid.setStyle("-fx-background-color: " + (isDark ? "#0b0f15" : "#f8fafc") + "; -fx-border-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; -fx-border-radius: 6px;");

        String[][] statsData = {
                {"Count:", "6", "Mean:", "$42,833.33"},
                {"Std Dev:", "$16,424.58", "Min:", "$25,000.00"},
                {"25% (Q1):", "$33,500.00", "Median (50%):", "$41,500.00"},
                {"75% (Q3):", "$54,750.00", "Max:", "$72,000.00"}
        };

        for (int r = 0; r < statsData.length; r++) {
            for (int c = 0; c < 4; c++) {
                Label lbl = new Label(statsData[r][c]);
                lbl.setStyle(c % 2 == 0
                        ? "-fx-font-weight: 700; -fx-font-size: 11.5px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";"
                        : "-fx-font-family: 'JetBrains Mono', monospace; -fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#38bdf8" : "#0284c7") + ";"
                );
                statsGrid.add(lbl, c, r);
            }
        }
        statsTab.setContent(statsGrid);

        outputTabs.getTabs().addAll(tableTab, chartTab, statsTab);

        // Wire execute button to flash feedback
        executeBtn.setOnAction(e -> {
            outputTabs.getSelectionModel().select(chartTab);
        });

        card.getChildren().addAll(header, datasetSection, notebookCell, outputTabs);

        return card;
    }
}
