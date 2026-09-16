package dev.codetrail.desktop.controller;

import dev.codetrail.desktop.simulation.input.SimulationInputAdapter.Field;
import dev.codetrail.desktop.simulation.input.SimulationInputAdapter.Kind;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** One FXML-composed, labelled student input; only its applicable control is managed. */
public final class SimulationInputFieldController {
    @FXML private Label fieldLabel;
    @FXML private Label fieldHelp;
    @FXML private TextField singleInput;
    @FXML private TextArea rowInput;
    @FXML private ComboBox<String> choiceInput;
    private Field field;

    public void configure(Field field, String value) {
        this.field = field;
        fieldLabel.setText(field.label());
        fieldHelp.setText(field.help());
        active(singleInput, !field.multiline() && field.kind() != Kind.CHOICE);
        active(rowInput, field.multiline());
        active(choiceInput, field.kind() == Kind.CHOICE);
        choiceInput.getItems().setAll(field.choices());
        singleInput.setAccessibleText(field.label());
        rowInput.setAccessibleText(field.label());
        choiceInput.setAccessibleText(field.label());
        Node control = field.kind() == Kind.CHOICE ? choiceInput : field.multiline() ? rowInput : singleInput;
        fieldLabel.setLabelFor(control);
        setValue(value);
    }

    public String value() {
        return field.kind() == Kind.CHOICE ? choiceInput.getValue() : field.multiline() ? rowInput.getText() : singleInput.getText();
    }

    public void setValue(String value) {
        singleInput.setText(value);
        rowInput.setText(value);
        choiceInput.setValue(value);
    }

    private static void active(Node node, boolean active) { node.setVisible(active); node.setManaged(active); }
}
