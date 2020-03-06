package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ConnectionNode {

   protected CircuitPoint location;
   protected ConnectibleEntity connectedTo;
   protected ConnectibleEntity parent;

    private State state;

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

   public ConnectionNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
       this.location = location;
       this.connectedTo = connectedTo;
       this.parent = connectingFrom;
   }

   public ConnectionNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
       this(location, connectingFrom, null);
   }

    public CircuitPoint getLocation() {
        return location;
    }

    public void setLocation(CircuitPoint location) {
        this.location = location;
    }

    public ConnectibleEntity getConnectedTo() {
        return connectedTo;
    }

    public boolean hasConnectedEntity() {
       return getConnectedTo() != null;
    }

    public void setConnectedTo(ConnectibleEntity connectedTo) {
        this.connectedTo = connectedTo;
    }

    public ConnectibleEntity getParent() {
        return parent;
    }

    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
       return parent.canConnectTo(e, at);
    }

    public void setParent(ConnectibleEntity parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConnectionNode) {
            ConnectionNode o = (ConnectionNode) other;
            if ((connectedTo == null || o.connectedTo == null) && !(o.connectedTo == null && connectedTo == null))
                return false;
            else return (((ConnectionNode) other).parent.equals(parent)
                    && ((ConnectionNode) other).location.equals(location)
                    && ((ConnectionNode) other).connectedTo.equals(connectedTo));
        }
        return false;
    }

    public void draw(GraphicsContext g) {
        g.setStroke(Color.BLACK);
        g.setFill(state.getColor());
        int circleSize = (int) (parent.getCircuit().getScale() * 0.4);
        if (circleSize % 2 != 0) circleSize++;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "location=" + location +
                ", connectedTo=" + connectedTo +
                ", parent=" + parent +
                ", state=" + state +
                '}';
    }
}