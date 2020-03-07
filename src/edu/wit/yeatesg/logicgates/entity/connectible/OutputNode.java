package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.LinkedList;

public class OutputNode extends ConnectionNode implements Dependent {

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
        assert connectedTo instanceof Wire;
    }

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    private DependentParentList dependencies = new DependentParentList(this);

    @Override
    public DependentParentList getDependencyList() {
        return dependencies;
    }

    private State state;

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public State getState() {
        return state;
    }

    public boolean isIndependent() {
        return parent != null && parent.isIndependent();
    }

    private void calculateDependedBy(LinkedList<Wire> checked, Wire connectingWire) {
        if (connectingWire.getFullConnections().size() > 1) {
            for (ConnectibleEntity ce : connectingWire.getConnectedEntities()) {
                if (ce instanceof Wire && !checked.contains(ce)) {
                    Wire w = (Wire) ce;
                    w.getDependencyList().add(this);
                    checked.add(w);
                    calculateDependedBy(checked, w);
                }
                else {
                    // MUST be ce.getConnectionTo(curr), not curr.getConnectionTo(ce) because curr (should)
                    // always be a wire and wires dont have distinct input/output nodes
                    ConnectionNode node = ce.getConnectionTo(connectingWire);
                    if (node instanceof InputNode) {
                        ((InputNode) node).getDependencyList().add(this);
                    }
                }
            }
        }
    }

    // During refreshTransmissions, this should be called on every entity that has OutputNodes
    // TODO make sure output/input nodes connect to wires only
    public void calculateDependedBy() {
        if (connectedTo instanceof Wire && getState() != State.ILLOGICAL) {
            LinkedList<Wire> checked = new LinkedList<>();
            Wire w = (Wire) connectedTo;
            checked.add(w);
            w.getDependencyList().add(this);
            calculateDependedBy(checked, w);
        }
    }

    @Override
    public void draw(GraphicsContext g) {
        g.setStroke(Color.BLACK);
        g.setFill(getState().getColor());
        int circleSize = (int) (parent.getCircuit().getScale() * 0.4);
        if (circleSize % 2 != 0) circleSize++;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
