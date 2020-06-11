package edu.wit.yeatesg.logicgates.entity;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.LinkedList;

public class PropertyList extends LinkedList<Property> {

    private LinkedList<PropertyMutable> parents;

    public PropertyList(PropertyMutable... parents) {
        this.parents = new LinkedList<>();
        for (PropertyMutable d : parents)
            addParent(d);
    }

    public void addParent(PropertyMutable parent) {
        parents.add(parent);
        for (Property p : this.clone())
            if (!parent.hasProperty(p.getPropertyName()))
                remove(p);
    }

    @Override
    public boolean add(Property property) {
        for (PropertyMutable p : parents)
            if (!p.hasProperty(property.getPropertyName()))
                return false;
        for (PropertyMutable p : parents)
            property.addChangeListener((observableValue, s, t1) -> p.onPropertyChange(observableValue.toString(), s, t1));
        return super.add(property);
    }

    @Override
    public PropertyList clone() {
        PropertyList clone = new PropertyList();
        clone.addAll(this);
        return clone;
    }

    @SuppressWarnings("unchecked")
    public TableView<Property> toTableView() {
        if (parents == null || parents.isEmpty())
            throw new RuntimeException("Can't Convert To TableView Without any parents");
        TableView<Property> tableView = new TableView<>();
        tableView.getColumns().clear();

        TableColumn<Property, Label> propCol = new TableColumn<>("Property Name");
        propCol.setCellValueFactory(new PropertyValueFactory<>("propertyName"));

        TableColumn<Property, Node> valCol = new TableColumn<>("Value");
        valCol.setCellValueFactory(new PropertyValueFactory<>("propertyValue"));

        String header = parents.size() == 1 ? parents.get(0).getPropertyTableHeader() :
                "Properties For: Multiple";
        TableColumn<Property, String> tableHeader = new TableColumn<>(header);

        tableHeader.getColumns().addAll(propCol, valCol);
        tableView.getColumns().add(tableHeader);

        for (Property prop : this)
            tableView.getItems().add(prop);

        return tableView;
    }
}
