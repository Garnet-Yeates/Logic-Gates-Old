package edu.wit.yeatesg.logicgates.entity;

import javafx.beans.value.ObservableValue;

public interface PropertyMutable {

    String getPropertyTableHeader();

    PropertyList getPropertyList();

    void onPropertyChange(String str, String old, String newVal);

    String getPropertyValue(String propertyName);

    boolean hasProperty(String propertyName);
}
