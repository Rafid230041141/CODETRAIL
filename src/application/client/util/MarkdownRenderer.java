package application.client.util;

import application.client.dsa.judge.CodeSyntaxHighlighter;
import application.client.dsa.judge.ProgrammingLanguage;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class MarkdownRenderer {
    private MarkdownRenderer() {
    }

    public static void render(String markdown, VBox destination) {
        render(markdown, destination, ProgrammingLanguage.JAVA);
    }

    public static void render(String markdown, VBox destination, ProgrammingLanguage defaultLang) {
        destination.getChildren().clear();
        List<String> code = new ArrayList<>();
        boolean inCode = false;
        ProgrammingLanguage currentCodeLang = defaultLang != null ? defaultLang : ProgrammingLanguage.JAVA;

        for (String rawLine : safe(markdown).split("\\R", -1)) {
            String line = rawLine.stripTrailing();
            if (line.startsWith("```")) {
                if (inCode) {
                    destination.getChildren().add(codeBlock(code, currentCodeLang));
                    code.clear();
                    currentCodeLang = defaultLang != null ? defaultLang : ProgrammingLanguage.JAVA;
                } else {
                    String fenceLang = line.substring(3).trim().toLowerCase(Locale.ROOT);
                    if (!fenceLang.isEmpty()) {
                        currentCodeLang = parseLanguage(fenceLang, defaultLang);
                    } else {
                        currentCodeLang = defaultLang != null ? defaultLang : ProgrammingLanguage.JAVA;
                    }
                }
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                code.add(rawLine);
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            destination.getChildren().add(renderLine(line));
        }
        if (!code.isEmpty()) {
            destination.getChildren().add(codeBlock(code, currentCodeLang));
        }
    }

    private static ProgrammingLanguage parseLanguage(String tag, ProgrammingLanguage defaultLang) {
        if (tag == null || tag.isBlank()) return defaultLang != null ? defaultLang : ProgrammingLanguage.JAVA;
        String t = tag.toLowerCase(Locale.ROOT);
        if (t.contains("java") && !t.contains("script")) return ProgrammingLanguage.JAVA;
        if (t.contains("cpp") || t.contains("c++")) return ProgrammingLanguage.CPP;
        if (t.equals("c")) return ProgrammingLanguage.C;
        if (t.contains("python") || t.equals("py")) return ProgrammingLanguage.PYTHON;
        if (t.contains("html")) return ProgrammingLanguage.HTML;
        if (t.contains("css")) return ProgrammingLanguage.CSS;
        if (t.contains("sql")) return ProgrammingLanguage.SQL;
        if (t.contains("js") || t.contains("javascript") || t.contains("ts") || t.contains("typescript")) return ProgrammingLanguage.JAVASCRIPT;
        if (t.contains("c#") || t.contains("csharp") || t.contains("cs")) return ProgrammingLanguage.CSHARP;
        return defaultLang != null ? defaultLang : ProgrammingLanguage.JAVA;
    }

    private static Node renderLine(String line) {
        Label label;
        if (line.startsWith("### ")) {
            label = label(line.substring(4), "markdown-heading-3");
        } else if (line.startsWith("## ")) {
            label = label(line.substring(3), "markdown-heading-2");
        } else if (line.startsWith("# ")) {
            label = label(line.substring(2), "markdown-heading-1");
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            label = label("•  " + line.substring(2), "markdown-bullet");
        } else {
            label = label(line, "markdown-paragraph");
        }
        return label;
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(cleanInlineMarkdown(text));
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private static Node codeBlock(List<String> lines, ProgrammingLanguage lang) {
        String codeText = String.join("\n", lines);

        CodeArea codeArea = new CodeArea();
        codeArea.setEditable(false);
        codeArea.getStyleClass().addAll("styled-text-area", "code-editor-area", "markdown-code-block");

        var cssRes = MarkdownRenderer.class.getResource("/resources/css/application.css");
        if (cssRes != null) {
            String cssUrl = cssRes.toExternalForm();
            if (!codeArea.getStylesheets().contains(cssUrl)) {
                codeArea.getStylesheets().add(cssUrl);
            }
        }

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.replaceText(codeText);

        ProgrammingLanguage effectiveLang = lang != null ? lang : ProgrammingLanguage.JAVA;
        StyleSpans<Collection<String>> spans = CodeSyntaxHighlighter.computeHighlighting(codeText, effectiveLang);
        codeArea.setStyleSpans(0, spans);

        codeArea.setStyle(
                "-fx-font-family: Consolas, Menlo, Monaco, 'Droid Sans Mono', 'Courier New', monospace;" +
                "-fx-font-size: 14px;" +
                "-fx-line-spacing: 5px;" +
                "-fx-background-color: #1c2130;"
        );

        int lineCount = Math.max(1, lines.size());
        double computedHeight = (lineCount * 24.0) + 26.0;
        codeArea.setPrefHeight(computedHeight);
        codeArea.setMinHeight(Math.min(computedHeight, 80.0));
        codeArea.setMaxHeight(Math.max(computedHeight, 600.0));

        // Forward scroll events to parent ScrollPane when scrolling vertically
        codeArea.addEventFilter(ScrollEvent.SCROLL, event -> {
            Node parent = codeArea.getParent();
            while (parent != null && !(parent instanceof ScrollPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof ScrollPane scrollPane) {
                double deltaY = event.getDeltaY();
                double vvalue = scrollPane.getVvalue();
                double contentHeight = scrollPane.getContent() != null ? scrollPane.getContent().getBoundsInLocal().getHeight() : 1.0;
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                double scrollableHeight = contentHeight - viewportHeight;
                if (scrollableHeight > 0) {
                    double newV = vvalue - (deltaY / scrollableHeight);
                    scrollPane.setVvalue(Math.max(0.0, Math.min(1.0, newV)));
                }
                event.consume();
            }
        });

        // Top bar with language badge and Copy button (Programiz / modern documentation layout)
        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(6, 14, 6, 14));
        topBar.setStyle(
                "-fx-background-color: #161b26;" +
                "-fx-border-color: #212838;" +
                "-fx-border-width: 0 0 1px 0;" +
                "-fx-background-radius: 8px 8px 0 0;"
        );

        Label langLabel = new Label(effectiveLang.displayName());
        langLabel.setStyle(
                "-fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;" +
                "-fx-font-size: 11.5px;" +
                "-fx-font-weight: 700;" +
                "-fx-text-fill: #7fdbca;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button copyBtn = new Button("Copy");
        copyBtn.setStyle(
                "-fx-background-color: #232a3b;" +
                "-fx-border-color: #30394f;" +
                "-fx-border-radius: 4px;" +
                "-fx-background-radius: 4px;" +
                "-fx-font-size: 10.5px;" +
                "-fx-font-weight: 600;" +
                "-fx-text-fill: #94a3b8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 2px 8px;"
        );
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(codeText);
            Clipboard.getSystemClipboard().setContent(content);
            copyBtn.setText("Copied! ✓");
            copyBtn.setStyle("-fx-background-color: #16382b; -fx-border-color: #10b981; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #10b981; -fx-cursor: hand; -fx-padding: 2px 8px;");
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> {
                copyBtn.setText("Copy");
                copyBtn.setStyle("-fx-background-color: #232a3b; -fx-border-color: #30394f; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #94a3b8; -fx-cursor: hand; -fx-padding: 2px 8px;");
            });
            pause.play();
        });

        topBar.getChildren().addAll(langLabel, spacer, copyBtn);

        VBox container = new VBox(topBar, codeArea);
        container.setStyle(
                "-fx-background-color: #1c2130;" +
                "-fx-border-color: #283347;" +
                "-fx-border-width: 1.5px;" +
                "-fx-border-radius: 8px;" +
                "-fx-background-radius: 8px;"
        );
        VBox.setMargin(container, new Insets(10, 0, 14, 0));
        return container;
    }

    private static String cleanInlineMarkdown(String value) {
        return value.replace("**", "").replace("`", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
