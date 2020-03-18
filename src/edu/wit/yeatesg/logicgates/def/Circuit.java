package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.entity.connectible.*;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import edu.wit.yeatesg.logicgates.gui.Project;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

import java.util.*;


public class Circuit implements Dynamic {

    public Circuit(Project p, String circuitName) {
        this.circuitName = circuitName;
        this.project = p;
        this.new InterceptMap();
        p.addCircuit(this);
    }

    public Circuit() { }

    private String circuitName;

    public String getCircuitName() {
        return circuitName;
    }

    private Project project;

    public EditorPanel getEditorPanel() {
        return project.getEditorPanel();
    }




    private InterceptMap interceptMap;

    public static class InterceptionList extends EntityList<Entity> {

        @Override
        public InterceptionList clone() {
            InterceptionList clone = new InterceptionList();
            clone.addAll(this);
            return clone;
        }
    }

    public InterceptMap getInterceptMap() {
        return interceptMap;
    }

    /**
     * Intercept map: Used to make the program efficient. Using a limited (however sill big) map size, we can track
     * interceptions much easier. To get the intercepting entities of an Entity e, instead of having to loop through
     * {@link Circuit#allEntities} and compare intercept points, we can instead loop through the intercept points of
     * e, and for each CircuitPoint p, check the [x][y] coordinate of this Array that corresponds to p. This makes
     * the program MUCH faster because the max iterations is determined by how many intercept points e has, instead
     * of having to depend on the number of entities in the circuit because of having to loop through a list.
     */
    public class InterceptMap {

        public static final int MAP_SIZE = 999; // Must be an odd number for origin to work properly. Don't break this rule.
        public static final int MAP_OFFSET = ( (MAP_SIZE - 1) / 2 ); // This needs to be added to map.get(x, y) operations
        public static final int CP_MIN = -1*MAP_OFFSET;
        public static final int CP_MAX = MAP_SIZE - 1 - MAP_OFFSET;

        private InterceptionList[][] map;

        private BoundingBox boundingBox;
        
        public InterceptMap() {
            map = new InterceptionList[MAP_SIZE][MAP_SIZE];
            for (int x = 0; x < map.length; x++)
                for (int y = 0; y < map.length; y++)
                    map[x][y] = new InterceptionList();
            Circuit.this.interceptMap = this;
            boundingBox = new BoundingBox(new CircuitPoint(CP_MIN, CP_MIN, Circuit.this), new CircuitPoint(CP_MAX, CP_MAX, Circuit.this), null);
        }

        public BoundingBox getBoundingBox() {
            return boundingBox.clone();
        }

        private InterceptionList getRef(int x, int y) {
            return map[x + MAP_OFFSET][y + MAP_OFFSET];
        }

        public InterceptionList get(int x, int y) {
            return getRef(x, y).clone();
        }

        public InterceptionList get(CircuitPoint c) {
            if (!c.isSimilar(c.getGridSnapped()))
                throw new RuntimeException("Cannot query intercepting entities for " + c + " because it is not grid-snapped");
            return get((int) c.x, (int) c.y);
        }

        public InterceptionList getEntitiesThatIntercept(int x, int y) {
            return get(x, y);
        }

        public InterceptionList getEntitiesThatIntercept(CircuitPoint c) {
            return get(c);
        }

        public InterceptionList getEntitiesThatIntercept(Entity e) {
            InterceptionList thatInterceptE = new InterceptionList();
            for (CircuitPoint intPoint : e.getInterceptPoints())
                for (Entity otherEntity : getEntitiesThatIntercept(intPoint))
                    if (!thatInterceptE.contains(otherEntity) && otherEntity != e)
                        thatInterceptE.add(otherEntity);  // We WANT != here instead of !equals(). This is because
            return thatInterceptE;                        // we want to be able to check interceptions for similar entities
        }                                                 // that may not be on the circuit

        public void addInterceptPoint(int x, int y, Entity e) {
            if (getRef(x, y).contains(e))
                throw new RuntimeException("Already Added");
            getRef(x, y).add(e);
        }

        public void addInterceptPoint(CircuitPoint p, Entity e) {
            addInterceptPoint((int) p.x, (int) p.y, e);
        }

        public void addInterceptPointsFor(Entity entity) {
            for (CircuitPoint p : entity.getInterceptPoints()) {
                addInterceptPoint(p, entity);
            }
        }

        public void removeInterceptPoint(int x, int y, Entity e) {
            if (getRef(x, y).remove(e))
                return;
            throw new RuntimeException("Could not remove interception entry for " + e + " at [" +  x + "]"
                    + "[" + y + "] because there is no entry for this entity here");
        }

        public void removeInterceptPoint(CircuitPoint p, Entity e) {
            removeInterceptPoint((int) p.x, (int) p.y, e);
        }

        public void removeInterceptPointsFor(Entity e) {
            for (CircuitPoint intPoint : e.getInterceptPoints())
                removeInterceptPoint(intPoint, e);
        }

        public void pushIntoRange(PointSet points) {
            for (CircuitPoint p : points) {
                while(p.x > InterceptMap.CP_MAX)
                    points.addVectorToAllPoints(new Vector(-1, 0));
                while(p.x < InterceptMap.CP_MIN)
                    points.addVectorToAllPoints(new Vector(1, 0));
                while(p.y > InterceptMap.CP_MAX)
                    points.addVectorToAllPoints(new Vector(0, -1));
                while(p.y < InterceptMap.CP_MIN)
                    points.addVectorToAllPoints(new Vector(0, 1));
            }
        }

        public boolean isInRange(CircuitPoint p) {
            return p.x >= CP_MIN && p.x <= CP_MAX
                    && p.y >= CP_MIN && p.y <= CP_MAX;
        }
    }

    public int getGridMin() {
        return InterceptMap.CP_MIN;
    }

    public int getGridMax() {
        return InterceptMap.CP_MAX;
    }


    public void pushIntoMapRange(PointSet points) {
        interceptMap.pushIntoRange(points);
    }

    public void pushIntoMapRange(CircuitPoint... points) {
        pushIntoMapRange(new PointSet(points));
    }

    public boolean isInMapRange(CircuitPoint p) {
        return interceptMap.isInRange(p);
    }




    public static final Color COL_BG = Color.WHITE;
    public static final Color COL_GRID = Color.DARKGREY;
    public static final Color COL_ORIGIN = Color.RED;

    /**
     * CircuitPoint to CircuitDrawPoint
     * Multiply the x and y of a CircuitPoint by this value to get the CircuitDrawPoint
     * Represents the distance between CircuitPoint 0,0 and 0,1 on the editor panel
     * (how many CircuitDrawPoints can fit between two CircuitPoints aka Grid Points?)
     * */
    private int scale = 30;

    private static final int SCALE_MAX = 80;
    private static final int SCALE_MIN = 10;
    private static final int SCALE_INC = 10;

    /**
     * Returns the number that you have to multiply a {@link CircuitPoint} by to get its {@link PanelDrawPoint}
     * @return CircuitPoint * this = PanelDrawPoint
     */
    public double getScale() {
        return scale;
    }

    public boolean canScaleUp() {
        return scale < SCALE_MAX;
    }

    public void scaleUp() {
        scale += canScaleUp() ? SCALE_INC : 0;
    }

    public boolean canScaleDown() {
        return scale > SCALE_MIN;
    }

    public void scaleDown() {
        scale -= canScaleDown() ? SCALE_INC : 0;
    }

    public int getLineWidth() {
        switch (scale) {
            case 10:
                return 2;
            case 20:
            case 30:
                return 3;
            case 40:
            case 50:
                return 5;
            case 60:
            case 70:
                return 7;
            case 80:
            case 90:
                return 9;
            default: return 0;
        }
    }

    public int getGridLineWidth() {
        int size = (int) (getLineWidth() / 1.5);
        if (size == 0) size++;
        return size;
    }

    /** def.Circuit Draw x to Panel draw x
     * you add this number to a CircuitDrawPoint x to get its PanelDrawPoint x */
    private int xoff = 0;

    /** def.Circuit Draw y to Panel draw y
     * you add this number to a CircuitDrawPoint y to get its PanelDrawPoint y */
    private int yoff = 0;

    /**
     * Returns the number that you have to add to a {@link CircuitPoint} x to get its {@link PanelDrawPoint} x
     * @return CircuitDrawPoint x +-> PanelDrawPoint x
     */
    public int getXOffset() {
        return xoff;
    }

    /**
     * Returns the number that you have to add to a {@link CircuitPoint} y to get its {@link PanelDrawPoint} y
     * @return CircuitDrawPoint y + this = PanelDrawPoint y
     */
    public int getYOffset() {
        return yoff;
    }

    /**
     * Returns the the vector that you have to add to a {@link CircuitPoint} to get its {@link PanelDrawPoint}
     * @return CircuitDrawPoint + this = PanelDrawPoint
     */
    public Vector getOffset() {
        return new Vector(xoff, yoff);
    }

    public void modifyOffset(Vector vector) {
        xoff += vector.x;
        yoff += vector.y;
    }

    public void setOffset(Vector vector) {
        xoff = (int) vector.x;
        yoff = (int) vector.y;
    }

    public void setXOffset(int x) {
        xoff = x;
    }

    public void setYOffset(int y) {
        yoff = y;
    }

    private EntityList<Entity> allEntities = new EntityList<>();

    /**
     * Obtains a shallow clone of all of the Entities that exist on this Circuit
     * @return
     */
    public EntityList<Entity> getAllEntities() {
        return allEntities.clone();
    }

    public EntityList<Entity> getEntitiesThatIntercept(CircuitPoint p) {
        return interceptMap.getEntitiesThatIntercept(p);
    }


    public void refreshTransmissions() {
   //     System.out.print("Refresh Transmissions Took " + LogicGates.doTimeTest(() -> {
            Dependent.resetDependencies(this);
            Dependent.resetPowerStatus(this, true);
            Dependent.calculateDependencies(this);
            Dependent.calculateSuperDependencies(this);

            Dependent.illogicalCheck(this);

            Dependent.resetDependencies(this);
            Dependent.resetPowerStatus(this, false); // Only difference from first 4 is 'false' here
            Dependent.calculateDependencies(this);
            Dependent.calculateSuperDependencies(this);

            Dependent.determinePowerStatuses(this);
   //     }));

    }



    /**
     * This should be the ONLY way entities are added to the circuit
     * @param entity
     */
    public void addEntity(Entity entity) {
        if (entity.getCircuit() != this)
            throw new RuntimeException("Entity does not have a reference to this Circuit in memory");
        if (entity.preAddToCircuit()) {
            allEntities.add(entity);
            entity.updateInvalidInterceptPoints();
            entity.onAddToCircuit();
        }
        System.out.println("num entities: " + allEntities.size());
    }

    /**
     * ONLY CALL ON ENITIES THAT HAVE NOT BEEN ADDED YET. IF YOU WANT TO TRACK STATE CHANGES WHEN CREATING ENTITIES,
     * USE 'FALSE' IN THE 'addToCircuit' PARAM THEN CALL THIS
     * @return
     */
    public EntityAddOperation addWithStateOperation(Entity entity, boolean appendStateImmediately) {
        EntityAddOperation op = new EntityAddOperation(entity.getSimilarEntity());
        addEntity(entity);
        if (appendStateImmediately)
            appendCurrentStateChanges();
        return op;
    }

    public void removeEntity(Entity e) {
        interceptMap.removeInterceptPointsFor(e);
        if (e instanceof ConnectibleEntity)
            ((ConnectibleEntity) e).disconnectAll();
        if (allEntities.remove(e)) {
            e.onRemovedFromCircuit();
        }
        System.out.println("num entities: " + allEntities.size());
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityList<T> getAllEntitiesOfType(Class<T> type) {
        return allEntities.ofType(type);
    }

    @Override
    public String getPropertyTableHeader() {
        return "Properties For: Circuit";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this);
        propList.add(new Property("Circuit Name", "", ""));
        return propList;
    }

    @Override
    public void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1) {
        System.out.println("OBS VAL " + observableValue + " CHANGED FROM " + s + " TO " + t1);
    }

    private static final String[] properties = new String[] { "Circuit Name" };

    @Override
    public boolean hasProperty(String propertyName) {
        return Arrays.asList(properties).contains(propertyName);
    }

    public void deepCloneEntitiesFrom(Circuit c) {
        for (Entity e : c.getAllEntities())
            e.clone(this);
    }

    public Circuit cloneOntoProject(String newName) {
        return new Circuit(project, newName);
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    private CircuitStateChain stateTracker = new CircuitStateChain();

    public CircuitStateChain stateController() {
        return stateTracker;
    }

    public void appendCurrentStateChanges() {
        stateTracker.appendCurrentStateChanges();
    }

    public void clearStateChangeBuffer() {
        stateTracker.clearBuffer();
    }

    public static class CircuitStateChain {

        private CircuitState first;
        private CircuitState curr;

        private ArrayList<StateChangeOperation> currChangeBuffer = new ArrayList<>();

        public CircuitStateChain() {
            first = new CircuitState();
            curr = first;
            listening = true;
        }

        public void appendState(List<StateChangeOperation> operations) {
            appendState(operations.toArray(new StateChangeOperation[0]));
        }

        public void appendCurrentStateChanges() {
            if (!currChangeBuffer.isEmpty()) {
                appendState(currChangeBuffer);
                currChangeBuffer.clear();
            }
        }

        public void appendState(StateChangeOperation... operations) {
            System.out.println("APPEND STATE");
            clip();
            if (operations == null)
                throw new RuntimeException();
            CircuitState curr = this.curr;
            while (curr.right != null)
                curr = curr.right;
            curr.right = new CircuitState();
            curr.right.left = curr;
            curr.toGoRight.addAll(Arrays.asList(operations));
            for (int i = operations.length - 1; i >= 0; i--)
                curr.right.toGoLeft.add(operations[i].getOpposite());
            this.curr = curr.right; // not goRight()
        }

        public boolean goRight() {
            if (!hasRight())
                return false;
            stopListening();
            currChangeBuffer.clear();
            curr.toGoRight.forEach(StateChangeOperation::operate);
            curr = curr.right;
            startListening();
            return true;
        }

        public boolean goLeft() {
            if (!hasLeft())
                return false;
            stopListening();
            currChangeBuffer.clear();
            curr.toGoLeft.forEach(StateChangeOperation::operate);
            curr = curr.left;
            startListening();
            return true;
        }

        public boolean hasLeft() {
            return curr.left != null;
        }

        public boolean hasRight() {
            return curr.right != null;
        }

        public CircuitState getRight() {
            return curr.right;
        }

        public CircuitState getLeft() {
            return curr.left;
        }

        public int getBufferLength() {
            return currChangeBuffer.size();
        }

        public void clip() {
            curr.right = null;
            curr.toGoRight.clear();
        }

        public void clearBuffer() {
            currChangeBuffer.clear();
        }

        public void onOperationOccurrence(StateChangeOperation op) {
            if (listening) {
                currChangeBuffer.add(op);
            }
        }

        private boolean listening;

        public void stopListening() {
            listening = false;
        }

        public void startListening() {
            listening = true;
        }

        public boolean isListening() {
            return listening;
        }

        public class CircuitState {

            private ArrayList<StateChangeOperation> toGoRight = new ArrayList<>();
            private ArrayList<StateChangeOperation> toGoLeft = new ArrayList<>();

            private CircuitState right = null;
            private CircuitState left = null;
        }
    }

    int opCodeAssign = 0;

    public abstract class StateChangeOperation {

        protected int opCode = opCodeAssign++;

        public StateChangeOperation() {
            stateTracker.onOperationOccurrence(this);
        }

        public abstract StateChangeOperation getOpposite();

        public abstract void operate();
    }

    public class SelectOperation extends StateChangeOperation {

        public Entity selecting;

        public SelectOperation(Entity selected) {
            this.selecting = selected.getSimilarEntity();
        }

        @Override
        public DeselectOperation getOpposite() {
            return new DeselectOperation(selecting);
        }

        @Override
        public void operate() {
            for (Entity e : selecting.getInterceptingEntities())
                if (e.isSimilar(selecting))
                    getEditorPanel().getCurrentSelection().selectWithoutOperation(e);
        }
    }

    public class DeselectOperation extends StateChangeOperation {

        public Entity deselecting;

        public DeselectOperation(Entity deselected) {
            this.deselecting = deselected.getSimilarEntity();
        }

        @Override
        public SelectOperation getOpposite() {
            return new SelectOperation(deselecting);
        }

        @Override
        public void operate() {
            for (Entity e : deselecting.getInterceptingEntities())
                if (e.isSimilar(deselecting))
                    getEditorPanel().getCurrentSelection().deselectWithoutOperation(e);
        }
    }

    public class EntityDeleteOperation extends StateChangeOperation {

        private Entity deleting;

        private HashMap<CircuitPoint, Direction> causingBisectHere = new HashMap<>();

        public EntityDeleteOperation(Entity deleting) {
            deleting = deleting.getSimilarEntity();
            this.deleting = deleting;
            System.out.println("New " + this);
            if (deleting instanceof Wire) {
                Wire del = (Wire) deleting;
                for (CircuitPoint delEdge : del.getEdgePoints()) {
                    EntityList<Wire> wiresInOppositeDir = delEdge.getInterceptingEntities().except(del)
                            .getWiresGoingInOppositeDirection(del);
                    if (wiresInOppositeDir.size() == 1) {
                        boolean touchesEdgePoint = false;
                        Wire inOppositeDir = wiresInOppositeDir.get(0);
                        for (CircuitPoint edge : inOppositeDir.getEdgePoints())
                            if (edge.intercepts(delEdge))
                                touchesEdgePoint = true;
                        if (!touchesEdgePoint) {
                            causingBisectHere.put(delEdge, del.getDirection().getPerpendicular());
                        }
                    }
                }
            }
            if (!causingBisectHere.isEmpty())
                System.out.println("CAUSES BISECT");
        }

        @Override
        public StateChangeOperation getOpposite() {
            EntityAddOperation addOp = new EntityAddOperation(deleting);
            addOp.setCausingMergeMap(causingBisectHere);
            return addOp;
        }

        @Override
        public void operate() {
            EntityList<Entity> scope = deleting.getInterceptingEntities(); // Whatever we end up deleting/shortening is going to touch this similar wire, 'deleting' in some way
            Circuit c = project.getCurrentCircuit();
            System.out.println("OPERATE: " + this);
            boolean didSomething = false;
            if (deleting instanceof Wire) {
                Wire del = (Wire) deleting;
                for (Entity e : scope) {
                    if (!e.existsInCircuit())
                        continue;
                    if ((e instanceof Wire && del.eats((Wire) e)) || del.isSimilar(e)) {
                        System.out.println("del eats " + e + " so e is now deleted");
                        e.remove();
                        didSomething = true;
                    } else if (e instanceof Wire && ((Wire) e).eats(del)) {
                        CircuitPoint sharedEdge = null;
                        Wire eater = (Wire) e; // eater eats del so we know it intercepts both of del's edge points, but
                        System.out.println("Our eater is: " + eater);
                        for (CircuitPoint eaterEge : eater.getEdgePoints())   // del def doesn't intercept both of eater's
                            for (CircuitPoint delEdge : del.getEdgePoints())  // edge points (it would be similar in that case). one of each of theirs may touch though
                                if (eaterEge.equals(delEdge))
                                    sharedEdge = eaterEge.getSimilar(); // In this case they share an edge point <-- this is the case where one of each of theirs touch
                        if (sharedEdge != null) {
                            CircuitPoint delsOtherEdge = del.getOppositeEdgePoint(sharedEdge);
                            CircuitPoint eatersOppositeEdge = eater.getOppositeEdgePoint(sharedEdge);
                            System.out.println("eater (" + eater + ") dot set " + sharedEdge + " to " + delsOtherEdge);
                            eater.set(sharedEdge.clone(c), delsOtherEdge.clone(c));
                            didSomething = true;
                        } else {
                            //           eatFirst = eater.getFirstEdgePoint(); <- Shows how the end points are arranged if these 2 wires were vertical
                            CircuitPoint delFirst = del.getLesserEdgePoint();
                            CircuitPoint delSec = del.getFullerEdgePoint();
                            CircuitPoint eatSecond = eater.getFullerEdgePoint();
                            System.out.println("eater (" + eater + ") dot set " + eatSecond + " to " + delFirst);
                            eater.set(eatSecond.clone(c), delFirst.clone(c));
                            Wire newWire = new Wire(delSec.clone(c), eatSecond.clone(c));
                            System.out.println("Delete operation created " + newWire);
                            didSomething = true;
                        }
                    }
                }
            } else {
                for (Entity e : scope)
                    if (e.isSimilar(deleting))
                        e.remove();
            }

        }

        @Override
        public String toString() {
            return "EntityDeleteOperation{" +
                    "opCode=" + opCode +
                    ", deleting=" + deleting +
                    '}';
        }
    }

    public class EntityAddOperation extends StateChangeOperation {

        private Entity adding;

        private HashMap<CircuitPoint, Direction> causingMergeMap = new HashMap<>();

        public EntityAddOperation(Entity adding) {
            adding = adding.getSimilarEntity();
            this.adding = adding;
            System.out.println("New " + this);
        }

        public void setCausingMergeMap(HashMap<CircuitPoint, Direction> causingMergeMap) {
            this.causingMergeMap = causingMergeMap;
        }

        @Override
        public StateChangeOperation getOpposite() {
            return new EntityDeleteOperation(adding);
        }

        @Override
        public void operate() {
            System.out.println("OPERATE: " + this);
            Entity.parseEntity(Circuit.this, false, adding.toParsableString());
            if (adding instanceof Wire) {
                for (CircuitPoint p : causingMergeMap.keySet()) {
                    System.out.println("CAUSING MERGE AT " + p + " IN DIR " + causingMergeMap.get(p));
                    Direction mergeDir = causingMergeMap.get(p);
                    EntityList<Wire> deleting = p.getInterceptingEntities()
                            .getWiresGoingInOppositeDirection(mergeDir);
                    deleting.deepClone().forEach(wire -> {
                        System.out.println("DELEET: " + wire);
                        removeEntity(wire);
                    });
                    // After these are deleted, the other wires will merge back together
                    if (deleting.get(0).getLesserEdgePoint().isSimilar(deleting.get(1).getFullerEdgePoint())) {
                        new Wire(deleting.get(0).getFullerEdgePoint(), deleting.get(1).getLesserEdgePoint());
                    } else {
                        new Wire(deleting.get(0).getLesserEdgePoint(), deleting.get(1).getFullerEdgePoint());
                    }

                }
            }

        }

        @Override
        public String toString() {
            return "EntityAddOperation{" +
                    "opCode=" + opCode +
                    ", adding=" + adding +
                    '}';
        }
    }

    public class MoveOperation extends StateChangeOperation {

        @Override
        public StateChangeOperation getOpposite() {
            return null;
        }

        @Override
        public void operate() {

        }
    }

}
