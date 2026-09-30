package application.client.learning;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.io.InputStream;

/**
 * LiveWebPreviewBlock provides an embedded live browser renderer (JavaFX WebView)
 * for real-time visual feedback of HTML, CSS, and JavaScript.
 */
public class LiveWebPreviewBlock implements LearningBlock {
    private final String id;
    private final String title;
    private final String initialHtml;
    private final String debugChallenge;
    private WebView webView;
    private StackPane previewContainer;

    public LiveWebPreviewBlock(String id, String title, String initialHtml, String debugChallenge) {
        this.id = id;
        this.title = title;
        this.initialHtml = initialHtml != null ? initialHtml : defaultDemoHtml();
        this.debugChallenge = debugChallenge;
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
        return CourseCategory.WEB_DEV;
    }

    @Override
    public Node render(boolean isDark) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));

        card.setStyle(
                "-fx-background-color: " + (isDark ? "#0f1723" : "#ffffff") + ";" +
                "-fx-border-color: " + (isDark ? "#1e2c3d" : "#e2e8f0") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 10px;" +
                "-fx-background-radius: 10px;"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView liveIconView = application.client.util.LucideIcons.icon("globe", 18, isDark);

        HBox liveIconBadge = new HBox(liveIconView);
        liveIconBadge.setAlignment(Pos.CENTER);
        liveIconBadge.setPadding(new Insets(3, 4, 3, 4));
        liveIconBadge.setStyle(
                "-fx-background-color: " + (isDark ? "#16222f" : "#e0f2fe") + ";" +
                "-fx-border-color: " + (isDark ? "#06b6d4" : "#0284c7") + ";" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px; -fx-background-radius: 6px;"
        );

        Label heading = new Label("LIVE WEB PREVIEW: " + title.toUpperCase());
        heading.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #06b6d4; -fx-letter-spacing: 1.1px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("Real-Time DOM Engine");
        badge.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #06b6d4; " +
                "-fx-background-color: " + (isDark ? "#083344" : "#cffafe") + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 6;"
        );

        header.getChildren().addAll(liveIconBadge, heading, spacer, badge);

        // Browser Address Bar Mockup
        HBox browserBar = new HBox(8);
        browserBar.setAlignment(Pos.CENTER_LEFT);
        browserBar.setPadding(new Insets(6, 10, 6, 10));
        browserBar.setStyle(
                "-fx-background-color: " + (isDark ? "#161f2e" : "#f1f5f9") + ";" +
                "-fx-border-color: " + (isDark ? "#243245" : "#cbd5e1") + ";" +
                "-fx-border-radius: 6px 6px 0 0; -fx-background-radius: 6px 6px 0 0;"
        );

        // Window traffic light dots
        HBox dots = new HBox(4);
        dots.setAlignment(Pos.CENTER_LEFT);
        Label dotR = new Label("●"); dotR.setStyle("-fx-font-size: 10px; -fx-text-fill: #ef4444;");
        Label dotY = new Label("●"); dotY.setStyle("-fx-font-size: 10px; -fx-text-fill: #f59e0b;");
        Label dotG = new Label("●"); dotG.setStyle("-fx-font-size: 10px; -fx-text-fill: #10b981;");
        dots.getChildren().addAll(dotR, dotY, dotG);

        Label urlField = new Label("http://localhost:3000/live-preview");
        urlField.setStyle(
                "-fx-font-family: 'JetBrains Mono', monospace; -fx-font-size: 11px; -fx-text-fill: " + (isDark ? "#94a3b8" : "#475569") + "; " +
                "-fx-background-color: " + (isDark ? "#0c131c" : "#ffffff") + "; -fx-padding: 3 10; -fx-background-radius: 4; -fx-border-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; -fx-border-radius: 4;"
        );
        HBox.setHgrow(urlField, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setStyle(
                "-fx-background-color: " + (isDark ? "#1e293b" : "#e2e8f0") + "; " +
                "-fx-text-fill: " + (isDark ? "#cbd5e1" : "#334155") + "; " +
                "-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 4; -fx-cursor: hand;"
        );

        browserBar.getChildren().addAll(dots, urlField, refreshBtn);

        // WebView Preview Container
        previewContainer = new StackPane();
        previewContainer.setMinHeight(240);
        previewContainer.setPrefHeight(280);
        previewContainer.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: " + (isDark ? "#243245" : "#cbd5e1") + "; " +
                "-fx-border-width: 0 1px 1px 1px; -fx-border-radius: 0 0 6px 6px; -fx-background-radius: 0 0 6px 6px;"
        );

        try {
            webView = new WebView();
            webView.getEngine().loadContent(initialHtml);
            previewContainer.getChildren().add(webView);
            refreshBtn.setOnAction(e -> webView.getEngine().loadContent(initialHtml));
        } catch (Throwable t) {
            Label fallback = new Label("Preview renderer active.\n(WebView initialized with fallback simulation)");
            fallback.setStyle("-fx-text-fill: #475569; -fx-alignment: center; -fx-font-size: 13px;");
            previewContainer.getChildren().add(fallback);
        }

        card.getChildren().addAll(header, browserBar, previewContainer);

        // Debug Challenge Box
        if (debugChallenge != null && !debugChallenge.isBlank()) {
            VBox challengeBox = new VBox(6);
            challengeBox.setPadding(new Insets(10, 14, 10, 14));
            challengeBox.setStyle(
                    "-fx-background-color: " + (isDark ? "#191128" : "#fdf4ff") + ";" +
                    "-fx-border-color: " + (isDark ? "#7c3aed" : "#c084fc") + ";" +
                    "-fx-border-width: 1.5px;" +
                    "-fx-border-radius: 8px; -fx-background-radius: 8px;"
            );

            HBox debugTitleBox = new HBox(7);
            debugTitleBox.setAlignment(Pos.CENTER_LEFT);

            ImageView bugIconView = new ImageView();
            String bugIconPath = isDark ? "/resources/images/icon-debug-dark.png" : "/resources/images/icon-debug.png";
            try {
                InputStream is = getClass().getResourceAsStream(bugIconPath);
                if (is != null) {
                    bugIconView.setImage(new Image(is));
                    bugIconView.setFitWidth(16);
                    bugIconView.setFitHeight(16);
                    bugIconView.setSmooth(true);
                }
            } catch (Throwable ignored) {}

            Label chTitle = new Label("DEBUGGING CHALLENGE");
            chTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isDark ? "#d8b4fe" : "#7e22ce") + "; -fx-letter-spacing: 0.9px;");
            debugTitleBox.getChildren().addAll(bugIconView, chTitle);

            Label chText = new Label(debugChallenge);
            chText.setWrapText(true);
            chText.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isDark ? "#f3e8ff" : "#581c87") + "; -fx-line-spacing: 2px;");

            challengeBox.getChildren().addAll(debugTitleBox, chText);
            card.getChildren().add(challengeBox);
        }

        return card;
    }

    /**
     * Updates the rendered HTML inside the preview.
     */
    public void updateContent(String html) {
        if (webView != null) {
            Platform.runLater(() -> {
                try {
                    webView.getEngine().loadContent(html != null ? html : "");
                } catch (Throwable ignored) {}
            });
        }
    }

    private static String defaultDemoHtml() {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<style>"
                + "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 24px; background: #0f172a; color: #f8fafc; margin: 0; }"
                + ".card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 20px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }"
                + "h2 { color: #38bdf8; margin-top: 0; font-size: 20px; }"
                + "p { color: #94a3b8; font-size: 14px; line-height: 1.5; }"
                + ".btn { background: #0284c7; color: #ffffff; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer; }"
                + ".btn:hover { background: #0369a1; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<h2>Modern Web Component</h2>"
                + "<p>This live preview renders HTML, CSS & JS in real time as you edit code in CodeTrail.</p>"
                + "<button class='btn' onclick='alert(\"Button clicked!\")'>Interactive Action</button>"
                + "</div>"
                + "</body></html>";
    }
}
