package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.PointSet;
import edu.wit.yeatesg.logicgates.connections.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

public abstract class Entity {

    protected Circuit c;

    public Entity(Circuit c) {
        this.c = c;
    }

    public abstract int getStrokeSize();

    public Stroke getStroke() {
        return new BasicStroke(getStrokeSize());
    }

    public Circuit getCircuit() {
        return c;
    }

    protected PointSet drawPoints;

    public abstract PointSet getInterceptPoints();


    public boolean invalidlyIntercepts(Entity e) {
        return getInvalidInterceptPoints(e) != null && getInvalidInterceptPoints(e).size() > 0;
    }


    public abstract PointSet getInvalidInterceptPoints(Entity e);

    public boolean intercepts(Entity other) {
        return getInterceptPoints(other).size() > 0;
    }

    public PointSet getInterceptPoints(Entity other) {
        return getInterceptPoints().intersection(other.getInterceptPoints());
    }

    public abstract BoundingBox getBoundingBox();

    protected boolean deleted = false;

    public boolean intercepts(CircuitPoint p) {
        return getInterceptPoints().contains(p);
    }

    public boolean interceptsAny(CircuitPoint... points) {
        for (CircuitPoint p : points)
            if (intercepts(p))
                return true;
        return false;
    }

    public boolean interceptsNone(CircuitPoint... points) {
        return !interceptsAny(points);
    }

    /** For drawing */

    public abstract boolean canMoveBy(Vector vector);


    public abstract void draw(Graphics2D g);


    public boolean delete() {
        deleted = true;
        return c.removeEntity(this);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public abstract void onDelete();

    public abstract boolean equals(Object other);

    public boolean interceptsAll(CircuitPoint... pts) {
        for (CircuitPoint p : pts)
            if (!intercepts(p))
                return false;
        return true;
    }

    public static class InterceptPermit {
        public Entity entity;
        public CircuitPoint allowedToConnectAt;

        public InterceptPermit(Entity e, CircuitPoint p) {
            this.entity = e;
            this.allowedToConnectAt = p;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof InterceptPermit && ((InterceptPermit) o).entity.equals(entity) &&
                    ((InterceptPermit) o).allowedToConnectAt.equals(allowedToConnectAt);
        }

        @Override
        public String toString() {
            return "InterceptException{" +
                    "entity=" + entity +
                    ", allowedToConnectAt=" + allowedToConnectAt +
                    '}';
        }
    }

    public static class PermitList extends LinkedList<InterceptPermit> {
        public PermitList(InterceptPermit... exceptions) {
            this.addAll(Arrays.asList(exceptions));
        }

        public PermitList() {
            super();
        }

        public PermitList(PermitList cloning) {
            super(cloning);
        }
    }

    public abstract boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo,
                                                                 PermitList exceptions,
                                                                 boolean strictWithWires);


}