package application.client.dsa.judge;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class TextAreaEditorAdapter implements EditorAdapter {
    private final TextArea textArea;

    public TextAreaEditorAdapter(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public int getCaretPosition() {
        return textArea.getCaretPosition();
    }

    @Override
    public String getText() {
        return textArea.getText();
    }

    @Override
    public void setText(String text) {
        textArea.setText(text);
    }

    @Override
    public void replaceText(int start, int end, String text) {
        textArea.replaceText(start, end, text);
    }

    @Override
    public void positionCaret(int pos) {
        textArea.positionCaret(pos);
    }

    @Override
    public void selectRange(int anchor, int caretPosition) {
        textArea.selectRange(anchor, caretPosition);
    }

    @Override
    public IndexRange getSelection() {
        return textArea.getSelection();
    }

    @Override
    public void requestFocus() {
        textArea.requestFocus();
    }

    @Override
    public Node getNode() {
        return textArea;
    }

    @Override
    public Bounds getCaretScreenBounds() {
        Node caret = textArea.lookup(".caret");
        if (caret != null) {
            return caret.localToScreen(caret.getBoundsInLocal());
        }
        return null;
    }

    @Override
    public void addEventFilter(EventType<KeyEvent> type, EventHandler<? super KeyEvent> handler) {
        textArea.addEventFilter(type, handler);
    }

    @Override
    public void setOnMouseClicked(EventHandler<? super MouseEvent> handler) {
        textArea.setOnMouseClicked(handler);
    }
}
