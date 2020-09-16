package edu.wit.yeatesg.logicgates.circuit.entity;

import com.sun.tools.javac.Main;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.OutputBlock;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.InputBlock;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import edu.wit.yeatesg.logicgates.gui.MainGUI;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.LinkedList;

public abstract class Entity implements PropertyMutable {

    private static int eIDAssign;
    protected int eID;

    protected final Circuit c;

    /**
     * Entity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * call construct()
     * @param c
     */
    public Entity(Circuit c) {
        eID = eIDAssign++;
        this.c = c;
    }

    public int getEntityID() {
        return eID;
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * EQUALITY/SIMILARITY CHECKING AS WELL AS CLONING
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public final boolean equalsExact(Entity other) {
        return other == this;
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

    public abstract Entity getCloned(Circuit onto);

    /**
     * Returns an entity where, if {@link #isSimilar(Entity)} is called on it, it will return true. When overriding this
     * method, make sure that you do not add the entity to the circuit (when constructing it, the addToCircuit parameter of
     * the super Entity constructor should be false)
     * @return an entity that is regarded as similar to this one.
     */
    public Entity getSimilarEntity() {
        return getCloned(c);
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * CONSTRUCTING AND RECONSTRUCTING (THE BASIS ON HOW MY ENTITIES WORK)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /**
     * The construct method should construct the entities vital fields such as interceptPoints, drawPoints,
     * non-volatile node locations. These changes should be directly based on the values of certain fields
     * (such as origin, rotation, size) fields. This setup will make it so that at any point in the program I can
     * change the value of an Entity's field, then reconstruct it so that it adjusts to the changes.
     */
    public abstract void construct();

    /**
     * If a given Entity exists in the Circuit, you can change whatever field value you want in the reference to the
     * Entity (such as origin, rotation for certain entities etc), then call this reconstruct method on the Entity
     * and it will remove it, construct its vital fields based on the new attributes you gave it, then add it back
     * to the Circuit. If the Entity does not exist in the circuit, use construct().
     */
    public final void reconstruct() {
        if (!existsInCircuit()) {
            construct(); // Soft error
            return;
        }
        boolean wasSelected = isSelected();
        remove();
        construct();
        if (wasSelected)
            select();
        add();
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * EXISTENCE IN THE CIRCUIT PART 1: CIRCUIT INDEX, ADDING, REMOVING, ETC
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private int circuitIndex = -1;

    public boolean existsInCircuit() {
        return circuitIndex > -1;
    }

    public int getCircuitIndex() {
        return circuitIndex;
    }

    public void setCircuitIndex(int index) {
        circuitIndex = index;
    }

    /**
     * This method is called when an Entity is added to the circuit. It is also called in the
     * {@link #reconstruct()} method because reconstruct() removes, constructs, then adds this
     * Entity.
     */
    public void onAddToCircuit() {
        addInterceptEntries();
        addChunkEntries();
        update();
    }

    /**
     * Calls {@link #c}.addEntity(this) which adds this Entity to the Circuit's CircuitEntityList, assigning this
     * Entity its {@link #circuitIndex} as well as calling {@link #onAddToCircuit()}. Many other things happen such
     * as InterceptMap entries being added, this Entity being updated, and also updating nearby entities. See
     * {@link Circuit#addEntity(Entity)} and more specifically see
     * {@link edu.wit.yeatesg.logicgates.circuit.Circuit.CircuitEntityList#addEntity(Entity)} to see everything that
     * happens
     */
    public final void add() {
        c.addEntity(this);
    }

    /**
     * Calls {@link #c}.removeExact(this) which removes this Entity from the Circuit's CircuitEntityList, removes
     * its InterceptMap entries, updates Entities that it used to intercept and calls {@link #onRemove()} so that
     * the circuitIndex of this Entity is set back to -1. Other things also happen, to see everything that happens
     * refer to {@link Circuit#removeExact(Entity)} and more specifically, look at
     * {@link edu.wit.yeatesg.logicgates.circuit.Circuit.CircuitEntityList#onRemove(Entity)} (Entity)}.
     * Entity its {@link #circuitIndex} as well as calling {@link #onAddToCircuit()}
     */
    public final void remove() {
        c.removeExact(this); // onRemove is called if it is removed
    }

    public void onRemove() {
        circuitIndex = -1;
    }

    public void addInterceptEntries() {
        getCircuit().getInterceptMap().addInterceptPointsFor(this);
    }

    private LinkedList<Circuit.Chunk.ChunkNode> myChunkNodes = new LinkedList<>();

    private EditorPanel.DrawMark drawMark;

    public boolean isDrawMarked() {
        return drawMark != null && drawMark.isActive();
    }

    public void setDrawMark(EditorPanel.DrawMark mark) {
        drawMark = mark;
    }


    public void addChunkEntries() {
        o: for (CircuitPoint p : interceptPoints) {
            Circuit.Chunk chunk = c.getChunkAt(p);
            for (Circuit.Chunk.ChunkNode node : myChunkNodes)
                if (node.getChunk() == chunk)
                    continue o;
            myChunkNodes.add(chunk.add(this));
        }
    }

    public void removeChunkEntries() {
        for (Circuit.Chunk.ChunkNode node : myChunkNodes)
            node.remove();
        myChunkNodes.clear();
    }

    public void removeInterceptEntries() {
        getCircuit().getInterceptMap().removeInterceptPointsFor(this);
    }


    public void removeWithTrackedStateOperation() {
        c.removeSimilarEntityAndTrackOperation(this);
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * EXISTENCE IN THE CIRCUIT PART 2: UPDATING (ENTITIES MAY UPDATE ENTITIES THAT THEY INTERCEPT)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    protected boolean blockUpdate = false;

    public void disableUpdate() {
        blockUpdate = true;
    }

    public void enableUpdate() {
        blockUpdate = false;
    }

    public void spreadUpdate() {
        if (!c.isUpdateDisabled() && !blockUpdate && existsInCircuit()) {
            update();
            for (Entity e : getInterceptingEntities())
                e.update();
        }
    }

    public final void update() {
        if (!c.isUpdateDisabled() && !blockUpdate && existsInCircuit()) {
            updateInvalidInterceptPoints();
            if (this instanceof ConnectibleEntity) {
                ((ConnectibleEntity) this).connectCheck();
                c.powerUpdate((ConnectibleEntity) this);
            }
        }
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * EXISTENCE IN THE CIRCUIT PART 3: INTERCEPTING (GOES HAND IN HAND WITH UPDATING)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    /** When this entity is added to the Circuit, a reference to this Entity is added to the InterceptionList that
     *  the {@link edu.wit.yeatesg.logicgates.circuit.Circuit.InterceptMap}
     *  at each [x][y] that shows up in this interceptPoints list */
    protected CircuitPointList interceptPoints;

    /** Generally adheres to the intercept points of this Entity, but this isn't always true. It is more used to show
     *  the bounds of the entity when it is selected
     */
    public abstract BoundingBox getBoundingBox();

    public EntityList<Entity> getInterceptingEntities() {
        return getCircuit().getInterceptMap().getEntitiesThatIntercept(this);
    }

    /**
     * Obtains a deep clone of this Entity's intercept points
     * @return deep clone of this Entity's intercept points
     */
    public CircuitPointList getInterceptPoints() {
        return interceptPoints.deepClone();
    }

    public final CircuitPointList getInterceptPoints(Entity other) {
        return getInterceptPoints().intersection(other.getInterceptPoints());
    }

    public boolean intercepts(Entity e, CircuitPoint at) {
        return this.getInterceptPoints().contains(at) && e.getInterceptPoints().contains(at);
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


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * EXISTENCE IN THE CIRCUIT PART 4: INVALID INTERCEPTING (INVALIDLY INTERCEPTING ENTITIES CANNOT CONNECT / FUNCTION
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    protected Map<CircuitPoint, Direction> blockWiresHere;

    public Map<CircuitPoint, Direction> getWireBlockLocations() {
        return blockWiresHere;
    }

    private int invalidEntityIndex = -1;

    public boolean isInvalid() {
        return invalidEntityIndex != -1;
    }

    public int getInvalidEntityIndex() {
        return invalidEntityIndex;
    }

    public void setInvalidEntityIndex(int invalidEntityIndex) {
        this.invalidEntityIndex = invalidEntityIndex;
    }

    public abstract CircuitPointList getInvalidInterceptPoints(Entity e);

    public boolean invalidlyIntercepts(Entity e) {
        CircuitPointList invalids = getInvalidInterceptPoints(e);
        return invalids.size() > 0;
    }

    protected CircuitPointList invalidInterceptPoints = new CircuitPointList();

    /**
     * Won't be called upon update() if it isn't in the Circuit. But can still be updated if it isn't in the Circuit
     * @param excluding the Entities that we are ignoring
     */
    public final void updateInvalidInterceptPoints(ExactEntityList<Entity> excluding) {
        invalidInterceptPoints.clear();
        boolean wasInvalid = isInvalid();
        for (Entity other : getInterceptingEntities())
            if (!excluding.containsExact(other))
                invalidInterceptPoints.addAll(getInvalidInterceptPoints(other));
        if (invalidInterceptPoints.size() > 0 && existsInCircuit()) {
            getCircuit().markInvalid(this);
            if (this instanceof ConnectibleEntity)
                ((ConnectibleEntity) this).disconnectAll();
        } else if (wasInvalid && existsInCircuit()) {
            getCircuit().markValid(this);
        }
    }

    public final void updateInvalidInterceptPoints() {
        updateInvalidInterceptPoints(new ExactEntityList<>());
    }

    public EntityList<Entity> getInvalidlyInterceptingEntities() {
        EntityList<Entity> list = new EntityList<>();
        for (CircuitPoint p : invalidInterceptPoints)
            for (Entity e : getCircuit().getInterceptMap().get(p))
                if (!list.contains(e) && !e.isSimilar(this) && e.invalidlyIntercepts(this))
                    list.add(e);
        return list.clone();
    }

    public CircuitPointList getInvalidInterceptPoints() {
        return invalidInterceptPoints;
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * EXISTENCE IN THE CIRCUIT PART 5: SELECTING AND DESELECTING
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private int selectionIndex = -1;

    public void setSelectionIndex(int index) {
        selectionIndex = index;
    }

    public int getSelectionIndex() {
        return selectionIndex;
    }

    public boolean isSelected()  {
        return selectionIndex > -1;
    }

    public void select() {
        getCircuit().select(this);
    }

    public void onSelect() {
        selectionIndex = getCircuit().currentSelectionReference().size() - 1;
    }

    public void deselect() {
        getCircuit().deselect(this);
    }

    public void onDeselect() {
        selectionIndex = -1;
        update();
    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * TEMPLATE ENTITY STUFF (PRINCIPAL ENTITIES THAT ARE CLONED WHEN THE USER ADDS THEM VIA THE LEFT SIDE TREE VIEW)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    protected MainGUI.EntityTreeItem treeItem;

    public boolean isItemEntity() {
        return treeItem != null;
    }

    public void setTreeItem(MainGUI.EntityTreeItem item) {
        treeItem = item;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * OTHER, IM TOO LAZY TO FINISH ORGANIZING. WHAT DID THE DOCTOR SAY TO THE OTHER DOCTOR? THIS ORGAN BOOK IS VERY ORGAN-IZED
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


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
        }
        else if (enityType.equalsIgnoreCase("GateAND"))
            return GateAND.parse(s, c);
        else if (enityType.equalsIgnoreCase("GateOR"))
            return GateOR.parse(s, c);
        else if (enityType.equalsIgnoreCase("GateXOR"))
            return GateXOR.parse(s, c);
        else if (enityType.equalsIgnoreCase("InputBlock"))
            return InputBlock.parse(s, c);
        else if (enityType.equalsIgnoreCase("OutputBlock"))
            return new OutputBlock(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]));
        else if (enityType.equalsIgnoreCase("GateNOT"))
            return GateNOT.parse(s, c);
        else if (enityType.equalsIgnoreCase("Transistor"))
            return Transistor.parse(s, c);
        else if (enityType.equalsIgnoreCase("PullResistor"))
            return PullResistor.parse(s, c);
        else if (enityType.equalsIgnoreCase("PowerEmitter"))
            return PowerEmitter.parse(s, c);
        else if (enityType.equalsIgnoreCase("GroundEmitter"))
            return GroundEmitter.parse(s, c);
        return null;
    }

    public abstract String getDisplayName();

    public Circuit getCircuit() {
        return c;
    }

    /**
     * Different entities will have different implementations for when they move by a vector. This is because some
     * entities act differently upon being moved, such as Wires (because they only have intercept points, no draw
     * points and no oriegin), so different fields need to be updated.
     * @param v the Vector that this Entity is being moved by
     */
    public abstract void move(Vector v); // SHOULD ONLY BE CALLED BY moveBy (meaning ONLY move operations)



    // Specialized Intercepting, for auto-generating Wires

    public abstract boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo,
                                                              PermitList exceptions,
                                                              boolean strictWithWires);

    public abstract String toParsableString();

    public Size getSize() {
        return size;
    }

    protected Size size;

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

    public CircuitPoint movingTo = null;

    public boolean beingMoved() {
        return movingTo != null;
    }



    /** For drawing */

    protected CircuitPointList drawPoints;

    public void draw(GraphicsContext g) {
        draw(g, null, 1);
    }

    public abstract void draw(GraphicsContext g, Color col, double opacity);

    public static final Color PREVIEW_COLOR = Color.rgb(90, 90, 90);

    public void drawPreview(GraphicsContext g) {
        draw(g, PREVIEW_COLOR, 1);
    }

    public double getLineWidth() {
        return c.getLineWidth();
    }

    public void duplicate() {
        getCloned(c);
    }

    public static final Size SMALL = Size.SMALL;
    public static final Size NORMAL = Size.NORMAL;

    public enum Size {
        SMALL, NORMAL;

        public static Size fromString(String s) {
            switch (s.toUpperCase()) {
                case "SMALL":
                    return SMALL;
                case "NORMAL":
                    return NORMAL;
                default:
                    return null;
            }
        }

    }
}