package edu.wit.yeatesg.logicgates.points;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CircuitPoint that = (CircuitPoint) o;
        return this.x == that.x &&
                this.y == that.y &&
                this.c == that.c;
    }

    public boolean isInLineWith(CircuitPoint other) {
        return x == other.x || y == other.y;
    }

    public boolean is4AdjacentTo(CircuitPoint other) {
        int xdiff = (int) Math.abs(other.x - this.x);
        int ydiff = (int) Math.abs(other.y - this.y);
        return (xdiff == 1 && ydiff == 0 || xdiff == 0 && ydiff == 1);
    }

    public boolean is8AdjacentTo(CircuitPoint other) {
        int xdiff = (int) Math.abs(other.x - this.x);
        int ydiff = (int) Math.abs(other.y - this.y);
        return (!(xdiff == 0 && ydiff == 0) && (xdiff <= 1 || ydiff <= 1));
    }

    public boolean intercepts(BoundingBox b) {
        return b.intercepts(this);
    }

    /**
     * Clones this CircuitPoint
     * @return a clone of this CircuitPoint
     */
    @Override
    public CircuitPoint clone() {
        return new CircuitPoint(x, y, c);
    }

    public CircuitPoint clone(Circuit onto) {
        return new CircuitPoint(x, y, onto);
    }


}