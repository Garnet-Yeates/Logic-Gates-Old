package edu.wit.yeatesg.logicgates.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Comparator;

public class ConnectionNode implements Dependent {

   protected CircuitPoint location;
   protected ConnectibleEntity connectedTo;
   protected ConnectibleEntity parent;

   protected Vector vectorToParent;

   public ConnectionNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
       this.location = location.getSimilar();
       this.connectedTo = connectedTo;
       this.parent = connectingFrom;
   }

   public static Comparator<ConnectionNode> getHorizontalComparator() {
       return Comparator.comparingDouble(node -> node.location.x);
   }

   public static Comparator<ConnectionNode> getVerticalComparator() {
       return Comparator.comparingDouble(o -> o.location.y);
   }

   // Dependent implementation

    protected DependencyList dependingOn = new DependencyList(this);

    @Override
    public DependencyList dependingOn() {
        return dependingOn;
    }

    protected PowerStatus powerStatus = PowerStatus.UNDETERMINED;

    @Override
    public void setPowerStatus(PowerStatus status) {
        this.powerStatus = status;
    }

    @Override
    public PowerStatus getPowerStatus() {
        return powerStatus;
    }


    protected boolean isNegated = false;

    public void negate() {
        if (!(this instanceof InputNode || this instanceof OutputNode))
            throw new RuntimeException();
        isNegated = true;
    }

    public boolean isNegated() {
        return isNegated;
    }

    /**
     * Takes isNegated into account, used for determining the power of what depends on this
     */
    public PowerStatus getTruePowerValue() {
        if (powerStatus == PowerStatus.OFF || powerStatus == PowerStatus.ON)
            return !isNegated ? powerStatus : powerStatus == PowerStatus.ON ? PowerStatus.OFF : PowerStatus.ON;
        return powerStatus;
    }


    /**
     * Should be a unit vector going towards the parent
     * @return
     */
   public Vector getVectorToParent() {
       return vectorToParent.clone();
   }

    /**
     * Since this is really only used when the node is negated we can assume two things:<br>
     * - This is either an InputNode or OutputNode<br>
     * - The parent of this non volatile node either implements InputNegatable an OutputNegatable
     * this means that setVectorToParent() is done in the postConstructInput/OutputNodes() method of InputOutput/OutputNegatable
     * so don't worry about it
     * @param vectorToParent
     */
   public void setVectorToParent(Vector vectorToParent) {
        this.vectorToParent = vectorToParent;
    }

   public ConnectionNode clone(Circuit onto) {
       return new ConnectionNode(location.clone(onto), parent.clone(onto), connectedTo.clone(onto));
   }

   public ConnectionNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
       this(location, connectingFrom, null);
   }

    public CircuitPoint getLocation() {
        return location.getSimilar();
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

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConnectionNode) {
            ConnectionNode o = (ConnectionNode) other;
            // Needs to be EXACT equals (in memory) for parent and connectedTo. Location needs to be sim simma
            return o.parent == parent && o.connectedTo == connectedTo && o.location.equals(location);
           /*
            String cTo = connectedTo == null ? "null" : connectedTo.toString();
            String oCTo = o.connectedTo == null ? "null" : o.connectedTo.toString();
           return (o.parent.equals(parent)
                     && ((ConnectionNode) other).location.equals(location)
                     && !((connectedTo == null || o.connectedTo == null) && !(o.connectedTo == null && connectedTo == null))
                     && oCTo.equalsIgnoreCase(cTo));*/
        }
        return false;
    }

    public void draw(GraphicsContext g) {
       draw(g, null, 1);
    }

    public void draw(GraphicsContext g, Color col, double opacity) { }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{location=" + location +
                '}';
    }

    /**
     *
     * @param center
     * @param size the side length of the square that encloses the circle
     */
    public static void drawNegationCircle(GraphicsContext g, Color col, CircuitPoint center, double size) {
        Color strokeCol = col == null ? Color.BLACK : col;
        size /= 2.0;
        BoundingBox bb = new BoundingBox(center.getIfModifiedBy(new Vector(-size, -size)), center.getIfModifiedBy(new Vector(size, size)), null);
        PanelDrawPoint p1 = bb.p1.toPanelDrawPoint();
        double dist = (bb.p4.x - bb.p1.x) * center.getCircuit().getScale();
        g.setStroke(strokeCol);
        g.strokeOval(p1.x, p1.y, dist, dist);
    }

}