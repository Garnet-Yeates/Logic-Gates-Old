package edu.wit.yeatesg.logicgates.entity;

import com.sun.javafx.scene.paint.GradientUtils;
import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class Entity implements Dynamic {

    public Entity(Circuit c, boolean addToCircuit) {
        this.c = c;
    }

    public void postInit(boolean addToCircuit) {
        if (addToCircuit)
            c.addEntity(this);
    }


    // General attributes

    protected Circuit c;

    protected boolean preview;

    public boolean izPreview() {
        return preview;
    }

    public static Entity parseEntity(Circuit c, boolean isPreview, String s) {
        int closeBrackIndex = s.indexOf(']');
        String enityType = s.substring(1, closeBrackIndex);
        s = s.substring(closeBrackIndex + 1, s.length());
        if (enityType.equalsIgnoreCase("Wire")) {
            String[] values = s.split(",");
            if (values.length != 4)
                throw new RuntimeException("Invalid Wire String");
            double x1 = Double.parseDouble(values[0]);
            double y1 = Double.parseDouble(values[1]);
            double x2 = Double.parseDouble(values[2]);
            double y2 = Double.parseDouble(values[3]);
            return new Wire(new CircuitPoint(x1, y1, c), new CircuitPoint(x2, y2, c));
        }
        return null;
    }

    protected boolean deleted = false;

    public abstract String getDisplayName();

    public Circuit getCircuit() {
        return c;
    }

    public void delete() {
        deleted = true;
        c.removeEntity(this);
    }

    public abstract void onDelete();

    public boolean isDeleted() {
        return deleted;
    }


    public abstract boolean equals(Object other);



    // General Intercepting

    public abstract BoundingBox getBoundingBox();

    /**
     * Returns a reference to this Entity's intercept points
     * @return a PointSet containing this entities intercept CircuitPoints
     */
    public abstract PointSet getInterceptPoints();

    public PointSet getInterceptPoints(Entity other) {
        return getInterceptPoints().intersection(other.getInterceptPoints());
    }

    public boolean intercepts(Entity other) {
        return getInterceptPoints(other).size() > 0;
    }

    public final boolean intercepts(CircuitPoint p) {
        return getInterceptPoints().contains(p);
    }

    public boolean intercepts(BoundingBox b) {
        return b.intercepts(this);
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

    public boolean interceptsAll(CircuitPoint... pts) {
        for (CircuitPoint p : pts)
            if (!intercepts(p))
                return false;
        return true;
    }


    // Invalid Intercepting

    public abstract PointSet getInvalidInterceptPoints(Entity e);

    public boolean invalidlyIntercepts(Entity e) {
        return getInvalidInterceptPoints(e) != null && getInvalidInterceptPoints(e).size() > 0;
    }

    protected PointSet invalidInterceptPoints = new PointSet();

    /**
     * Note to self: Must be called before connectCheck is called when constructing ConnectibleEntities
     */
    public void updateInvalidInterceptPoints() {
        updateInvalidInterceptPoints(false);
    }

    public void updateInvalidInterceptPoints(boolean subCall) {
        invalidInterceptPoints.clear();
        for (Entity other : c.getEntitiesWithinScope(getBoundingBox().getExpandedBy(5))) {
            PointSet invalidInterceptPoints = getInvalidInterceptPoints(other);
            if (invalidInterceptPoints.size() > 0)
                invalidInterceptPoints.addAll(getInvalidInterceptPoints(other));
            if (!subCall)
                other.updateInvalidInterceptPoints(true);
        }
        if (invalidInterceptPoints.size() > 0 && this instanceof ConnectibleEntity) {
            ((ConnectibleEntity) this).disconnectAll();
        }
    }

    public EntityList<Entity> getInvalidlyInterceptingEntities() {
        EntityList<Entity> list = new EntityList<>();
        for (CircuitPoint p : invalidInterceptPoints)
            for (Entity e : getCircuit().getAllEntities())
                if (e.intercepts(p) && !list.contains(e))
                    list.add(e);
        return list.clone();
    }

    public boolean isInvalid() {
        return invalidInterceptPoints.size() > 0;
    }

    public PointSet getInvalidInterceptPoints() {
        return invalidInterceptPoints;
    }



    // Specialized Intercepting, for auto-generating Wires

    public abstract boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo,
                                                              PermitList exceptions,
                                                              boolean strictWithWires);




    public void setPreview(boolean b) {
        preview = b;
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


    // For Moving/Repositioning Entities

    public abstract boolean canMove();

    public CircuitPoint movingTo = null;

    public boolean beingMoved() {
        return movingTo != null;
    }

    /**
     * Movable subclasses should override this
     * @param newLocation the CircuitPoint that this Entity's new origin will be
     */
    public Entity onDragMove(CircuitPoint newLocation) {
        movingTo = newLocation;
        return null;
    }

    /**
     * Movable subclasses should override this
     * @param newLocation the CircuitPoint that this Entity's new origin will be
     */
    public void onDragMoveRelease(CircuitPoint newLocation) {
        movingTo = null;
    }


    /** For drawing */

    protected PointSet drawPoints;

    public abstract void draw(GraphicsContext g);

    public abstract int getLineWidth();

    public abstract Entity clone(Circuit onto);

    public void duplicate() {
        clone(c);
    }
}