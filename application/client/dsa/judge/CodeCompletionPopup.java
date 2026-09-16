package application.client.dsa.judge;

import application.client.dsa.judge.CodeCompletionEngine.CompletionItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.List;

/**
 * Modern floating code completion popup for the CodeTrail code editor.
 * Provides interactive keyboard navigation (Up/Down, Enter/Tab to accept, Esc to dismiss),
 * rich category badges, snippet insertion, and caret-anchored positioning.
 */
public class CodeCompletionPopup {

    private final Popup popup;
    private final ListView<CompletionItem> listView;
    private final EditorAdapter editor;
    private final boolean isDark;

    private ProgrammingLanguage currentLanguage = ProgrammingLanguage.CPP;
    private String activePrefix = "";
    private boolean justAcceptedCompletion = false;

    public CodeCompletionPopup(EditorAdapter editor, boolean isDark) {
        this.editor = editor;
        this.isDark = isDark;
        this.popup = new Popup();
        this.popup.setAutoHide(true);

        VBox container = new VBox();
        container.setPrefWidth(320);
        container.setMaxWidth(380);
        container.setMaxHeight(260);

        String bg = "#1c2130";
        String border = "#30394f";
        String headerBg = "#151923";
        String footerBg = "#151923";
        String textMuted = "#707e94";

        container.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-border-color: " + border + ";" +
                "-fx-border-width: 1px;" +
                "-fx-background-radius: 6px; -fx-border-radius: 6px;"
        );

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.45));
        shadow.setRadius(14);
        shadow.setOffsetY(6);
        container.setEffect(shadow);

        // Header
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(5, 10, 5, 10));
        header.setStyle(
                "-fx-background-color: " + headerBg + ";" +
                "-fx-border-color: #283144; -fx-border-width: 0 0 1px 0;" +
                "-fx-background-radius: 6px 6px 0 0;"
        );

        Label title = new Label("⚡ IntelliSense (Tab / Space / ↵ to insert)");
        title.setStyle(
                "-fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;" +
                "-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #82aaff;"
        );
        header.getChildren().add(title);

        // ListView
        listView = new ListView<>();
        listView.setFocusTraversable(false);
        listView.setPrefHeight(180);
        listView.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-control-inner-background: " + bg + ";" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 2px;" +
                "-fx-border-color: transparent;"
        );

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CompletionItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(3, 6, 3, 6));

                    // Category badge
                    Label badge = new Label(item.category());
                    badge.setStyle(getBadgeStyle(item.category(), isDark));

                    // Label with bold monospace
                    Label label = new Label(item.label());
                    label.setStyle(
                            "-fx-font-family: 'JetBrains Mono', 'Fira Code', Menlo, Monaco, Consolas, monospace;" +
                            "-fx-font-size: 12px; -fx-font-weight: 700;" +
                            "-fx-text-fill: #d5deeb;"
                    );

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // Description
                    Label desc = new Label(item.description());
                    desc.setStyle("-fx-font-size: 10.5px; -fx-text-fill: " + textMuted + "; -fx-font-family: -apple-system, 'Segoe UI', sans-serif;");

                    row.getChildren().addAll(badge, label, spacer, desc);
                    setGraphic(row);

                    if (isSelected()) {
                        setStyle("-fx-background-color: #23354d; -fx-background-radius: 4px;");
                        label.setStyle(
                                "-fx-font-family: 'JetBrains Mono', 'Fira Code', Menlo, Monaco, Consolas, monospace;" +
                                "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #addb67;"
                        );
                        desc.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #82aaff; -fx-font-family: -apple-system, 'Segoe UI', sans-serif;");
                    } else {
                        setStyle("-fx-background-color: transparent;");
                    }
                }
            }
        });

        listView.setOnMouseClicked(e -> {
            CompletionItem selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                applyCompletion(selected);
            }
        });

        // Footer
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 10, 4, 10));
        footer.setStyle(
                "-fx-background-color: " + footerBg + ";" +
                "-fx-border-color: #283144; -fx-border-width: 1px 0 0 0;" +
                "-fx-background-radius: 0 0 6px 6px;"
        );

        Label hint = new Label("Esc to close • Ctrl+Space to reopen • ↑↓ navigate");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: " + textMuted + "; -fx-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;");
        footer.getChildren().add(hint);

        container.getChildren().addAll(header, listView, footer);
        popup.getContent().add(container);

        installEditorKeyFilters();
    }

    public CodeCompletionPopup(javafx.scene.control.TextArea codeEditorArea, boolean isDark) {
        this(new TextAreaEditorAdapter(codeEditorArea), isDark);
    }

    public CodeCompletionPopup(org.fxmisc.richtext.CodeArea codeArea, boolean isDark) {
        this(new CodeAreaEditorAdapter(codeArea), isDark);
    }


    private String getBadgeStyle(String category, boolean isDark) {
        return switch (category) {
            case "I/O" -> "-fx-background-color: #172b38; -fx-text-fill: #82aaff; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1px 5px; -fx-background-radius: 3px;";
            case "KEY" -> "-fx-background-color: #332238; -fx-text-fill: #c792ea; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1px 5px; -fx-background-radius: 3px;";
            case "TYPE" -> "-fx-background-color: #1c3330; -fx-text-fill: #3cc9b0; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1px 5px; -fx-background-radius: 3px;";
            case "FUNC" -> "-fx-background-color: #253322; -fx-text-fill: #addb67; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1px 5px; -fx-background-radius: 3px;";
            case "SNIP" -> "-fx-background-color: #382a24; -fx-text-fill: #ffcb8b; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1px 5px; -fx-background-radius: 3px;";
            default -> "-fx-background-color: #283144; -fx-text-fill: #d5deeb; -fx-font-size: 9px; -fx-font-weight: 700; -fx-padding: 1px 5px; -fx-background-radius: 3px;";
        };
    }

    public void setLanguage(ProgrammingLanguage lang) {
        this.currentLanguage = lang != null ? lang : ProgrammingLanguage.CPP;
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void hide() {
        if (popup.isShowing()) {
            popup.hide();
        }
    }

    public void showSuggestionsForCurrentCaret() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        String prefix = extractPrefix(text, caret);
        this.activePrefix = prefix;

        if (prefix.length() < 2) {
            hide();
            return;
        }

        List<CompletionItem> suggestions = CodeCompletionEngine.getSuggestions(currentLanguage, prefix);
        if (suggestions.isEmpty()) {
            hide();
            return;
        }

        listView.setPrefHeight(Math.min(220, Math.max(60, suggestions.size() * 32 + 8)));
        listView.setItems(FXCollections.observableArrayList(suggestions));
        listView.getSelectionModel().select(0);

        positionAndShow();
    }

    public void showAllSuggestionsExplicit() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        String prefix = extractPrefix(text, caret);
        this.activePrefix = prefix;

        List<CompletionItem> suggestions = CodeCompletionEngine.getSuggestions(currentLanguage, prefix);
        if (suggestions.isEmpty()) {
            suggestions = CodeCompletionEngine.getSuggestions(currentLanguage, "");
        }
        if (suggestions.isEmpty()) return;

        listView.setPrefHeight(Math.min(220, Math.max(60, suggestions.size() * 32 + 8)));
        listView.setItems(FXCollections.observableArrayList(suggestions));
        listView.getSelectionModel().select(0);

        positionAndShow();
    }

    private void positionAndShow() {
        Window window = editor.getNode().getScene() != null ? editor.getNode().getScene().getWindow() : null;
        if (window == null) return;

        double targetX;
        double targetY;

        Bounds b = editor.getCaretScreenBounds();
        if (b != null && b.getMinX() > 0 && b.getMaxY() > 0) {
            targetX = b.getMinX();
            targetY = b.getMaxY() + 4;
        } else {
            Bounds eb = editor.getNode().localToScreen(editor.getNode().getBoundsInLocal());
            targetX = eb != null ? eb.getMinX() + 65 : 100;
            targetY = eb != null ? eb.getMinY() + 45 : 100;
        }

        if (popup.isShowing()) {
            popup.setX(targetX);
            popup.setY(targetY);
        } else {
            popup.show(window, targetX, targetY);
        }
    }

    public void applyCompletion(CompletionItem item) {
        applyCompletion(item, false);
    }

    public void applyCompletion(CompletionItem item, boolean addSpaceIfNeeded) {
        if (item == null) return;

        int caret = editor.getCaretPosition();
        String text = editor.getText();
        String prefix = extractPrefix(text, caret);
        int replaceStart = caret - prefix.length();

        String replacement = item.insertText();
        if (addSpaceIfNeeded && !replacement.endsWith(" ") && !replacement.endsWith("\n") && !replacement.endsWith("(") && !replacement.endsWith("\"")) {
            replacement += " ";
        }
        editor.replaceText(replaceStart, caret, replacement);
        int targetCaret = replaceStart + replacement.length();
        editor.positionCaret(Math.min(targetCaret, editor.getText().length()));
        hide();
        editor.requestFocus();
    }

    public static String extractPrefix(String text, int caretPos) {
        if (text == null || caretPos <= 0 || caretPos > text.length()) return "";
        int start = caretPos - 1;
        while (start >= 0) {
            char c = text.charAt(start);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '#' || c == ':' || c == '.') {
                start--;
            } else {
                break;
            }
        }
        return text.substring(start + 1, caretPos);
    }

    private void installEditorKeyFilters() {
        // Intercept keys for popup navigation and IDE ergonomics
        editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
                showAllSuggestionsExplicit();
                event.consume();
                return;
            }

            if (popup.isShowing()) {
                if (event.getCode() == KeyCode.DOWN) {
                    int next = Math.min(listView.getItems().size() - 1, listView.getSelectionModel().getSelectedIndex() + 1);
                    listView.getSelectionModel().select(next);
                    listView.scrollTo(next);
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.UP) {
                    int prev = Math.max(0, listView.getSelectionModel().getSelectedIndex() - 1);
                    listView.getSelectionModel().select(prev);
                    listView.scrollTo(prev);
                    event.consume();
                    return;
                }
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.SPACE) {
                    CompletionItem selected = listView.getSelectionModel().getSelectedItem();
                    if (selected == null && !listView.getItems().isEmpty()) {
                        selected = listView.getItems().get(0);
                    }
                    if (selected != null) {
                        justAcceptedCompletion = true;
                        applyCompletion(selected, event.getCode() == KeyCode.SPACE);
                        event.consume();
                        return;
                    }
                }
                if (event.getCode() == KeyCode.ESCAPE) {
                    hide();
                    event.consume();
                    return;
                }
            }

            // Tab key support: Indent with 4 spaces instead of losing focus!
            if (event.getCode() == KeyCode.TAB && !popup.isShowing()) {
                if (event.isShiftDown()) {
                    unindentSelection();
                } else {
                    indentSelectionOrInsertSpaces();
                }
                event.consume();
                return;
            }

            // Smart Enter: Preserve line indentation and indent after '{' or ':'
            if (event.getCode() == KeyCode.ENTER && !popup.isShowing() && !event.isControlDown() && !event.isMetaDown()) {
                handleSmartEnter();
                event.consume();
                return;
            }

            // Backspace pair deletion
            if (event.getCode() == KeyCode.BACK_SPACE && !popup.isShowing()) {
                if (handleBackspacePair()) {
                    event.consume();
                    return;
                }
            }
        });

        // Typing triggers suggestions and auto-closing pairs
        editor.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (justAcceptedCompletion) {
                justAcceptedCompletion = false;
                event.consume();
                return;
            }

            String ch = event.getCharacter();
            if (ch == null || ch.isEmpty()) return;

            // Auto-closing brackets and quotes
            if (handleAutoClosingPairs(ch)) {
                event.consume();
                Platform.runLater(this::showSuggestionsForCurrentCaret);
                return;
            }

            // Trigger suggestion check on letters/digits
            Platform.runLater(this::showSuggestionsForCurrentCaret);
        });

        // Hide popup if caret moves via mouse click
        editor.setOnMouseClicked(e -> hide());
    }

    private boolean handleAutoClosingPairs(String ch) {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        if (text == null) text = "";

        char typed = ch.charAt(0);
        char next = caret < text.length() ? text.charAt(caret) : '\0';

        // Step-over closing characters if already present
        if ((typed == ')' && next == ')') ||
            (typed == ']' && next == ']') ||
            (typed == '}' && next == '}') ||
            (typed == '"' && next == '"') ||
            (typed == '\'' && next == '\'')) {
            editor.positionCaret(caret + 1);
            return true;
        }

        // Insert matching pairs
        String pair = switch (typed) {
            case '(' -> "()";
            case '[' -> "[]";
            case '{' -> "{}";
            case '"' -> "\"\"";
            case '\'' -> "''";
            default -> null;
        };

        if (pair != null) {
            String before = text.substring(0, caret);
            String after = text.substring(caret);
            editor.setText(before + pair + after);
            editor.positionCaret(caret + 1);
            return true;
        }

        return false;
    }

    private boolean handleBackspacePair() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        if (text == null || caret <= 0 || caret >= text.length()) return false;

        char prev = text.charAt(caret - 1);
        char next = text.charAt(caret);

        boolean isPair = (prev == '(' && next == ')') ||
                         (prev == '[' && next == ']') ||
                         (prev == '{' && next == '}') ||
                         (prev == '"' && next == '"') ||
                         (prev == '\'' && next == '\'');

        if (isPair) {
            String before = text.substring(0, caret - 1);
            String after = text.substring(caret + 1);
            editor.setText(before + after);
            editor.positionCaret(caret - 1);
            return true;
        }
        return false;
    }

    private void handleSmartEnter() {
        int caret = editor.getCaretPosition();
        String text = editor.getText();
        if (text == null) text = "";

        // Find current line start
        int lineStart = text.lastIndexOf('\n', Math.max(0, caret - 1));
        lineStart = lineStart == -1 ? 0 : lineStart + 1;
        String lineUntilCaret = text.substring(lineStart, caret);

        // Calculate leading indentation
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < lineUntilCaret.length(); i++) {
            char c = lineUntilCaret.charAt(i);
            if (c == ' ' || c == '\t') {
                indent.append(c);
            } else {
                break;
            }
        }

        // Check if line ends with '{' or ':'
        String trimmed = lineUntilCaret.trim();
        boolean extraIndent = trimmed.endsWith("{") || trimmed.endsWith(":");

        char nextChar = caret < text.length() ? text.charAt(caret) : '\0';
        boolean betweenBraces = trimmed.endsWith("{") && nextChar == '}';

        StringBuilder toInsert = new StringBuilder("\n").append(indent);
        if (extraIndent) {
            toInsert.append("    ");
        }

        if (betweenBraces) {
            int newCaretOffset = toInsert.length();
            toInsert.append("\n").append(indent);
            String before = text.substring(0, caret);
            String after = text.substring(caret);
            editor.setText(before + toInsert + after);
            editor.positionCaret(caret + newCaretOffset);
        } else {
            String before = text.substring(0, caret);
            String after = text.substring(caret);
            editor.setText(before + toInsert + after);
            editor.positionCaret(caret + toInsert.length());
        }
    }

    private void indentSelectionOrInsertSpaces() {
        IndexRange range = editor.getSelection();
        if (range == null || range.getLength() == 0) {
            // Insert 4 spaces at caret
            int caret = editor.getCaretPosition();
            String text = editor.getText();
            String before = text.substring(0, caret);
            String after = text.substring(caret);
            editor.setText(before + "    " + after);
            editor.positionCaret(caret + 4);
        } else {
            // Indent selected lines
            String text = editor.getText();
            int start = range.getStart();
            int end = range.getEnd();
            int lineStart = text.lastIndexOf('\n', Math.max(0, start - 1));
            lineStart = lineStart == -1 ? 0 : lineStart + 1;

            String prefix = text.substring(0, lineStart);
            String selectedBlock = text.substring(lineStart, end);
            String suffix = text.substring(end);

            String[] lines = selectedBlock.split("\n", -1);
            StringBuilder indented = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                indented.append("    ").append(lines[i]);
                if (i < lines.length - 1) indented.append("\n");
            }

            editor.setText(prefix + indented + suffix);
            editor.selectRange(lineStart, lineStart + indented.length());
        }
    }

    private void unindentSelection() {
        String text = editor.getText();
        IndexRange range = editor.getSelection();
        int start = range != null ? range.getStart() : editor.getCaretPosition();
        int end = range != null && range.getLength() > 0 ? range.getEnd() : start;

        int lineStart = text.lastIndexOf('\n', Math.max(0, start - 1));
        lineStart = lineStart == -1 ? 0 : lineStart + 1;

        String prefix = text.substring(0, lineStart);
        String selectedBlock = text.substring(lineStart, Math.max(lineStart, end));
        String suffix = text.substring(Math.max(lineStart, end));

        String[] lines = selectedBlock.split("\n", -1);
        StringBuilder unindented = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.startsWith("    ")) {
                unindented.append(l.substring(4));
            } else if (l.startsWith("\t")) {
                unindented.append(l.substring(1));
            } else {
                int spaces = 0;
                while (spaces < l.length() && l.charAt(spaces) == ' ') spaces++;
                unindented.append(l.substring(spaces));
            }
            if (i < lines.length - 1) unindented.append("\n");
        }

        editor.setText(prefix + unindented + suffix);
        editor.selectRange(lineStart, lineStart + unindented.length());
    }
}
