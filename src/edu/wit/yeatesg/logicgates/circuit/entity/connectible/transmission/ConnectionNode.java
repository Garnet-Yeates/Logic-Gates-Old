package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.LogicGates;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;

public class ConnectionNode implements Powerable {

   protected CircuitPoint location;
   private ConnectibleEntity connectedTo;
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

   public static final int MAX_DATA_BITS = 16;

   private int numBits = 1;

   public void setNumBits(int numBits) {
       if (numBits < 1 || numBits > MAX_DATA_BITS)
           throw new RuntimeException("Invalid Number Of Data Bits");
       this.numBits = numBits;
   }

   public int getNumBits() {
       return numBits;
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
       return new ConnectionNode(location.clone(onto), parent.getCloned(onto), connectedTo.getCloned(onto));
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

    @Override
    public void setPowerValue(PowerValue value) {
       powerValue = value;
    }

    public ConnectibleEntity getParent() {
        return parent;
    }

    private PowerValue powerValue = PowerValue.FLOATING;

    @Override
    public PowerValue getPowerValue() {
        return powerValue;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConnectionNode) {
            ConnectionNode o = (ConnectionNode) other;
            // Needs to be EXACT equals (in memory) for parent and connectedTo. Location needs to be sim simma
            return o.parent == parent && o.connectedTo == connectedTo && o.location.equals(location);
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

    public DependencyTree dependencyTree;

    @Override
    public DependencyTree getDependencyTree() {
        return dependencyTree;
    }

    @Override
    public void setDependencyTree(DependencyTree tree) {
        dependencyTree = tree;
    }

    @Override
    public Circuit getCircuit() {
        return location.getCircuit();
    }

    private boolean marked = false;

    @Override
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    @Override
    public boolean isMarked() {
        return marked;
    }

    public boolean hasDependencyTree() {
        return dependencyTree != null;
    }

    private int oscillationIndex = -1;

    public void drawOscillationNumber(GraphicsContext g) {
      //  LogicGates.drawText(oscillationIndex + "", getCircuit().getLineWidth(), getCircuit(), g, Color.BLACK, getLocation(), getCircuit().getScale()*1.2);
    }

    @Override
    public int getOscillationIndex() {
        return oscillationIndex;
    }

    @Override
    public void setOscillationIndex(int index) {
        oscillationIndex = index;
    }

}