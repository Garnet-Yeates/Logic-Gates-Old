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
                Dependent thatDependsOnThis = null;
                if (ce instanceof Wire)
                    thatDependsOnThis = (Dependent) ce;
                else if (ce.getConnectionTo(connectingWire) instanceof InputNode)
                    thatDependsOnThis = (Dependent) ce.getConnectionTo(connectingWire);
                if (thatDependsOnThis == null || thatDependsOnThis.getPowerStatus() != PowerStatus.UNDETERMINED)
                    continue;
                if (!thatDependsOnThis.dependingOn().contains(this)) {
                    thatDependsOnThis.dependingOn().add(this);
                    if (thatDependsOnThis instanceof Wire)
                        calculateDependedBy((Wire) thatDependsOnThis);
                }
            }
        }
    }

    // During refreshTransmissions, this should be called on every entity that has OutputNodes
    public final void calculateDependedBy() {
        if (connectedTo instanceof Wire && getPowerStatus() == PowerStatus.UNDETERMINED) {
            Wire w = (Wire) connectedTo;
            w.dependingOn().add(this);
            calculateDependedBy(w);
        }
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        col = col == null ? getPowerStatus().getColor() : col;
        col = Color.rgb((int) (255*col.getRed()), (int) (255*col.getGreen()), (int) (255*col.getBlue()), opacity);
        g.setFill(col);
        double circleSize = parent.getCircuit().getScale() * 0.6;
        if (getLocation().getCircuit().getScale() == 5)
            circleSize *= 1.35;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
