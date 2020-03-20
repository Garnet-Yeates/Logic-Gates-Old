package edu.wit.yeatesg.logicgates.points;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;

public class CircuitPoint {

    private Circuit c;

    public double x;
    public double y;

    public CircuitPoint(double x, double y, Circuit c) {
        this.c = c;
        this.x = x;
        this.y = y;
    }

    public CircuitPoint(PanelDrawPoint p) {
        this((p.x - p.c.getXOffset()) / p.c.getScale(),
                (p.y - p.c.getYOffset()) / p.c.getScale(),
                p.c);
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
        for (Wire w : getInterceptingEntities().ofType(Wire.class))
            if (w.isEdgePoint(this))
                return true;
        return false;
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

    public EntityList<Entity> getInterceptingEntities() {
        return c.getInterceptMap().get(this.getGridSnapped());
    }

    public CircuitPoint clone(Circuit onto) {
        return new CircuitPoint(x, y, onto);
    }

    public String toParsableString() {
        return x + "," + y;
    }



}