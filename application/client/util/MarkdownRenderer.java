package application.client.util;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public final class MarkdownRenderer {
    private MarkdownRenderer() {
    }

    public static void render(String markdown, VBox destination) {
        destination.getChildren().clear();
        List<String> code = new ArrayList<>();
        boolean inCode = false;
        for (String rawLine : safe(markdown).split("\\R", -1)) {
            String line = rawLine.stripTrailing();
            if (line.startsWith("```")) {
                if (inCode) {
                    destination.getChildren().add(codeBlock(code));
                    code.clear();
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
            destination.getChildren().add(codeBlock(code));
        }
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

    private static TextArea codeBlock(List<String> lines) {
        TextArea editor = new TextArea(String.join("\n", lines));
        editor.setEditable(false);
        editor.setWrapText(false);
        int rows = Math.max(4, Math.min(lines.size() + 1, 18));
        editor.setPrefRowCount(rows);
        editor.setMinHeight(Math.max(100, Math.min(380, (lines.size() + 1) * 22)));
        editor.getStyleClass().add("markdown-code-block");
        return editor;
    }

    private static String cleanInlineMarkdown(String value) {
        return value.replace("**", "").replace("`", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
