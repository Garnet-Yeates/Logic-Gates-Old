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
                    if (OutputNode.this.parent.getState() != State.ILLOGICAL)
                        w.dependencies.add(OutputNode.this.parent);
                    checked.add(w);
                    calculateDependedBy(checked, w);
                }
                else {
                    // MUST be ce.getConnectionTo(curr), not curr.getConnectionTo(ce) because curr (should)
                    // always be a wire and wires dont have distinct input/output nodes
                    ConnectionNode node = ce.getConnectionTo(connectingWire);
                    if (node instanceof InputNode) {
                        if (OutputNode.this.getState() != State.ILLOGICAL)
                            ((InputNode) node).dependencies.add(OutputNode.this);
                        if (OutputNode.this.parent.getState() != State.ILLOGICAL)
                            ce.dependencies.add(OutputNode.this.parent);
                        // don't return, because we want 'checked' to fill up with ALL wires on the dependent path
                    }
                }
            }
        }
    }

    // During refreshTransmissions, this should be called on every entity that has OutputNodes
    public void calculateDependedBy() {
        if (connectedTo != null && connectedTo instanceof Wire && this.parent.getState() != State.ILLOGICAL) {
            LinkedList<Wire> checked = new LinkedList<>();
            Wire w = (Wire) connectedTo;
            checked.add(w);
            w.dependencies.add(this.parent);
            calculateDependedBy(checked, w);
        }
    }

    public void determinePowerState() {
        // Do state of the parent, but if the parent has input nodes and at least one of them doesnt have a super dependency, make it
        // partial color
        if (connectedTo != null) {
            if (connectedTo.getState() == State.ILLOGICAL) {
                setState(State.ILLOGICAL);
                return;
            }
            if (connectedTo.getState() == State.PARTIALLY_DEPENDENT) {
                setState(State.PARTIALLY_DEPENDENT);
                return;
            }
        }
        if (parent.hasInputNodes()) {
            boolean foundOneWithSuperDepend = false;
            boolean foundOneWithPartialDepend = false;
            for (InputNode in : parent.getInputNodes()) {
                if (in.hasSuperDependencies()) {
                    foundOneWithSuperDepend = true;
                    foundOneWithPartialDepend = true;
                    break;
                }
                if (in.hasPartialDependencies())
                    foundOneWithPartialDepend = true;
            }
            if (foundOneWithSuperDepend)
                setState(parent.getState());
            else if (foundOneWithPartialDepend)
                setState(State.PARTIALLY_DEPENDENT);
            else
                setState(State.NO_DEPENDENT);
        }
        else setState(parent.getState());
    }
}
