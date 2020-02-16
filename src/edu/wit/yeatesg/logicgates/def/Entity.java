package edu.wit.yeatesg.logicgates.def;

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
   //     System.out.println("New Entity: " + this.getClass().getSimpleName());
        if (canRotate()) {
            pointSetForDrawing = getRelativePointSet();
            setRotation(0);
        }
    }

    public abstract int getStrokeSize();

    public Stroke getStroke() {
        return new BasicStroke(getStrokeSize());
    }

    public Circuit getCircuit() {
        return c;
    }

    protected class PointSet extends ArrayList<CircuitPoint> {
        public PointSet(CircuitPoint... points) {
            this.addAll(Arrays.asList(points));
        }

        public PointSet(int initialCapacity) {
            super(initialCapacity);
        }

        public PointSet(double... points) {
            if (points.length % 2 != 0)
                throw new RuntimeException("Invalid Bezier Curve Points!");
            CircuitPoint[] circuitPoints = new CircuitPoint[points.length / 2];
            for (int i = 0, j = 1; j < points.length; i += 2, j += 2)
                circuitPoints[i / 2] = new CircuitPoint(points[i], points[j], c);
            this.addAll(Arrays.asList(circuitPoints));
        }

        public PointSet() {
            super();
        }

        public boolean intercepts(CircuitPoint p) {
            for (CircuitPoint other : this)
                if (p.equals(other))
                    return true;
            return false;
        }

        public boolean intercepts(PointSet other) {
            for (CircuitPoint o : other)
                if (intercepts(o))
                    return true;
            return false;
        }
    }

    public PointSet getRelativePointSet() {
        return null;
    }

    /** For drawing */
    protected PointSet pointSetForDrawing;

    public PointSet getPointSetForDrawing() {
        return pointSetForDrawing;
    }

    public CircuitPoint getLocation() {
        return location.clone();
    }

    public abstract boolean canMoveBy(Vector vector);

    public abstract BoundingBox getBoundingBox();

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

    public abstract void draw(Graphics2D g);

    public boolean canRotate() {
        return getRelativePointSet() != null;
    }

    public void onRotate() { }

    protected boolean deleted = false;

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

    public abstract boolean intercepts(CircuitPoint p);

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
