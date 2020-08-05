package edu.wit.yeatesg.logicgates.circuit.entity;

import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Objects;

public class Property {

    private String propertyName;
    private Label propertyLabel;
    private Node propertyValue;

    public Property(String propertyName, String initialValue, String... possibleValues) {
        this.propertyName = propertyName;
        this.propertyLabel = new Label(propertyName);
        if (possibleValues.length == 0)
            throw new RuntimeException();
        if (possibleValues.length == 1) {
            TextField textField = new TextField(initialValue);
            textField.setText(initialValue);
            this.propertyValue = textField;
        }
        else {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setValue(initialValue);
            this.propertyValue = comboBox;
            comboBox.setPrefWidth(Integer.MAX_VALUE);
            for (String s : possibleValues)
                comboBox.getItems().add(s);
        }
    }

    @SuppressWarnings("unchecked")
    public void addChangeListener(ChangeListener<String> listener) {
        if (propertyValue instanceof ComboBox) {
            ((ComboBox<String>) propertyValue).valueProperty().addListener(listener);
        } else if (propertyValue instanceof TextField) {
            ((TextField) propertyValue).textProperty().addListener(listener);
        }
    }

    public boolean hasComboBoxValue() {
        return propertyValue instanceof ComboBox;
    }

    public boolean hasTextFieldValue() {
        return propertyValue instanceof TextField;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Property && ((Property) o).propertyName.equalsIgnoreCase(propertyName);
    }

    public Label getPropertyLabel() {
        return propertyLabel;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Node getPropertyValue() {
        return propertyValue;
    }
}

