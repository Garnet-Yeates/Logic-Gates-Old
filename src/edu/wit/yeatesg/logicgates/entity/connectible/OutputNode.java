package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.LinkedList;

public class OutputNode extends ConnectionNode {

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    public void calculateDependedBy(LinkedList<Wire> checked, Wire connectingWire) {
        if (connectingWire.getFullConnections().size() > 1) {
            for (ConnectibleEntity ce : connectingWire.getConnectedEntities()) {
                if (ce instanceof Wire && !checked.contains(ce)) {
                    Wire w = (Wire) ce;
                    w.dependencies.put(OutputNode.this.parent, checked);
                    checked.add(w);
                    calculateDependedBy(checked, w);
                }
                else {
                    // MUST be ce.getConnectionTo(curr), not curr.getConnectionTo(ce) because curr (should)
                    // always be a wire and wires dont have distinct input/output nodes
                    ConnectionNode node = ce.getConnectionTo(connectingWire);
                    if (node instanceof InputNode) {
                        ((InputNode) node).dependencies.put(OutputNode.this, checked);
                        ce.dependencies.put(OutputNode.this.parent, checked);
                        // don't return, because we want 'checked' to fill up with ALL wires on the dependent path
                    }
                }
            }
        }
    }

    // During refreshTransmissions, this should be called on every entity that has OutputNodes
    public void calculateDependedBy() {
        if (connectedTo != null && connectedTo instanceof Wire) {
            LinkedList<Wire> checked = new LinkedList<>();
            Wire w = (Wire) connectedTo;
            w.dependencies.put(this.parent, checked);
            calculateDependedBy(checked, w);
        }
    }

    public void determinePowerState() {
        if (parent.isIndependent() && connectedTo == null)
            setState(parent.getState());
        else
            setState(connectedTo == null ? State.PARTIALLY_DEPENDENT : connectedTo.getState());
    }
}
