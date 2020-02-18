package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.connections.PointSet;
import edu.wit.yeatesg.logicgates.connections.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class Entity {

    protected Circuit c;

    protected CircuitPoint location;

    public Entity(CircuitPoint location) {
        c = location.getCircuit();
        this.location = location;
        if (canRotate()) {
            pointSetForDrawing = getRelativePointSet();
            setRotation(0);
        }
    }

    /** For theoretical entities, where you don't want them to have an actual location */
    public Entity(Circuit circuit) {
        c = circuit;
    }

    public abstract int getStrokeSize();

    public Stroke getStroke() {
        return new BasicStroke(getStrokeSize());
    }

    public Circuit getCircuit() {
        return c;
    }

    protected PointSet pointSetForDrawing;

    public abstract PointSet getInterceptPoints();


    public abstract BoundingBox getBoundingBox();

    public PointSet getRelativePointSet() {
        return null;
    }

    private int rotation;

    private int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        if (!canRotate())
            return;
        PointSet defaultSet = getRelativePointSet();
        ArrayList<Vector> relationshipToOriginSet = new ArrayList<>();
        CircuitPoint relativeOrigin = defaultSet.get(0);
        for (CircuitPoint point : defaultSet) {
            Vector relationToOrigin = new Vector(relativeOrigin, point);
            relationshipToOriginSet.add(relationToOrigin);
        }
        for (Vector v : relationshipToOriginSet) {
            double xv = v.x;
            double yv = v.y;
            switch (rotation) {
                case 1: case 90:
                    rotation = 90;
                    v.x = -yv;
                    v.y = xv;
                    break;
                case 2: case 180:
                    rotation = 180;
                    v.x = -xv;
                    v.y = -yv;
                    break;
                case 3: case 270:
                    rotation = 270;
                    v.x = yv;
                    v.y = -xv;
                    break;
            }
        }
        for (int i = 0; i < pointSetForDrawing.size(); i++) {
            pointSetForDrawing.set(i, location.getIfModifiedBy(relationshipToOriginSet.get(i)));
        }
        onRotate();
    }

    public boolean canRotate() {
        return getRelativePointSet() != null;
    }

    public void onRotate() { }

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


    public CircuitPoint getLocation() {
        return location.clone();
    }

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

    @Override
    public boolean equals(Object other) {
        if (!other.getClass().equals(getClass()))
            return false;
        return ((Entity) other).location.equals(location);
    }

    public boolean interceptsAll(CircuitPoint... pts) {
        for (CircuitPoint p : pts)
            if (!intercepts(p))
                return false;
        return true;
    }

    public abstract boolean doesGenWireInvalidlyInterceptThis(Wire w, CircuitPoint... exceptions);
    public boolean doesGenWireInvalidlyInterceptThis(Wire w) {
        return doesGenWireInvalidlyInterceptThis(w, new CircuitPoint[0]);
    }

}
