package edu.wit.yeatesg.logicgates.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class InputNode extends ConnectionNode implements Dependent {

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    protected DependencyList transmitterList = new DependencyList(this);

    @Override
    public DependencyList dependingOn() {
        return transmitterList;
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

    @Override
    public void draw(GraphicsContext g) {
        g.setStroke(Color.BLACK);
        g.setFill(getPowerStatus().getColor());
        double circleSize = parent.getCircuit().getScale() * 0.6;
        if (getLocation().getCircuit().getScale() == 5)
            circleSize *= 1.35;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
