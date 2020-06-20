package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.entity.connectible.*;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import edu.wit.yeatesg.logicgates.gui.MainGUI;
import edu.wit.yeatesg.logicgates.gui.Project;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;

import javax.swing.Timer;
import java.util.*;

public class Circuit implements PropertyMutable {

    public Circuit(Project p, String circuitName) {
        this.circuitName = circuitName;
        this.project = p;
        this.new InterceptMap();
        p.addCircuit(this);
    }

    private String circuitName;

    public String getCircuitName() {
        return circuitName;
    }

    private Project project;

    public EditorPanel getEditorPanel() {
        return project.getEditorPanel();
    }


    // Circuit Entity List Stuff, And Associated Methods

    private CircuitEntityList<Entity> allEntities = new CircuitEntityList<>();

    private class CircuitEntityList<E extends Entity> extends ExactEntityList<E> {

        @Override
        public boolean add(E entity) {
            System.out.println("[+1][" + allEntities.size() + "]" + "ADD "  + entity + entity.getEntityID());
            if (entity.existsInCircuit())
                throw new RuntimeException("Entity already exists in Circuit");
            if (entity.getCircuit() != Circuit.this)
                throw new RuntimeException("Entity does not have a reference to this Circuit in memory");
            super.add(entity);
            entity.onAddToCircuit();
            return true;
        }

        @SuppressWarnings("unchecked")
        public void addAndTrackStateOperation(Entity entity) {
            new EntityAddOperation(entity, true).operate();
        }

        @Override
        public boolean removeExact(E e) {
            if (!e.existsInCircuit())
                throw new RuntimeException("existsInCircuit set to false for this entity");
            if (e.existsInCircuit() != containsExact(e))
                throw new RuntimeException("Discrepancy between existsInCircuit field and circuit.contains(this) for " + e);
            if (super.removeExact(e))
                onRemove(e);
            return false;
        }

        @Override
        public boolean removeSimilar(E e) {
            if (super.removeSimilar(e))
                onRemove(e);
            return false;
        }

        public void onRemove(Entity e) {
            System.out.println("[-1] " + "Entity removed " + e);
            if (e instanceof ConnectibleEntity)
                ((ConnectibleEntity) e).disconnectAll();
            EntityList<Entity> usedToIntercept = e.getInterceptingEntities();
            interceptMap.removeInterceptPointsFor(e);
            invalidEntities.removeExact(e); // Just in case it was invalid when it was deleted
            e.onRemove(); // Sets inCircuit field to false, but is encapsulated so I don't do it here
            for (Entity usedTo : usedToIntercept) {
                if (usedTo.existsInCircuit())
                    usedTo.spreadUpdate();
            }
        }

        /**
         * Removes the Entity that exists on this Circuit that is similar to 'notOnCircuit'. This method uses
         * {@link EntityDeleteOperation#operate()} to do the deletion. The reason why this is necessary in the
         * first place is because when trying to delete multiple Wires at once, if you do not use delete operations,
         * some of the Wires you are deleting will cause other wires you are deleting to be merged, so once you get
         * to those other wires to delete them, their interceptPoints are already off the map and they will try to be
         * deleted again and it wont work. We use this method/EntityDeleteOperations to fix this because they are very
         * smart about how they delete Wires.
         * @param notOnCircuit the Entity that isn't on the Circuit but is similar to an entity that is on the Circuit
         *                     that you want to remove, and you are using this Entity to compare entities on the Circuit
         *                     to for deletion.
         */
        public void removeSimilarEntity(Entity notOnCircuit) {
            new EntityDeleteOperation(notOnCircuit, false).operate();
        }

        /**
         * Almost the same as {@link #removeSimilarEntity(Entity)}, but this method will create an EntityDeleteOperation
         * that will be associated with the removed entity. This operation, upon instantiation, will be tracked by
         * the Circuit's state tracker.
         * @param notOnCircuit the Entity that is not on the Circuit, but there is an Entity on the Circuit that is
         *                     similar to it (or it is a Wire and it eats other wires, those other wires will be
         *                     deleted)
         */
        public void removeSimilarEntityAndTrackOperation(Entity notOnCircuit) {
            EntityDeleteOperation op = new EntityDeleteOperation(notOnCircuit.getSimilarEntity(), true);
            op.operate(); // Doesn't actually have to call remove(). The delete operation will do it for us
        }
    }

    /**
     * Obtains a shallow clone of all of the Entities that exist on this Circuit
     * @return a shallow clone of this Circuit's entity list
     */
    public EntityList<Entity> getAllEntities() {
        return allEntities.clone();
    }

    public EntityList<Entity> getSimilarEntities(Entity similarTo) {
        EntityList<Entity> similar = new EntityList<>();
        for (Entity e : similarTo.getInterceptingEntities())
            if (e.isSimilar(similarTo))
                similar.add(e);
        return similar;
    }

    public Entity getOldestSimilarEntity(Entity similarTo) {
        Entity oldest = null;
        int oldestID = 0;
        for (Entity e : similarTo.getInterceptingEntities()) {
            if (e.isSimilar(similarTo) && (oldest == null || e.getEntityID() < oldestID) ) {
                oldest = e;
                oldestID =  e.getEntityID();
            }
        }
        return oldest;
    }

    public <T extends Entity> EntityList<T> getAllEntitiesOfType(Class<T> type) {
        return allEntities.thatExtend(type);
    }

    public EntityList<Entity> getEntitiesThatIntercept(CircuitPoint p) {
        return interceptMap.getEntitiesThatIntercept(p);
    }

    public int getNumEntities() {
        return allEntities.size();
    }

    public void addEntity(Entity entity) {
        allEntities.add(entity);
    }

    public final void updateEntities(Entity... list) {
        updateEntities(Arrays.asList(list));
    }

    public final void updateEntities(Collection<? extends Entity> list) {
        list.forEach(Entity::update);
    }

    public final void updateEntitiesAt(CircuitPoint... atLocations) {
        updateEntitiesAt(Arrays.asList(atLocations));
    }

    public final void updateEntitiesAt(Collection<CircuitPoint> atLocations) {
        for (CircuitPoint cp : atLocations)
            for (Entity e : getEntitiesThatIntercept(cp))
                e.update(); // Entities might be updated multiple times, but that's fine
    }

    public void addEntityAndTrackOperation(Entity entity) {
        allEntities.addAndTrackStateOperation(entity);
    }

    public void removeExact(Entity entity) {
        allEntities.removeExact(entity);
    }

    public void removeSimilarEntityAndTrackOperation(Entity simsimma) {
        allEntities.removeSimilarEntityAndTrackOperation(simsimma); // Keys in ma bimma
    }

    public void removeSimilarEntity(Entity simsimma) {
        allEntities.removeSimilarEntity(simsimma);
    }


    // Intercept Map / Map Range Stuff

    private InterceptMap interceptMap;

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
                    if (!thatInterceptE.containsExact(otherEntity) && otherEntity != e) {
                        thatInterceptE.add(otherEntity);
                   ///     System.out.println("E = " + e + " ," + " ONCIRCUIT = " + otherEntity.existsInCircuit() + "intercepts = " + otherEntity);
                    }// We WANT != here instead of !equals(). This is because
            return thatInterceptE;                        // we want to be able to check interceptions for similar entities
        }                                                 // that may not be on the circuit

        public void addInterceptPoint(int x, int y, Entity e) {
            if (getRef(x, y).containsExact(e))
                throw new RuntimeException("Already Added " + e);
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
            if (getRef(x, y).removeExact(e))
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

    /**
     * Uses == instead of .equals throughout
     */
    public static class InterceptionList extends ExactEntityList<Entity> {

        @Override
        public InterceptionList clone() {
            InterceptionList clone = new InterceptionList();
            clone.addAll(this);
            return clone;
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



    // Invalidly Intercepting Entity Checks

    private static class InvalidEntityList extends ExactEntityList<Entity> {
        @Override
        public boolean add(Entity entity) {
            if (containsExact(entity))
                return false;
            return super.add(entity);
        }

    }

    private InvalidEntityList invalidEntities = new InvalidEntityList();

    private EntityList<Entity> getInvalidEntities() {
        return invalidEntities.clone();
    }

    public void markValid(Entity e) {
        invalidEntities.removeExact(e);
    }

    public void markInvalid(Entity e) {
        System.out.println(e + " MARKED INVALID");
        if (!e.existsInCircuit())
            throw new RuntimeException("Cannot mark " + e + " as invalid on this Circuit. This Entity does not exist on the Circuit");
        if (e.getInvalidInterceptPoints().isEmpty())
            throw new RuntimeException("This Entity is not invalid. You are dumb");
        invalidEntities.add(e);
    }

    public void drawInvalidEntities() {
        for (Entity invalidEntity : invalidEntities)
            for (CircuitPoint interceptPoint : invalidEntity.getInvalidInterceptPoints())
                drawInvalidGridPoint(interceptPoint);
    }

    public void drawGridPoint(CircuitPoint gridSnapped) {

    }

    public void drawInvalidGridPoint(CircuitPoint gridSnapped) {
        GraphicsContext g = getEditorPanel().getGraphicsContext();
        double circleSize = scale*0.75;
        double offset = circleSize / 2.0;
        PanelDrawPoint drawPoint = gridSnapped.toPanelDrawPoint();
        double drawX = drawPoint.x - offset;
        double drawY = drawPoint.y - offset;
        g.setFill(Color.rgb(255, 0, 0, 0.15));
        g.fillOval(drawX, drawY, circleSize, circleSize);
    }

    public Selection currSelection = new Selection();
    public ConnectionSelection currConnectionView = new ConnectionSelection();

    public Selection currentSelectionReference() {
        return currSelection;
    }

    public ConnectionSelection currentConnectionViewReference() {
        return currConnectionView;
    }

    public boolean select(Entity e) {
        return currSelection.select(e);
    }

    public boolean selectAndTrack(Entity e) {
        return currSelection.selectAndTrackStateOperation(e);
    }

    public EntityList<Entity> selectMultiple(Collection<? extends Entity> list) {
        return currSelection.selectMultiple(list);
    }

    public EntityList<Entity> selectMultipleAndTrack(Collection<? extends Entity> list) {
        return currSelection.selectMultipleAndTrackOperation(list);
    }

    public boolean deselect(Entity e) {
        return currSelection.deselect(e);
    }

    public boolean deselectAndTrack(Entity e) {
        return currSelection.deselectAndTrackOperation(e);
    }

    public EntityList<Entity> deselectAll() {
        return currSelection.deselectAll();
    }

    public EntityList<Entity> deselectAllAndTrack() {
        return currSelection.deselectAllAndTrack();
    }

    public void deleteSelectedEntities() {
        currSelection.deleteAll();
    }

    public void deleteSelectedEntitiesAndTrack() {
        currSelection.deleteAllEntitiesAndTrack();;
    }

    public void deleteMultipleEntities(Collection<? extends Entity> list) {
        Selection select = new Selection(list);
        select.deleteAll();
    }

    public void delete(Entity e) {
        e.remove();
    }

    public void repaint() {
        getEditorPanel().repaint(this);
    }

    public class Selection extends ExactEntityList<Entity> {

        public Selection(Entity... entities) {
            if (entities != null)
                selectMultiple(Arrays.asList(entities));
        }

        public Selection(Collection<? extends Entity> list) {
            list.forEach(this::select);
        }

        public boolean intercepts(CircuitPoint p) {
            for (Entity e : this)
                if (e.getBoundingBox().intercepts(p))
                    return true;
            return false;
        }

        @Override
        public boolean add(Entity entity) {
            throw new UnsupportedOperationException("Use select(Entity)");
        }

        public boolean select(Entity entity) {
            boolean added = false;
            if (!containsExact(entity)) {
                added = super.add(entity);
                updateConnectionView();
            }
            return added;
        }

        public boolean selectAndTrackStateOperation(Entity e) {
            if (select(e)) {
                new SelectOperation(e, true);
                return true;
            }
            return false;
        }

        public EntityList<Entity> selectMultiple(Collection<? extends Entity> list) {
            EntityList<Entity> selected = new EntityList<>();
            for (Entity e : list)
                if (select(e))
                    selected.add(e);
            return selected;
        }

        public EntityList<Entity> selectMultipleAndTrackOperation(Collection<? extends Entity> list) {
            EntityList<Entity> selected = new EntityList<>();
            for (Entity entity : list)
                if (selectAndTrackStateOperation(entity))
                    selected.add(entity);
            return selected;
        }

        public boolean deselect(Entity e) {
            boolean removed = removeSimilar(e);
            e.onDeselect();
            updateConnectionView();
            return removed;
        }

        public boolean deselectAndTrackOperation(Entity e) {
            if (deselect(e)) {
                new DeselectOperation(e, true);
                return true;
            }
            return false;
        }

        public EntityList<Entity> deselectMultipleAndTrackStateOperation(Collection<? extends Entity> list) {
            EntityList<Entity> deselected = new EntityList<>();
            for (Entity entity : list)
                if (deselectAndTrackOperation(entity))
                    deselected.add(entity);
            return deselected;
        }

        public EntityList<Entity> deselectMultiple(Collection<? extends Entity> list) {
            EntityList<Entity> deselected = new EntityList<>();
            for (Entity entity : list)
                if (deselect(entity))
                    deselected.add(entity);
            return deselected;
        }

        public EntityList<Entity> deselectAll() {
            return deselectMultiple(this.clone());
        }

        public EntityList<Entity> deselectAllAndTrack() {
            return deselectMultipleAndTrackStateOperation(this.clone());
        }

        public void deleteAll() {
            EntityList<Entity> removing = deselectAll().deepClone();
            for (Entity e : removing)
                removeSimilarEntity(e);
        }

        /**
         * Always going to push 2 distinct operations: deselect all, then delete all
         */
        public void deleteAllEntitiesAndTrack() {
            EntityList<Entity> removing = deselectAllAndTrack().deepClone();
            for (Entity e : removing)
                e.removeWithTrackedStateOperation();
            appendCurrentStateChanges("Delete Selection Of " + removing.size() + " Entities");
            recalculateTransmissions();
            repaint();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Use deselectAllWithStateOperation");
        }

        public void updateConnectionView() {
            currConnectionView.clear();
            if (size() == 1 && get(0) instanceof ConnectibleEntity) {
                ConnectibleEntity selectedConnectible = (ConnectibleEntity) get(0);
                currConnectionView.addAll(selectedConnectible.getConnectedEntities());
                currConnectionView.resetTimer();
            }
        }

        public boolean intercepts(PanelDrawPoint p) {
            return intercepts(p.toCircuitPoint());
        }

        @Override
        public Selection clone() {
            return new Selection(this);
        }

        public Selection deepClone() {
            return new Selection(super.deepClone());
        }
    }

    public class ConnectionSelection extends EntityList<Entity> {

        public javax.swing.Timer blinkTimer;
        public boolean blinkState = true;

        public ConnectionSelection(Entity... entities) {
            super(Arrays.asList(entities));
            blinkTimer = new Timer(750, (e) -> {
                blinkState = !blinkState;
                if (size() > 0)
                    repaint();
            });
            blinkTimer.start();
        }

        public void draw(Entity inThisSelection, GraphicsContext g) {
            inThisSelection.draw(g);
            if (blinkState) {
                g.setLineWidth(3);
                g.setStroke(Color.ORANGE);
                inThisSelection.getBoundingBox().drawBorder(g);
            }
        }

        public void resetTimer() {
            blinkState = true;
            blinkTimer.restart();
        }

    }


    // State tracking stuff: Operations that are tracked by the Circuit and can be undone/redone

    private CircuitStateController stateController = new CircuitStateController();

    public CircuitStateController stateController() {
        return stateController;
    }

    public void appendCurrentStateChanges() {
        appendCurrentStateChanges(null);
    }

    public void appendCurrentStateChanges(String undoMsg) {
        stateController.appendState(undoMsg);
    }

    public void clearStateChangeBuffer() {
        stateController.clearBuffer();
    }

    public class CircuitStateController {

        private CircuitState first;
        private CircuitState curr;

        private ArrayList<StateChangeOperation> currChangeBuffer = new ArrayList<>();

        public CircuitStateController() {
            first = new CircuitState();
            curr = first;
        }

        public void appendState(String undoMessage) {
            if (currChangeBuffer.isEmpty())
                return;
            clip();
            CircuitState curr = this.curr;
            curr.right = new CircuitState();
            curr.right.left = curr;
            curr.toGoRight.addAll(currChangeBuffer);
            for (int i = currChangeBuffer.size() - 1; i >= 0; i--)
                curr.right.toGoLeft.add(currChangeBuffer.get(i).getOpposite());
            this.curr = curr.right; // not goRight()
            currChangeBuffer.clear();
            this.curr.setUndoMessage(undoMessage);
            updateMenuBars();
        }

        private int getNumToRight() {
            int num = 0;
            CircuitState curr = this.curr;
            while (curr.hasRight()) {
                num++;
                curr = curr.right;
            }
            return num;
        }

        private int getNumToLeft() {
            int num = 0;
            CircuitState curr = this.curr;
            while (curr.hasLeft()) {
                num++;
                curr = curr.left;
            }
            return num;
        }

        public String getUndoMessage() {
            return curr.getUndoMessage();
        }

        public void updateMenuBars() {
            MainGUI gui = project.getGUI();
            MenuItem undoItem = gui.getUndoMenuItem();
            undoItem.setText("Undo ");
            undoItem.setDisable(true);
            MenuItem megaUndoItem = gui.getMegaUndoMenuItem();
            megaUndoItem.setText("Mega Undo ");
            megaUndoItem.setDisable(true);
            if (hasLeft()) {
                undoItem.setText("Undo " + getUndoMessage() + " ");
                undoItem.setDisable(false);
                megaUndoItem.setText("Mega Undo (" + getNumToLeft() + " total operations) ");
                megaUndoItem.setDisable(false);
            }
            MenuItem redoItem = gui.getRedoMenuItem();
            redoItem.setDisable(true);
            redoItem.setText("Redo ");
            MenuItem megaRedoItem = gui.getMegaRedoMenuItem();
            megaRedoItem.setText("Mega Redo ");
            megaRedoItem.setDisable(true);
            if (hasRight()) {
                redoItem.setText("Redo " + getRight().getUndoMessage() + " ");
                megaRedoItem.setText("Mega Redo (" + getNumToRight() + " total operations) ");
                redoItem.setDisable(false);
                megaRedoItem.setDisable(false);
            }
        }

        public boolean goRight() {
            if (!hasRight())
                return false;
            currChangeBuffer.clear();
            curr.toGoRight.forEach(StateChangeOperation::operate);
            curr = curr.right;
            updateMenuBars();
            return true;
        }

        public boolean goLeft() {
            if (!hasLeft())
                return false;
            currChangeBuffer.clear();
            curr.toGoLeft.forEach(StateChangeOperation::operate);
            curr = curr.left;
            updateMenuBars();
            return true;
        }

        public boolean hasLeft() {
            return curr.hasLeft();
        }

        public boolean hasRight() {
            return curr.hasRight();
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
            updateMenuBars();
        }

        public void clearBuffer() {
            currChangeBuffer.clear();
        }

        public void onOperationOccurrence(StateChangeOperation op) {
            currChangeBuffer.add(op);
        }

    }

    public static class CircuitState {

        private ArrayList<StateChangeOperation> toGoRight = new ArrayList<>();
        private ArrayList<StateChangeOperation> toGoLeft = new ArrayList<>();

        private CircuitState right = null;
        private CircuitState left = null;

        private String leftMessage;

        public void setUndoMessage(String message) {
            leftMessage = message;
        }

        public String getUndoMessage() {
            return leftMessage != null ? leftMessage : "";
        }

        public boolean hasLeft() {
            return left != null;
        }

        public boolean hasRight() {
            return right != null;
        }
    }

    int opCodeAssign = 0;

    public abstract class StateChangeOperation {

        protected int opCode = opCodeAssign++;

        public StateChangeOperation(boolean track) {
            if (track)
                stateController.onOperationOccurrence(this);
        }

        public abstract StateChangeOperation getOpposite();

        public abstract void operate();
    }

    public class SelectOperation extends StateChangeOperation {

        public Entity selecting;

        public SelectOperation(Entity selected, boolean track) {
            super(track);
            this.selecting = selected.getSimilarEntity();
        }

        @Override
        public DeselectOperation getOpposite() {
            return new DeselectOperation(selecting, false);
        }

        @Override
        public void operate() {
            Circuit.this.currSelection.select(Circuit.this.getOldestSimilarEntity(selecting));
        }
    }

    public class DeselectOperation extends StateChangeOperation {

        public Entity deselecting;

        public DeselectOperation(Entity deselected, boolean track) {
            super(track);
            this.deselecting = deselected.getSimilarEntity();
        }

        @Override
        public SelectOperation getOpposite() {
            return new SelectOperation(deselecting, false);
        }

        @Override
        public void operate() {
            Circuit.this.currSelection.deselect(Circuit.this.getOldestSimilarEntity(deselecting));
        }
    }

    public class WireShortenOperation extends EntityDeleteOperation {

        private HashMap<CircuitPoint, Direction> causingBisectHere = new HashMap<>();

        public WireShortenOperation(Wire shortening, CircuitPoint edge, CircuitPoint to) {
            super(new Wire(edge, to), true);
            if (new Wire(edge, to).isSimilar(shortening))
                throw new RuntimeException("This should be a delete operation instead");
            EntityList<Entity> toIntercepting = to.getInterceptingEntities().exceptSimilar(shortening);
            EntityList<Wire> toInterceptingWires = toIntercepting.thatExtend(Wire.class);
            if (!to.interceptsWireEdgePoint()
                    && toInterceptingWires.getWiresGoingInOppositeDirection(shortening).size() == 1)
                causingBisectHere.put(to.getSimilar(), shortening.getDirection().getPerpendicular());
        }

        @Override
        public StateChangeOperation getOpposite() {
            EntityAddOperation addOp = new EntityAddOperation(deleting, false);
            addOp.setCausingMergeMap(causingBisectHere);
            return addOp;
        }
    }

    public class EntityDeleteOperation extends StateChangeOperation {

        protected Entity deleting;

        public EntityDeleteOperation(Entity deleting, boolean track) {
            super(track);
            deleting = deleting.getSimilarEntity();
            this.deleting = deleting;
        }

        @Override
        public StateChangeOperation getOpposite() {
            return new EntityAddOperation(deleting, false);
        }

        @Override
        public void operate() {
            EntityList<Entity> scope = deleting.getInterceptingEntities(); // Whatever we end up deleting/shortening is going to touch this similar wire, 'deleting' in some way
            Circuit c = project.getCurrentCircuit();

            EntityList<Entity> deletedOnce = new EntityList<>();
            if (deleting instanceof Wire) {
                // Split into deleting subwires, based on how bisects work
                ArrayList<CircuitPoint> intervalPoints = new ArrayList<>();
                intervalPoints.add(((Wire) deleting).getLefterEdgePoint());
                ((Wire) deleting).edgeToEdgeIterator().forEachRemaining(intPoint -> {
                    EntityList<Wire> intercepting = intPoint.getInterceptingEntities()
                            .getWiresGoingInOppositeDirection((Wire) deleting);
                    for (Wire w : intercepting)
                        if (w.isEdgePoint(intPoint) && !intervalPoints.contains(intPoint))
                            intervalPoints.add(intPoint);
                });
                if (!intervalPoints.contains(((Wire) deleting).getRighterEdgePoint()))
                    intervalPoints.add(((Wire) deleting).getRighterEdgePoint());
                ArrayList<Wire> intervalWires =  new ArrayList<>();
                for (int i = 0, j = 1; j < intervalPoints.size(); i++, j++)
                    intervalWires.add(new Wire(intervalPoints.get(i), intervalPoints.get(j)));
                for (Wire del : intervalWires) {
                    for (Entity e : scope) {
                        //if (e != getOldestSimilarEntity(e))
                         //   continue;
                        if (!deletedOnce.contains(del)) {
                            if (!e.existsInCircuit())
                                continue;
                            if (del.isSimilar(e)) {
                                e.remove();
                                deletedOnce.add(del);
                            } else if (e instanceof Wire && ((Wire) e).eats(del)) {
                                CircuitPoint sharedEdge = null;
                                deletedOnce.add(del);
                                Wire eater = (Wire) e; // eater eats del so we know it intercepts both of del's edge points, but
                                for (CircuitPoint eaterEge : eater.getEdgePoints())   // del def doesn't intercept both of eater's
                                    for (CircuitPoint delEdge : del.getEdgePoints())  // edge points (it would be similar in that case). one of each of theirs may touch though
                                        if (eaterEge.equals(delEdge))
                                            sharedEdge = eaterEge.getSimilar(); // In this case they share an edge point <-- this is the case where one of each of theirs touch
                                if (sharedEdge != null) {
                                    CircuitPoint delsOtherEdge = del.getOppositeEdgePoint(sharedEdge);
                                    CircuitPoint eatersOppositeEdge = eater.getOppositeEdgePoint(sharedEdge);
                                    eater.set(sharedEdge.clone(c), delsOtherEdge.clone(c));
                                } else {
                                    //           eatFirst = eater.getFirstEdgePoint(); <- Shows how the end points are arranged if these 2 wires were vertical
                                    CircuitPoint delFirst = del.getLefterEdgePoint();
                                    CircuitPoint delSec = del.getRighterEdgePoint();
                                    CircuitPoint eatSecond = eater.getRighterEdgePoint();
                                    eater.set(eatSecond.clone(c), delFirst.clone(c));
                                    c.addEntity(new Wire(delSec.clone(c), eatSecond.clone(c)));
                                }
                            }
                        }
                    }
                }
            } else {
                for (Entity e : scope) {
                    if (e.isSimilar(deleting) && !deletedOnce.contains(deleting)) {
                        e.remove();
                        deletedOnce.add(deleting);
                    }
                }
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

        public EntityAddOperation(Entity adding, boolean track) {
            super(track);
            adding = adding.getSimilarEntity();
            this.adding = adding;
        }

        public void setCausingMergeMap(HashMap<CircuitPoint, Direction> causingMergeMap) {
            this.causingMergeMap = causingMergeMap;
        }

        @Override
        public StateChangeOperation getOpposite() {
            return new EntityDeleteOperation(adding, false);
        }

        @Override
        public void operate() {
            System.out.println("Add OP OP");
            Entity parsed = Entity.parseEntity(Circuit.this, false, adding.toParsableString());
            if (parsed == null)
                throw new NullPointerException();
            parsed.add();
            if (adding instanceof Wire) {
                for (CircuitPoint p : causingMergeMap.keySet()) {
                    System.out.println("CAUSING MERGE AT " + p + " IN DIR " + causingMergeMap.get(p));
                    Direction mergeDir = causingMergeMap.get(p);
                    EntityList<Wire> deletingSimilar = p.getInterceptingEntities()
                            .getWiresGoingInOppositeDirection(mergeDir);
                    deletingSimilar.deepClone().forEach(Circuit.this::removeSimilarEntity);
                    // After these are deleted, the other wires will merge back together
                    if (deletingSimilar.get(0).getLefterEdgePoint().isSimilar(deletingSimilar.get(1).getRighterEdgePoint())) {
                        addEntity(new Wire(deletingSimilar.get(0).getRighterEdgePoint(), deletingSimilar.get(1).getLefterEdgePoint()));
                    } else {
                        addEntity(new Wire(deletingSimilar.get(0).getLefterEdgePoint(), deletingSimilar.get(1).getRighterEdgePoint()));
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

    public class EntityMoveOperation extends StateChangeOperation {

        private Entity moving;
        private Vector movement;

        public EntityMoveOperation(Entity similarMoving, Vector movement, boolean track) {
            super(track);
            moving = similarMoving.getSimilarEntity();
            this.movement = movement.clone();
        }

        @Override
        public StateChangeOperation getOpposite() {
            Entity clone = moving.getSimilarEntity();
            clone.move(movement);
            return new EntityMoveOperation(clone, movement.getMultiplied(-1), false);
        }

        @Override
        public void operate() {
            EntityList<Entity> similars = Circuit.this.getSimilarEntities(moving);
            Entity selected = null;
            for (Entity e : similars) {
                if (e.isSelected()) {
                    selected = e;
                    break;
                }
            }
            if (selected == null)
                throw new RuntimeException("Move Operation Without Selected");
            selected.move(movement);
        }
    }

    public class PropertyChangeOperation extends StateChangeOperation {

        private Entity entity;
        private String propertyKey;
        private String oldValue;
        private String newValue;

        public PropertyChangeOperation(Entity beingModified, String propertyKey, String newValue, boolean track) {
            super(track);
            entity = beingModified.getSimilarEntity();
            this.propertyKey = propertyKey;
            this.oldValue = entity.getPropertyValue(propertyKey);
            this.newValue = newValue;
        }

        @Override
        public StateChangeOperation getOpposite() {
            Entity clone = entity.getSimilarEntity();
            clone.onPropertyChange(propertyKey, oldValue, newValue);
            return new PropertyChangeOperation(clone, propertyKey, oldValue, false);
        }

        @Override
        public void operate() {
            EntityList<Entity> scope = entity.getInterceptingEntities();
            for (Entity e : scope) {
                if (e.isSimilar(entity)) {
                    e.onPropertyChange(propertyKey, newValue);
                    return; // Only change one
                }
            }
        }
    }



    // Scale, Offset, Drawing/Color Stuff Below

    public static final Color COL_BG = Color.WHITE;
    public static final Color COL_GRID = Color.DARKGREY;
    public static final Color COL_ORIGIN = Color.RED;

    /**
     * CircuitPoint to CircuitDrawPoint
     * Multiply the x and y of a CircuitPoint by this value to get the CircuitDrawPoint
     * Represents the distance between CircuitPoint 0,0 and 0,1 on the editor panel
     * (how many CircuitDrawPoints can fit between two CircuitPoints aka Grid Points?)
     * */
    private int scale = 15;

    private static final int SCALE_MAX = 60;
    private static final int SCALE_MIN = 5;
    private static final int SCALE_INC = 5;

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
        if (canScaleUp()) {
            if (scale > 5)
                scale +=SCALE_INC;
            else
                scale += 5;
        }

    }

    public boolean canScaleDown() {
        return scale > SCALE_MIN;
    }

    public void scaleDown() {
        if (canScaleDown()) {
            if (scale > 10)
                scale -= SCALE_INC;
            else
                scale -= 5;
        }

    }

    public double getLineWidth() {
        return scale * 0.22;
    }

    public int getGridLineWidth() {
        int size = (int) (getLineWidth() / 2);
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


    public void recalculateTransmissions() {
        Dependent.resetDependencies(this);
        Dependent.resetPowerStatus(this, true); // Reset power statuses and illogicals as well
        Dependent.calculateDependencies(this); // Calculate dependencies, may cause illogicies
        Dependent.calculateSuperDependencies(this);

        Dependent.illogicalCheck(this); // Determine illogicals based on calculated dependencies

        Dependent.resetDependencies(this);
        Dependent.resetPowerStatus(this, false); // Reset power statuses, but not for illogicals
        Dependent.calculateDependencies(this); // Re-calculate dependencies, ignoring illogicals
        Dependent.calculateSuperDependencies(this);

        Dependent.determinePowerStatuses(this);
    }

    public void recalculatePowerStatuses() {
        Dependent.resetPowerStatus(this, false);
        Dependent.determinePowerStatuses(this);
    }


    // Dynamic / 'Propetiable' Methods

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
    public void onPropertyChange(String propertyName, String old, String newVal) {

    }

    @Override
    public String getPropertyValue(String propertyName) {
        return null;
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

}
