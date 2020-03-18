package edu.wit.yeatesg.logicgates.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class OutputNode extends ConnectionNode implements Dependent {

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    private DependencyList receivingFrom = new DependencyList(this);

    @Override
    public DependencyList dependingOn() {
        return receivingFrom;
    }

    private PowerStatus powerStatus = PowerStatus.UNDETERMINED;

    @Override
    public void setPowerStatus(PowerStatus status) {
        this.powerStatus = status;
    }

    @Override
    public PowerStatus getPowerStatus() {
        return powerStatus;
    }

    public boolean isIndependent() {
        return parent != null && parent.transmitsOnly();
    }

    private void calculateDependedBy(Wire connectingWire) {
        if (connectingWire.getFullConnections().size() > 1) {
            for (ConnectibleEntity ce : connectingWire.getConnectedEntities()) {
                if (ce instanceof Wire && !((Wire) ce).dependingOn().contains(this)) {
                    Wire w = (Wire) ce;
                    w.dependingOn().add(this);
                    calculateDependedBy(w);
                } else {
                    // MUST be ce.getConnectionTo(curr), not curr.getConnectionTo(ce) because curr (should)
                    // always be a wire and wires dont have distinct input/output nodes
                    ConnectionNode node = ce.getConnectionTo(connectingWire);
                    if (node instanceof InputNode && !((InputNode) node).dependingOn().contains(this)) {
                        ((InputNode) node).dependingOn().add(this);
                    }
                }
            }
        }
    }

    // During refreshTransmissions, this should be called on every entity that has OutputNodes
    // TODO make sure output/input nodes connect to wires only
    public final void calculateDependedBy() {
        if (connectedTo instanceof Wire && getPowerStatus() == PowerStatus.UNDETERMINED) {
            Wire w = (Wire) connectedTo;
            w.dependingOn().add(this);
            calculateDependedBy(w);
        }
    }

    @Override
    public void draw(GraphicsContext g) {
        g.setStroke(Color.BLACK);
        g.setFill(getPowerStatus().getColor());
        double circleSize = parent.getCircuit().getScale() * 0.45;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
