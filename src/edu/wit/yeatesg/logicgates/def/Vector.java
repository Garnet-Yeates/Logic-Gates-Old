package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.Direction;
import edu.wit.yeatesg.logicgates.connections.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Vector {

    public static final Vector ZERO_VECTOR = new Vector(0, 0);
    public static final Vector RIGHT = new Vector(1, 0);
    public static final Vector LEFT = new Vector(-1, 0);
    public static final Vector DOWN = new Vector(0, 1);
    public static final Vector UP = new Vector(0, -1);

    private static final Vector[] DIRECTION_VECS = new Vector[] { RIGHT, LEFT, DOWN, UP };

    public double x;
    public double y;

    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector(double x1, double y1, double x2, double y2) {
        this(x2 - x1, y2 - y1);
    }

    public Vector(CircuitPoint startPoint, CircuitPoint endPoint) {
        this(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
    }

    public static List<Vector> getDirectionVecs() {
        return Arrays.asList(DIRECTION_VECS);
    }

    public Vector(PanelDrawPoint startPoint, PanelDrawPoint endPoint) {
        this(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
    }

    public Vector getMultiplied(double multiplyingBy) {
        return new Vector(x * multiplyingBy, y * multiplyingBy);
    }

    public Vector getDivided(double divingBy) {
        return getMultiplied(1.0 / divingBy);
    }

    public double getLength() {
        return Math.sqrt(x*x + y*y);
    }

    public Vector getUnitVector() {
        return getDivided(getLength());
    }

    public Vector getParallelVectorWithLength(double length) {
        return getUnitVector().getMultiplied(length);
    }

    public Vector getParallelVectorWithPercentOfLength(double percentOfLength) {
        double desiredLen = percentOfLength * getLength();
        return getParallelVectorWithLength(desiredLen);
    }

    public PanelDrawPoint addedTo(PanelDrawPoint drawPoint) {
        return drawPoint.getIfModifiedBy(this);
    }

    public CircuitPoint addedTo(CircuitPoint point) {
        return point.getIfModifiedBy(this);
    }

    public static Vector directionVectorFrom(CircuitPoint from, CircuitPoint to, Direction dir) {
        if (dir == Direction.VERTICAL) {
            if (from.y < to.y)
                return DOWN;
            else if (from.y > to.y)
                return UP;
            else return null;
        } else {
            if (from.x < to.x)
                return RIGHT;
            else if (from.x > to.x)
                return LEFT;
            else return null;
        }
    }

    public static Direction getGeneralDirection(Vector v) {
        if (v == null)
            return null;
        if (v.equals(RIGHT) || v.equals(LEFT))
            return Direction.HORIZONTAL;
        else if (v.equals(UP) || v.equals(DOWN))
            return Direction.VERTICAL;
        else
            return null;
    }

    public static Direction getGeneralDirection(CircuitPoint p1, CircuitPoint p2) {
        if (!p1.isInLineWith(p2))
            throw new RuntimeException();
        return p1.x == p2.x ? Direction.VERTICAL : Direction.HORIZONTAL;
    }

    @Override
    public String toString() {
        return "Vector{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public boolean equals(Object other) {
        return other instanceof Vector && ((Vector) other).x == x && ((Vector) other).y == y;
    }
}
