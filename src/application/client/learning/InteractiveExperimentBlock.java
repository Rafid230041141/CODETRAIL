package application.client.learning;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * InteractiveExperimentBlock provides a live parameter experiment widget with sliders,
 * real-time graph visualization (fitted line, residual errors), and dynamic MSE / R² metrics.
 */
public class InteractiveExperimentBlock implements LearningBlock {
    public enum ExperimentType {
        LINEAR_REGRESSION,
        SIGMOID_ACTIVATION
    }

    private final String id;
    private final String title;
    private final ExperimentType type;

    // Sample data points for linear regression: (x, y)
    private static final double[] X_DATA = { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0 };
    private static final double[] Y_DATA = { 2.2, 2.9, 4.2, 5.1, 5.8, 7.2, 7.9, 9.1 };

    public InteractiveExperimentBlock(String id, String title, ExperimentType type) {
        this.id = id;
        this.title = title;
        this.type = type != null ? type : ExperimentType.LINEAR_REGRESSION;
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
                "-fx-background-color: " + (isDark ? "#121a24" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#1f2f42" : "#e2e8f0") + ";" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.image.ImageView expIconView = application.client.util.LucideIcons.icon("sparkles", 18, isDark);
        HBox expIconBadge = new HBox(expIconView);
        expIconBadge.setAlignment(Pos.CENTER);
        expIconBadge.setPadding(new Insets(3, 4, 3, 4));
        expIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#082f49" : "#e0f2fe") + ";" +
                "-fx-border-color: " + (isDark ? "#0284c7" : "#38bdf8") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("INTERACTIVE EXPERIMENT: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-letter-spacing: 1.1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(type == ExperimentType.LINEAR_REGRESSION ? "Loss Minimization" : "Non-Linear Activation");
        badge.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #38bdf8; " +
                "-fx-background-color: " + (isDark ? "#082f49" : "#e0f2fe") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        header.getChildren().addAll(expIconBadge, heading, spacer, badge);

        // Subtitle instructions
        Label instructions = new Label(
                type == ExperimentType.LINEAR_REGRESSION
                        ? "Drag the sliders below to adjust weight (w) and bias (b). Notice how the residual error bars shrink as you approach the optimal fit!"
                        : "Adjust steepness (w) and shift (b) to observe how the activation threshold shifts between 0 and 1."
        );
        instructions.setWrapText(true);
        instructions.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        // Canvas for Live Graphing
        double canvasWidth = 560;
        double canvasHeight = 220;
        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Metrics Labels
        Label mseValueLabel = new Label("0.00");
        mseValueLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #f59e0b;");

        Label r2ValueLabel = new Label("0.00");
        r2ValueLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #10b981;");

        Label fitStatusBadge = new Label("Underfitting");
        fitStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 6;");

        // Sliders
        Slider weightSlider = new Slider(0.0, 2.5, 0.5);
        weightSlider.setShowTickMarks(false);
        weightSlider.setShowTickLabels(false);
        HBox.setHgrow(weightSlider, Priority.ALWAYS);

        Label weightValLabel = new Label(String.format(Locale.US, "w = %.2f", weightSlider.getValue()));
        weightValLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#e2e8f0" : "#1e293b") + "; -fx-min-width: 75px;");

        Slider biasSlider = new Slider(-2.0, 4.0, 0.0);
        biasSlider.setShowTickMarks(false);
        biasSlider.setShowTickLabels(false);
        HBox.setHgrow(biasSlider, Priority.ALWAYS);

        Label biasValLabel = new Label(String.format(Locale.US, "b = %.2f", biasSlider.getValue()));
        biasValLabel.setStyle("-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#e2e8f0" : "#1e293b") + "; -fx-min-width: 75px;");

        // Update Routine
        Runnable updateGraph = () -> {
            double w = weightSlider.getValue();
            double b = biasSlider.getValue();
            weightValLabel.setText(String.format(Locale.US, "w = %.2f", w));
            biasValLabel.setText(String.format(Locale.US, "b = %.2f", b));

            // Background
            gc.clearRect(0, 0, canvasWidth, canvasHeight);
            gc.setFill(Color.web(isDark ? "#090d13" : "#f1f5f9"));
            gc.fillRoundRect(0, 0, canvasWidth, canvasHeight, 8, 8);

            // Subtle Grid
            gc.setStroke(Color.web(isDark ? "#1e293b" : "#e2e8f0"));
            gc.setLineWidth(1.0);
            for (double gx = 40; gx < canvasWidth; gx += 50) {
                gc.strokeLine(gx, 10, gx, canvasHeight - 25);
            }
            for (double gy = 20; gy < canvasHeight - 20; gy += 40) {
                gc.strokeLine(40, gy, canvasWidth - 15, gy);
            }

            // Coordinate mapping (x in [0, 10], y in [0, 12])
            double xMargin = 45;
            double yMargin = 25;
            double plotW = canvasWidth - xMargin - 20;
            double plotH = canvasHeight - yMargin - 15;

            // Axes
            gc.setStroke(Color.web(isDark ? "#475569" : "#94a3b8"));
            gc.setLineWidth(1.5);
            gc.strokeLine(xMargin, 10, xMargin, canvasHeight - yMargin);
            gc.strokeLine(xMargin, canvasHeight - yMargin, canvasWidth - 15, canvasHeight - yMargin);

            if (type == ExperimentType.LINEAR_REGRESSION) {
                // Calculate MSE and R2
                double totalSquaredResiduals = 0.0;
                double sumY = 0.0;
                int n = X_DATA.length;

                for (double y : Y_DATA) sumY += y;
                double meanY = sumY / n;

                double totalSumSquares = 0.0;
                for (double y : Y_DATA) {
                    totalSumSquares += (y - meanY) * (y - meanY);
                }

                // Draw residual dashed lines
                gc.setLineWidth(1.2);
                gc.setLineDashes(4.0);

                for (int i = 0; i < n; i++) {
                    double xi = X_DATA[i];
                    double yi = Y_DATA[i];
                    double yHat = w * xi + b;
                    double res = yi - yHat;
                    totalSquaredResiduals += res * res;

                    double px = xMargin + (xi / 10.0) * plotW;
                    double py = (canvasHeight - yMargin) - (yi / 12.0) * plotH;
                    double pyHat = (canvasHeight - yMargin) - (Math.max(0, Math.min(12, yHat)) / 12.0) * plotH;

                    // Color residual line: green if very close, orange/red if far
                    double absErr = Math.abs(res);
                    if (absErr < 0.4) {
                        gc.setStroke(Color.web("#10b981", 0.7));
                    } else if (absErr < 1.2) {
                        gc.setStroke(Color.web("#f59e0b", 0.7));
                    } else {
                        gc.setStroke(Color.web("#ef4444", 0.7));
                    }
                    gc.strokeLine(px, py, px, pyHat);
                }

                gc.setLineDashes(0); // clear dash

                // Draw Regression Line
                double yStart = w * 0.0 + b;
                double yEnd = w * 10.0 + b;

                double px0 = xMargin;
                double py0 = (canvasHeight - yMargin) - (yStart / 12.0) * plotH;
                double px1 = xMargin + plotW;
                double py1 = (canvasHeight - yMargin) - (yEnd / 12.0) * plotH;

                gc.setStroke(Color.web("#38bdf8"));
                gc.setLineWidth(2.5);
                gc.strokeLine(px0, py0, px1, py1);

                // Draw Data Points
                for (int i = 0; i < n; i++) {
                    double xi = X_DATA[i];
                    double yi = Y_DATA[i];
                    double px = xMargin + (xi / 10.0) * plotW;
                    double py = (canvasHeight - yMargin) - (yi / 12.0) * plotH;

                    gc.setFill(Color.web("#818cf8"));
                    gc.fillOval(px - 4.5, py - 4.5, 9, 9);
                    gc.setStroke(Color.web("#ffffff"));
                    gc.setLineWidth(1.5);
                    gc.strokeOval(px - 4.5, py - 4.5, 9, 9);
                }

                // Calculate metrics
                double mse = totalSquaredResiduals / n;
                double r2 = 1.0 - (totalSquaredResiduals / (totalSumSquares > 0 ? totalSumSquares : 1.0));
                r2 = Math.max(-1.0, Math.min(1.0, r2));

                mseValueLabel.setText(String.format(Locale.US, "%.3f", mse));
                r2ValueLabel.setText(String.format(Locale.US, "%.2f", r2));

                if (mse < 0.18) {
                    fitStatusBadge.setText("Optimal Fit");
                    fitStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #10b981; -fx-background-color: #064e3b; -fx-padding: 4 10; -fx-background-radius: 6;");
                } else if (mse < 1.0) {
                    fitStatusBadge.setText("Acceptable Fit");
                    fitStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #f59e0b; -fx-background-color: #78350f; -fx-padding: 4 10; -fx-background-radius: 6;");
                } else {
                    fitStatusBadge.setText("High Error / Poor Fit");
                    fitStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #ef4444; -fx-background-color: #7f1d1d; -fx-padding: 4 10; -fx-background-radius: 6;");
                }
            } else {
                // Sigmoid curve: y = 1 / (1 + exp(-(w*x + b)))
                gc.setStroke(Color.web("#10b981"));
                gc.setLineWidth(2.5);

                double prevPx = -1, prevPy = -1;
                for (double x = -5.0; x <= 5.0; x += 0.2) {
                    double z = w * x + b;
                    double sigmoid = 1.0 / (1.0 + Math.exp(-z));
                    double px = xMargin + ((x + 5.0) / 10.0) * plotW;
                    double py = (canvasHeight - yMargin) - (sigmoid) * plotH;

                    if (prevPx >= 0) {
                        gc.strokeLine(prevPx, prevPy, px, py);
                    }
                    prevPx = px;
                    prevPy = py;
                }

                mseValueLabel.setText("Active");
                r2ValueLabel.setText("σ(wx+b)");
                fitStatusBadge.setText("Threshold: " + String.format(Locale.US, "%.1f", -b / (Math.abs(w) > 0.001 ? w : 1.0)));
                fitStatusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-background-color: #082f49; -fx-padding: 4 10; -fx-background-radius: 6;");
            }
        };

        // Wire slider listeners
        weightSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateGraph.run());
        biasSlider.valueProperty().addListener((obs, oldVal, newVal) -> updateGraph.run());

        // Control Sliders Panel
        VBox controlsPanel = new VBox(8);
        controlsPanel.setPadding(new Insets(8, 0, 0, 0));

        HBox wRow = new HBox(10, new Label("Slope (w):"), weightSlider, weightValLabel);
        wRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) wRow.getChildren().get(0)).setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + "; -fx-min-width: 70px;");

        HBox bRow = new HBox(10, new Label("Bias (b):"), biasSlider, biasValLabel);
        bRow.setAlignment(Pos.CENTER_LEFT);
        ((Label) bRow.getChildren().get(0)).setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + "; -fx-min-width: 70px;");

        // Action Buttons: Auto-Fit OLS & Reset
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(6, 0, 0, 0));

        Button autoFitBtn = new Button("Find Optimal Fit (OLS)");
        autoFitBtn.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-size: 11.5px; -fx-font-weight: 800; " +
                "-fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;"
        );
        autoFitBtn.setOnAction(e -> {
            if (type == ExperimentType.LINEAR_REGRESSION) {
                // Compute analytical OLS
                double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
                int n = X_DATA.length;
                for (int i = 0; i < n; i++) {
                    sumX += X_DATA[i];
                    sumY += Y_DATA[i];
                    sumXY += X_DATA[i] * Y_DATA[i];
                    sumXX += X_DATA[i] * X_DATA[i];
                }
                double wOpt = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
                double bOpt = (sumY - wOpt * sumX) / n;
                weightSlider.setValue(wOpt);
                biasSlider.setValue(bOpt);
            }
        });

        Button resetBtn = new Button("Reset Sliders");
        resetBtn.setStyle(
                "-fx-background-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; " +
                "-fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + "; " +
                "-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-padding: 5 12; -fx-background-radius: 6; -fx-cursor: hand;"
        );
        resetBtn.setOnAction(e -> {
            weightSlider.setValue(0.5);
            biasSlider.setValue(0.0);
        });

        buttonBar.getChildren().addAll(resetBtn, autoFitBtn);
        controlsPanel.getChildren().addAll(wRow, bRow, buttonBar);

        // Metrics Banner
        HBox metricsBanner = new HBox(18);
        metricsBanner.setAlignment(Pos.CENTER_LEFT);
        metricsBanner.setPadding(new Insets(10, 14, 10, 14));
        metricsBanner.setStyle(
                "-fx-background-color: " + (isDark ? "#090d13" : "#f1f5f9") + ";" +
                "-fx-border-color: " + (isDark ? "#1e293b" : "#cbd5e1") + ";" +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;"
        );

        HBox mseCard = new HBox(6, new Label("MSE Loss:"), mseValueLabel);
        mseCard.setAlignment(Pos.CENTER_LEFT);
        ((Label) mseCard.getChildren().get(0)).setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        HBox r2Card = new HBox(6, new Label("R² Score:"), r2ValueLabel);
        r2Card.setAlignment(Pos.CENTER_LEFT);
        ((Label) r2Card.getChildren().get(0)).setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + ";");

        Region bannerSpacer = new Region();
        HBox.setHgrow(bannerSpacer, Priority.ALWAYS);

        metricsBanner.getChildren().addAll(mseCard, r2Card, bannerSpacer, fitStatusBadge);

        card.getChildren().addAll(header, instructions, canvas, metricsBanner, controlsPanel);

        // Initial render
        updateGraph.run();

        return card;
    }
}
