package edu.wit.yeatesg.logicgates.entity;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.SimpleGateAND;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.InputBlock;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.LinkedList;

public abstract class Entity implements PropertyMutable {

    private static int idAssign;
    protected int id;

    private final Circuit c;

    protected enum Size { SMALL, MEDIUM, LARGE }

    /**
     * Entity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * call construct()
     * @param c
     */
    public Entity(Circuit c) {
        id = idAssign++;
        this.c = c;
    }

    public int getEntityID() {
        return id;
    }

    /**
     * I am going to make it so Entities are more mutable. Previously I was thinking about making it so that
     * entities PointSet's are constant (besides wires) and cannot be changed. This means that if an entity was to
     * be rotated, I would have to construct a whole new entity and each respective undo/redo operation would use
     * EntityDeleteOperation and EntityAddOperation to change the entities. That is fucking stupid. I am going to add
     * PropertyChangeOperation which is going to take an Entity and 2 Strings and it will look for an entity that is
     * similar to the specified one and will call onPropertyChange(String property, String newValue) with the supplied
     * strings. Each entity already has an abstract method that they must override called onPropertyChange already.
     * What the onPropertyChange method should do is associate strings with fields in the entities, use the inputted
     * string to mutate that field, then call construct() to reconstruct the object based on the fields
     *
     * The construct method should construct the entities vital fields such as interceptPoints, drawPoints,
     * non-volatile node locations. These changes should be directly based on the values of certain fields
     * (such as origin, rotation, size) fields. This setup will make it so that at any point in the program I can
     * change the value of an Entities field, then reconstruct it so that it adjusts to the changes. Even simpler, I
     * will simply be able to call onPropertyChange(String property, String newValue) to mutate entities whenever I want
     */
    public abstract void construct();

    /**
     * This should be called if the entity already exists in the circuit and has registered InterceptPoints in the
     * Circuit's InterceptMap
     *
     * PointSet should NOT be fiddled with by the time this is called. This method should be doing the PointSet fiddling
     */
    public void reconstruct() {
        if (!existsInCircuit()) {
            construct(); // Soft error
            return;
        }
        remove();
        construct();
        add();
    }

    /**
     * This method is called when an Entity is added to the circuit. It is also called in the
     * {@link #reconstruct()} method because reconstruct() removes, constructs, then adds this
     * Entity.
     */
    public void onAddToCircuit() {
        inCircuit = true;
        addInterceptEntries();
        spreadUpdate();
    }

    public final void add() {
        c.addEntity(this);
    }

    public final void update() {
        updateInvalidInterceptPoints();
        if (this instanceof ConnectibleEntity)
            ((ConnectibleEntity) this).connectCheck();
    }

    public void updateNearby() {
        if (!existsInCircuit())
            throw new RuntimeException("You probably fucked up 2" + this);
        for (Entity e : getInterceptingEntities())
            e.update();
    }

    public final void spreadUpdate() {
        System.out.println("SpreadUpdate called on " + this + " exists? " + existsInCircuit());
        if (!existsInCircuit())
            throw new RuntimeException("You probably fucked up " + this);
        update();
        updateNearby();
    }


    public void addWithStateOperation() {
        c.addEntityAndTrackOperation(this);
    }

    /**
     * Should only really be called in {@link #onAddToCircuit()}
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


    // General attributes


    public void removeEntity() {
        c.removeExact(this);
    }

    public void removeWithTrackedStateOperation() {
        c.removeSimilarEntityAndTrackOperation(this);
    }

    private boolean inCircuit = false;

    public boolean existsInCircuit() {
        return inCircuit;
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
        String[] fields = s.split(",");
        if (enityType.equalsIgnoreCase("Wire")) {
            if (fields.length != 4)
                throw new RuntimeException("Invalid Wire String");
            double x1 = Double.parseDouble(fields[0]);
            double y1 = Double.parseDouble(fields[1]);
            double x2 = Double.parseDouble(fields[2]);
            double y2 = Double.parseDouble(fields[3]);
            return new Wire(new CircuitPoint(x1, y1, c), new CircuitPoint(x2, y2, c));
        } else if (enityType.equalsIgnoreCase("SimpleGateAND"))
            return new SimpleGateAND(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]));
        else if (enityType.equalsIgnoreCase("InputBlock"))
            return new InputBlock(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]));
        return null;
    }

    public abstract String getDisplayName();

    public Circuit getCircuit() {
        return c;
    }

    /**
     * Different entities will have different implementations for when they move by a vector. This is because some
     * entities act differently upon being moved, such as Wires (because they only have intercept points, no draw
     * points and no origin), so different fields need to be updated.
     * @param v the Vector that this Entity is being moved by
     */
    public abstract void move(Vector v);

    /**
     * Removes this Entity from its Circuit
     */
    public final void remove() {
        c.removeExact(this); // onRemove is called if it is removed
    }

   public void onRemove() {
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

    public boolean isInvalid() {
        return invalidInterceptPoints.size() > 0;
    }

    public boolean select() {
        return getCircuit().select(this);
    }

    public void onSelect() {
        // TODO nothing yet
    }

    public boolean deselect() {
        return getCircuit().deselect(this);
    }

    public void onDeselect() {
        update();
    }


    public boolean isSelected()  {
        return getCircuit().currentSelectionReference().containsExact(this);
    }

    public final void updateInvalidInterceptPoints() {
        invalidInterceptPoints.clear();
        boolean wasInvalid = isInvalid();
        for (Entity other : getInterceptingEntities())
            invalidInterceptPoints.addAll(getInvalidInterceptPoints(other));
        if (isInvalid()) {
            if (existsInCircuit())
                getCircuit().markInvalid(this);
            if (this instanceof ConnectibleEntity)
                ((ConnectibleEntity) this).disconnectAll();
        } else if (wasInvalid) {
            getCircuit().markValid(this);
        }
    }

    public EntityList<Entity> getInvalidlyInterceptingEntities() {
        EntityList<Entity> list = new EntityList<>();
        for (CircuitPoint p : invalidInterceptPoints)
            for (Entity e : getCircuit().getInterceptMap().get(p))
                if (!list.contains(e) && !e.isSimilar(this) && e.invalidlyIntercepts(this))
                    list.add(e);
        return list.clone();
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

    public void draw(GraphicsContext g) {
        draw(g, null);
    }

    public abstract void draw(GraphicsContext g, Color col);

    public abstract double getLineWidth();

    public void duplicate() {
        clone(c);
    }
}