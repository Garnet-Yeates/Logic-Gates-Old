package edu.wit.yeatesg.logicgates.circuit.entity;

import javafx.beans.value.ObservableValue;

public interface PropertyMutable {

    String getPropertyTableHeader();

    PropertyList getPropertyList();

    void onPropertyChange(String propertyName, String old, String newVal);

    void onPropertyChangeViaTable(String propertyName, String old, String newVal);

    default void onPropertyChangeViaTable(String propertyName, String newVal) {
        onPropertyChangeViaTable(propertyName, getPropertyValue(propertyName), newVal);
    }


    default void onPropertyChange(String propertyName, String newValue) {
        onPropertyChange(propertyName, getPropertyValue(propertyName), newValue);
    }

    String getPropertyValue(String propertyName);

    boolean hasProperty(String propertyName);
}
