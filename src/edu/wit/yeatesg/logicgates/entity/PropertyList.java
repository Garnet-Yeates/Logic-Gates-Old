package edu.wit.yeatesg.logicgates.entity;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Arrays;
import java.util.LinkedList;

public class PropertyList extends LinkedList<Property> {

    private LinkedList<Dynamic> parents;

    public PropertyList(Dynamic... parents) {
        this.parents = new LinkedList<>();
        for (Dynamic d : parents)
            addParent(d);
    }

    public void addParent(Dynamic parent) {
        parents.add(parent);
        for (Property p : this.clone())
            if (!parent.hasProperty(p.getPropertyName()))
                remove(p);
    }

    @Override
    public boolean add(Property property) {
        for (Dynamic p : parents)
            if (!p.hasProperty(property.getPropertyName()))
                return false;
        for (Dynamic p : parents)
            property.addChangeListener((p::onPropertyChange));
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
