package edu.wit.yeatesg.logicgates.entity;

import javafx.beans.value.ObservableValue;

public interface PropertyMutable {

    String getPropertyTableHeader();

    PropertyList getPropertyList();

    void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1);

    boolean hasProperty(String propertyName);
}
