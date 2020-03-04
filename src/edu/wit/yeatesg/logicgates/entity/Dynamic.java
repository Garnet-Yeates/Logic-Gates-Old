package edu.wit.yeatesg.logicgates.entity;

import javafx.beans.value.ObservableValue;

public interface Dynamic {

    String getPropertyTableHeader();

    PropertyList getPropertyList();

    void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1);

    boolean hasProperty(String propertyName);
}
