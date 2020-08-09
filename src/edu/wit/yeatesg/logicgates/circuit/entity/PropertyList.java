package edu.wit.yeatesg.logicgates.circuit.entity;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.LinkedList;

public class PropertyList extends LinkedList<Property> {

    private LinkedList<PropertyMutable> parents;

    private Circuit c;

    public PropertyList(PropertyMutable mainParent, Circuit c) {
        parents = new LinkedList<>();
        parents.add(mainParent);
        this.c = c;
    }

    /**
     * Merges this property list with another one. Any property in this list that the other one doesn't have is
     * removed from this list
     * @param other the PropertyList that this one is being merged with
     */
    @SuppressWarnings("unchecked")
    public void merge(PropertyList other) {
        for (Property p: new ArrayList<>(this)) {
            boolean found = false;
            for (Property p2 : other) {
                if (p2.equals(p)) {
                    found = true;
                    if (p2.getPropertyValue() instanceof ComboBox) {
                        ComboBox<String> box = (ComboBox<String>) p.getPropertyValue();
                        ComboBox<String> p2Box = (ComboBox<String>) p2.getPropertyValue();
                        if (!box.getValue().equalsIgnoreCase(p2Box.getValue()))
                            box.setValue("");
                    } else if (p2.getPropertyValue() instanceof TextField) {
                        TextField field = (TextField) p.getPropertyValue();
                        TextField p2field = (TextField) p2.getPropertyValue();
                        if (!field.getText().equalsIgnoreCase(p2field.getText()))
                            field.setText("");
                    }
                    break;
                }
            }

            if (!found)
                remove(p);
        }
    }


    public void addParent(PropertyMutable parent) {
        merge(parent.getPropertyList());
        parents.add(parent);
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

        /*
         * If i want to make it so enter has to be pressed or focus has to be lost for text field value property changes
         * to take effect, i need to make my own TableView subclass with a parents field. then in the change listener
         * below I need to make it so if it is a combo box value, it insta changes on property change. if it is not
         * a combo box value (meaning its a text field), then i need to have a HashMap in my custom TableView class
         * of 'recently changed properties' they keys string property names to new values, so that when focus is lost
         * from the table view or enter is pressed (ill have to add an enter listener to the text field as well), it
         * calls onPropertyChange then. For now, im just gonna make it change the properties as you type in the text field
         */

        for (Property prop : this) {
            tableView.getItems().add(prop);
            prop.addChangeListener((propertyName, oldVal, newVal) -> {
                for (PropertyMutable parent : parents)
                    parent.onPropertyChangeViaTable(prop.getPropertyName(), oldVal, newVal);
                c.appendCurrentStateChanges("Change Property '" + propertyName + "' to " + newVal);
                c.recalculateTransmissions(); // TODO maybe this is inefficient
                c.repaint();
            });
        }
        return tableView;
    }
}
