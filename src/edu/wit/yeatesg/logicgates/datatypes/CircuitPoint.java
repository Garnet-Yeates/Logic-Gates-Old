package edu.wit.yeatesg.logicgates.datatypes;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.EntityList;
import edu.wit.yeatesg.logicgates.circuit.entity.ExactEntityList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire;

import java.util.Collection;

public class CircuitPoint {

    private Circuit c;

    public double x;
    public double y;

    public CircuitPoint(double x, double y, Circuit c) {
        this.c = c;
        this.x = x;
        this.y = y;
    }

    public CircuitPoint(String x, String y, Circuit c) {
        this(Double.parseDouble(x), Double.parseDouble(y), c);
    }

    public CircuitPoint(PanelDrawPoint p) {
        this((p.x - p.getCircuit().getXOffset()) / p.getCircuit().getScale(),
                (p.y - p.getCircuit().getYOffset()) / p.getCircuit().getScale(),
                p.getCircuit());
    }

    public double distance(CircuitPoint other) {
        return Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.y - y, 2));
    }

    public PanelDrawPoint toPanelDrawPoint() {
        return new PanelDrawPoint(this);
    }

    public Circuit getCircuit() {
        return c;
    }

    public CircuitPoint getGridSnapped() {
        return new CircuitPoint((int) Math.round(x), (int) Math.round(y), c);
    }

    public boolean representsOrigin() {
        return x == 0.0 && y == 0.0;
    }

    public CircuitPoint getIfModifiedBy(Vector vector) {
        return new CircuitPoint(x + vector.x, y + vector.y, c);
    }

    @Override
    public String toString() {
        return "[ " +
                 + x + " , " + y +
                " ]";
    }

    public boolean isInMapRange() {
        return c.isInMapRange(this);
    }

    public boolean isInLineWith(CircuitPoint other) {
        return x == other.x || y == other.y;
    }

    public boolean is4AdjacentTo(CircuitPoint other) {
        int xDiff = (int) Math.abs(other.x - this.x);
        int yDiff = (int) Math.abs(other.y - this.y);
        return (xDiff == 1 && yDiff == 0 || xDiff == 0 && yDiff == 1);
    }

    public boolean is8AdjacentTo(CircuitPoint other) {
        int xDiff = (int) Math.abs(other.x - this.x);
        int yDiff = (int) Math.abs(other.y - this.y);
        return (!(xDiff == 0 && yDiff == 0) && (xDiff <= 1 || yDiff <= 1));
    }

    /**
     * Clones this CircuitPoint
     * @return a clone of this CircuitPoint
     */
    @Override
    public CircuitPoint clone() {
        return getSimilar();
    }

    public boolean interceptsWireEdgePoint() {
        for (Wire w : getInterceptingEntities().thatExtend(Wire.class))
            if (w.isEdgePoint(this))
                return true;
        return false;
    }

    /**
     * Treats this CircuitPoint as a Point relative to 0,0, gets the vector from
     * 0,0 to this point, rotates that vector, and adds the rotated vector to the supplied origin
     * @param origin
     * @param rotation
     * @return
     */
    public CircuitPoint getRotated(CircuitPoint origin, int rotation) {
        Vector zeroToThis = new Vector(new CircuitPoint(0, 0, c), this);
        return origin.getIfModifiedBy(zeroToThis.getRotated(rotation));
    }

    public CircuitPoint getSimilar() {
        return clone(c);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CircuitPoint && ((CircuitPoint) o).isSimilar(this);
    }

    public boolean isSimilar(CircuitPoint other) {
        return other.x == x && other.y == y && other.c == c; // Circuit should be same in mem
    }

    public boolean intercepts(BoundingBox b) {
        return b.intercepts(this);
    }

    public boolean intercepts(CircuitPoint other) {
        return isSimilar(other);
    }

    public boolean intercepts(Entity e) {
        return e.intercepts(this);
    }

    public ExactEntityList<Entity> getInterceptingEntities() {
        return c.getInterceptMap().get(this.getGridSnapped());
    }

    public CircuitPoint clone(Circuit onto) {
        return new CircuitPoint(x, y, onto);
    }

    public String toParsableString() {
        return x + "," + y;
    }



}