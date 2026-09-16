package application.client.dsa.judge;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.fxmisc.richtext.CodeArea;

public class CodeAreaEditorAdapter implements EditorAdapter {
    private final CodeArea codeArea;

    public CodeAreaEditorAdapter(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    @Override
    public int getCaretPosition() {
        return codeArea.getCaretPosition();
    }

    @Override
    public String getText() {
        return codeArea.getText();
    }

    @Override
    public void setText(String text) {
        codeArea.replaceText(text != null ? text : "");
    }

    @Override
    public void replaceText(int start, int end, String text) {
        codeArea.replaceText(start, end, text);
    }

    @Override
    public void positionCaret(int pos) {
        codeArea.moveTo(Math.max(0, Math.min(pos, codeArea.getLength())));
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        codeArea.selectRange(anchor, caretPosition);
    }

    @Override
    public IndexRange getSelection() {
        return codeArea.getSelection();
    }

    @Override
    public void requestFocus() {
        codeArea.requestFocus();
    }

    @Override
    public Node getNode() {
        return codeArea;
    }

    @Override
    public Bounds getCaretScreenBounds() {
        return codeArea.getCaretBounds().orElse(null);
    }

    @Override
    public void addEventFilter(EventType<KeyEvent> type, EventHandler<? super KeyEvent> handler) {
        codeArea.addEventFilter(type, handler);
    }

    @Override
    public void setOnMouseClicked(EventHandler<? super MouseEvent> handler) {
        codeArea.setOnMouseClicked(handler);
    }
}
