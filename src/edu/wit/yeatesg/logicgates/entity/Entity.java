package edu.wit.yeatesg.logicgates.entity;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;

import java.util.Arrays;
import java.util.LinkedList;

public abstract class Entity implements PropertyMutable {

    private static int idAssign;
    protected int id;

    public Entity(Circuit c) {
        id = idAssign++;
        this.c = c;
    }

    // General attributes

    private Circuit c;

    public void add() {
        c.addEntity(this);
    }

    public void addWithStateOperation() {
        c.addEntityAndTrackOperation(this);
    }

    public void removeEntity() {
        c.removeEntity(this);
    }

    public void removeWithTrackedStateOperation() {
        c.removeSimilarEntityAndTrackOperation(this);
    }

    private boolean inCircuit = false;

    public boolean existsInCircuit() {
        return inCircuit;
    }

    public void onAddToCircuit() {
        inCircuit = true;
        updateInvalidInterceptPoints();
        addInterceptEntries();
    }

    /**
     * Should only really be called in {@link #onAddToCircuit()} and when the PointSet is updated (i.e the entity is moved)
     */
    public void addInterceptEntries() {
        getCircuit().getInterceptMap().addInterceptPointsFor(this);
    }

    /**
     * Should only really be called when {@link #remove()} is called and when the PointSet is updated (i.e the entity is moved)
     */
    public void removeInterceptEntries() {
        getCircuit().getInterceptMap().removeInterceptPointsFor(this);
    }

    /**
     * This method is not overridable because we want our equality checks to be the same as the similarity check
     * @param other the entity that {@link #isSimilar(Entity)} is going to be called on.
     * @return true if this entity is similar to 'other'
     */
    public final boolean equals(Object other) {
        return other instanceof Entity && isSimilar((Entity) other);
    }

    /**
     * This method should determine whether these two entities are equivalent in all aspects besides their memory
     * address (same circuit, same locations etc).This method should not care if one of the entities exists on the
     * circuit and the other doesn't. If they are connectible entities it should not care about their
     * current connections or anything like that. The only things that should be compared is the type and the determining
     * origin/edge points of the entities. For logic gates it will compare the size, num inputs, nots, etc.
     * @param other the entity that is being checked for similarity
     * @return true if these two entities are deemed similar
     */
    public abstract boolean isSimilar(Entity other);

    public abstract Entity clone(Circuit onto);

    /**
     * Returns an entity where, if {@link #isSimilar(Entity)} is called on it, it will return true. When overriding this
     * method, make sure that you do not add the entity to the circuit (when constructing it, the addToCircuit parameter of
     * the super Entity constructor should be false)
     * @return an entity that is regarded as similar to this one.
     */
    public Entity getSimilarEntity() {
        return clone(c);
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
            Wire added = new Wire(new CircuitPoint(x1, y1, c), new CircuitPoint(x2, y2, c));
            added.add(); // Same as c.addEntity(added)
            return added;
        }
        return null;
    }

    public abstract String getDisplayName();

    public Circuit getCircuit() {
        return c;
    }

    /**
     * Different entities will have different implementations for when they move by a vector. This is because some
     * entities act differently upon being moved, such as Wires (because they only have intercept points, no draw
     * points and no origin), so different PointSet/Fields need to be updated. Make sure to call
     * {@link Circuit#onEntityMove()} after this is called
     * @param v the Vector that this Entity is being moved by
     */
    public abstract void move(Vector v);

    /**
     * Removes this Entity from its Circuit
     */
    public final void remove() {
        c.removeEntity(this); // If it was successfully removed, onRemovedFromCircuit() is called
    }

    /**
     * Called when this Entity is successfully removed from the Entity list of its Circuit. Call super
     * when overriding this
     * '.
     */
    public void onRemovedFromCircuit() {
        c.getInterceptMap().removeInterceptPointsFor(this);
        inCircuit = false;
    }

    // General Intercepting

    public abstract BoundingBox getBoundingBox();

    protected PointSet interceptPoints;

    public PointSet getInterceptPoints() {
        return interceptPoints.deepClone();
    }

    public boolean intercepts(Entity e, CircuitPoint at) {
        return this.getInterceptPoints().contains(at) && e.getInterceptPoints().contains(at);
    }

    public PointSet getInterceptPoints(Entity other) {
        return getInterceptPoints().intersection(other.getInterceptPoints());
    }

    public EntityList<Entity> getInterceptingEntities() {
        return getCircuit().getInterceptMap().getEntitiesThatIntercept(this);
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
        PointSet invalids = getInvalidInterceptPoints(e);
        return invalids != null && invalids.size() > 0;
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
        for (Entity other : getInterceptingEntities()) {
            PointSet invalidInterceptPoints = getInvalidInterceptPoints(other);
            if (invalidInterceptPoints.size() > 0) {
                invalidInterceptPoints.addAll(getInvalidInterceptPoints(other));
                getCircuit().markInvalid(this);
            }
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
            for (Entity e : getCircuit().getInterceptMap().get(p))
                if (!list.contains(e) && !e.isSimilar(this))
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



    public abstract String toParsableString();

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

    public abstract double getLineWidth();


    public void duplicate() {
        clone(c);
    }
}