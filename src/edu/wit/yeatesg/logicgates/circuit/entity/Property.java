package edu.wit.yeatesg.logicgates.circuit.entity;

import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.OutputType;
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

    public static String[] possibleNumInputs = { "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28",
            "29", "30", "31", "32"};

    public static String[] possibleSizes = { "Normal",  "Small" };

    public static String[] possibleDataBits;
    static {
        possibleDataBits = new String[ConnectionNode.MAX_DATA_BITS];
        for (int i = 0; i < possibleDataBits.length; i++)
            possibleDataBits[i] = (i + 1) + "";
    }

    public static String[] possibleOutTypes;
    static {
        possibleOutTypes = new String[OutputType.values().length];
        for (int i = 0; i < OutputType.values().length - 1; i++)
            possibleOutTypes[i] = OutputType.values()[i].getSimpleString();
    }


}

