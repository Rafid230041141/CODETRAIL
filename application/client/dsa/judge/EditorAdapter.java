package application.client.dsa.judge;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public interface EditorAdapter {
    int getCaretPosition();
    String getText();
    void setText(String text);
    void replaceText(int start, int end, String text);
    void positionCaret(int pos);
    void selectRange(int anchor, int caretPosition);
    IndexRange getSelection();
    void requestFocus();
    Node getNode();
    Bounds getCaretScreenBounds();
    void addEventFilter(EventType<KeyEvent> type, EventHandler<? super KeyEvent> handler);
    void setOnMouseClicked(EventHandler<? super MouseEvent> handler);
}
