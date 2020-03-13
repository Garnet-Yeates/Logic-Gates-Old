package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class InputNode extends ConnectionNode implements Dependent {

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
        setState(State.UNDETERMINED);
    }

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    protected DependentParentList dependencyList = new DependentParentList(this);

    @Override
    public DependentParentList getDependencyList() {
        return dependencyList;
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
