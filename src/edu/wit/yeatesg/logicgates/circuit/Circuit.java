package edu.wit.yeatesg.logicgates.circuit;

import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.circuit.entity.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.InputNegatable;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.OutputNegatable;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import edu.wit.yeatesg.logicgates.gui.MainGUI;
import edu.wit.yeatesg.logicgates.gui.Project;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;

import javax.swing.Timer;
import java.util.*;

public class Circuit implements PropertyMutable {

    public Circuit(Project p, String circuitName) {
        if (circuitName == null)
            throw new RuntimeException();
        this.circuitName = circuitName;
        this.project = p;
        this.new InterceptMap();
        this.new ChunkMap();
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

    public class CircuitEntityList<E extends Entity> extends ExactEntityList<E> {

        @Override
        public boolean add(E entity) {
            if (entity.isTemplateEntity())
                throw new RuntimeException("Should not add default entities to Circuit");
   //         System.out.println("[+1][" + allEntities.size() + "]" + "ADD "  + entity + entity.getEntityID());
            if (entity.existsInCircuit())
                throw new RuntimeException("Entity already exists in Circuit");
            if (entity.getCircuit() != Circuit.this)
                throw new RuntimeException("Entity does not have a reference to this Circuit in memory");
            super.add(entity);
            entity.setCircuitIndex(size() - 1);
            entity.onAddToCircuit();
            return true;
        }

        @Override
        public int indexOfExact(E ent) {
            return ent.getCircuitIndex();
        }

        @Override
        public void onSwap(Entity e, int toIndex) {
            e.setCircuitIndex(toIndex);
        }

        @Override
        public boolean removeExact(E e) {
            if (!e.existsInCircuit())
                return false;
            if (get(indexOfExact(e)) != e)
                throw new RuntimeException("Index Discrepency");
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
    //        System.out.println("[-1] " + "Entity removed " + e.toParsableString() + " (" + e.getEntityID() + ")");
            if (e instanceof ConnectibleEntity)
                ((ConnectibleEntity) e).disconnectAll();
            EntityList<Entity> usedToIntercept = e.getInterceptingEntities();
            e.removeInterceptEntries();
            e.removeChunkEntries();
            invalidEntities.removeExact(e); // Just in case it was invalid when it was deleted
            e.onRemove(); // Sets inCircuit field to false, but is encapsulated so I don't do it here
            for (Entity usedTo : usedToIntercept) {
                if (usedTo.existsInCircuit())
                    usedTo.update();
            }
            e.deselect();
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
            if (e.isSimilar(similarTo) && e != similarTo)
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

    public final void spreadUpdateEntities(Collection<? extends Entity> list) {
        list.forEach(Entity::spreadUpdate);
    }

    public final void spreadUpdateEntities(Entity... entities) {
        spreadUpdateEntities(Arrays.asList(entities));
    }

    public final void updateEntitiesAt(CircuitPoint... atLocations) {
        updateEntitiesAt(Arrays.asList(atLocations));
    }

    public final void updateEntitiesAt(Collection<? extends CircuitPoint> atLocations) {
        for (CircuitPoint cp : atLocations)
            for (Entity e : getEntitiesThatIntercept(cp))
                e.update(); // Entities might be updated multiple times, but that's fine
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



    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *  CIRCUIT INTERCEPT MAP. USED TO TRACK ENTITY INTERCEPT POINTS (IN MEMORY) IN LINKED LISTS AT ALL X,Y GRID POINTS
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private InterceptMap interceptMap;

    public InterceptMap getInterceptMap() {
        return interceptMap;
    }

    private boolean updateDisabled;

    public void disableUpdate() {
        updateDisabled = true;
    }

    public void enableUpdate() {
        updateDisabled = false;
    }

    public boolean isUpdateDisabled() {
        return updateDisabled;
    }

    private boolean transformDisabled;

    public void disableTransforms() {
        transformDisabled = true;
    }

    public void enableTransforms() {
        transformDisabled = false;
    }

    public boolean isTransformDisabled() {
        return transformDisabled;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *  CHUNK MAP STUFF (USED FOR RENDERING EFFICIENTLY)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    public static final int NUM_CHUNKS = 64; // Must be even
    public static final int CHUNK_SIZE = 16;

    public static final int CHUNK_MAP_OFFSET = NUM_CHUNKS / 2; // Add to x,y to go from Chunk<x,y> to ChunkMap[x,y]

    public class ChunkMap {

        private Chunk[][] map;

        public ChunkMap() {
            map = new Chunk[NUM_CHUNKS + 1][NUM_CHUNKS + 1];
            Circuit.this.chunkMap = this;
            for (int chunkX = -NUM_CHUNKS/2; chunkX <= NUM_CHUNKS/2; chunkX++)
                for (int chunkY = -NUM_CHUNKS/2; chunkY <= NUM_CHUNKS/2; chunkY++) {
                    if (chunkX != 0 && chunkY != 0) {
                   //     System.out.println("map[" + (chunkX + CHUNK_MAP_OFFSET) + "]" + "[" + (chunkY + CHUNK_MAP_OFFSET) + "] = new Chunk(" + (chunkX) + "," + (chunkY) + ")");
                        map[chunkX + CHUNK_MAP_OFFSET][chunkY + CHUNK_MAP_OFFSET] = new Chunk(chunkX, chunkY);
                    }
                }
        }

        /**
         * Obtains the {@link Chunk} that exists at the given chunkX and chunkY. If there is no chunk here (either because
         * the given chunk coordinate is out of the ChunkMap bounds, or because the chunkX or chunkY are 0), then a new,
         * empty chunk is returned instead of returning null (to prevent NullPointerExceptions).
         * @param chunkX the x-coordinate of this chunk.
         * @param chunkY the y-coordinate of this chunk
         * @return the {@link Chunk} that exists at these chunk coordinates, or an empty chunk if none is found
         */
        private Chunk getChunkAt(int chunkX, int chunkY) {
            try {
                Chunk at;
                return (at = map[chunkX + CHUNK_MAP_OFFSET][chunkY + CHUNK_MAP_OFFSET]) == null ? new Chunk() : at;
            } catch (IndexOutOfBoundsException e) {
                return new Chunk(); // Return a blank chunk
            }
        }
    }

    private ChunkMap chunkMap;

    public Chunk getChunkAt(int chunkX, int chunkY) {
        if (chunkX == 0)
            chunkX = 1;
        if (chunkY == 0)
            chunkY = 1;
        return chunkMap.getChunkAt(chunkX, chunkY);
    }

    public Chunk getChunkAt(CircuitPoint cp) {
        CircuitPoint chunkCoords = cp.getChunkCoords();
        return getChunkAt((int) chunkCoords.x, (int) chunkCoords.y);
    }

    /**
     * Gets the chunks between two grid snapped CircuitPoints
     * @param topLeft the top left grid point
     * @param botRight the bottom right grid point
     * @return a chunk list of all the chunks between these two points
     */
    public ChunkList getChunksBetween(CircuitPoint topLeft, CircuitPoint botRight) {
        ChunkList chunkList = new ChunkList();
        topLeft = topLeft.getChunkCoords();
        botRight = botRight.getChunkCoords();
        for (int chunkX = (int) topLeft.x; chunkX <= (int) botRight.x; chunkX++)
            for (int chunkY = (int) topLeft.y; chunkY <= (int) botRight.y; chunkY++)
                if (chunkX != 0 && chunkY != 0)
                    chunkList.add(getChunkAt(chunkX, chunkY));
        return chunkList;
    }

    public static class ChunkList extends LinkedList<Chunk> {

        public Iterator<Entity> getEntityIterator() {
            return new Iterator<>() {
                private int currIndex = 0;
                private Iterator<Entity> currIterator = currIndex < size() ? get(currIndex).iterator() : null;

                @Override
                public boolean hasNext() {
                    if (currIterator != null && currIterator.hasNext())
                        return true;
                    else if (++currIndex < size()) {
                        currIterator = get(currIndex).iterator();
                        return hasNext();
                    }
                    else return false;
                }

                @Override
                public Entity next() {
                    return currIterator.next();
                }
            };
        }

    }

    /**
     * A Chunk is a collection of entities inside a confined area. The size of this area is determined by the
     * CHUNK_SIZE constant in the Circuit class. If CHUNK_SIZE is 16, then the size of a chu
     */
    public class Chunk implements Iterable<Entity> {

        private int size;

        private ChunkNode first;
        private ChunkNode last;

        private int x;
        private int y;

        private boolean exists = true;

        private Chunk(int x, int y) {
            System.out.println(x + " " + y);
            if (getChunkAt(x, y).exists)
                throw new RuntimeException("This Chunk already exists!");
            this.x = x;
            this.y = y;
        }

        private Chunk() { exists = false; } // Non existent chunk

        @Override
        public String toString() {
            return "Chunk{x=" + x +
                    ", y=" + y +
                    '}';
        }

        public ChunkNode add(Entity e) {
            ChunkNode node = new ChunkNode(e);
            nodeAdd(node);
            return node;
        }

        private void nodeRemove(ChunkNode node) {
            size--;
            if (node == first) {
                if (size == 0) {
                    last = null;
                    first = null;
                } else {
                    first = first.next;
                    first.previous = null;
                }
            } else if (node == last) {
                last.previous.next = null;
                last = last.previous;
            } else {
                node.previous.next = node.next;
                node.next.previous = node.previous;
            }

        }

        private void nodeAdd(ChunkNode node) {
            size++;
            if (first == null ) { // implies last == null as well
                first = node;
                last = node;
                return;
            }
            last.next = node;
            node.previous = last;
            last = node;
        }

        @Override
        public Iterator<Entity> iterator() {
            return new Iterator<>() {
                ChunkNode curr = first;

                @Override
                public boolean hasNext() {
                    return curr != null;
                }

                @Override
                public Entity next() {
                    Entity data = curr.data;
                    curr = curr.next;
                    return data;
                }
            };
        }

        public class ChunkNode {

            private ChunkNode next;
            private ChunkNode previous;
            private Entity data;
            private Chunk chunk;

            private ChunkNode(Entity data) {
                this.chunk = Chunk.this;
                this.data = data;
            }

            public void remove() {
                Chunk.this.nodeRemove(this);
            }

            public Entity getData() {
                return data;
            }

            public ChunkNode getNext() {
                return next;
            }

            public Chunk getChunk() {
                return chunk;
            }
        }

    }


    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *  INTERCEPT MAP STUFF (USED FOR PROGRAM EFFICIENCY)
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */


    /**
     * Intercept map: Used to make the program efficient. Using a limited (however sill big) map size, we can track
     * interceptions much easier. To get the intercepting entities of an Entity e, instead of having to loop through
     * {@link Circuit#allEntities} and compare intercept points, we can instead loop through the intercept points of
     * e, and for each CircuitPoint p, check the [x][y] coordinate of this Array that corresponds to p. This makes
     * the program MUCH faster because the max iterations is determined by how many intercept points e has, instead
     * of having to depend on the number of entities in the circuit because of having to loop through a list.
     */
    public class InterceptMap {

        public static final int MAP_SIZE = NUM_CHUNKS * CHUNK_SIZE + 1; // Must be an odd number for origin to work properly. Don't break this rule.
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
            try {
                return map[x + MAP_OFFSET][y + MAP_OFFSET];
            } catch (IndexOutOfBoundsException e) {
                return new InterceptionList();
            }
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


    public boolean isInMapRange(CircuitPoint p) {
        return interceptMap.isInRange(p);
    }



    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     *  CIRCUIT INVALID ENTITY STUFF. INVALID ENTITIES CANT CONNECT AND ARE DISPLAYED WITH RED CIRCLES ON INVALID POINTS
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private InvalidEntityList invalidEntities = new InvalidEntityList();

    private static class InvalidEntityList extends ExactEntityList<Entity> {
        @Override
        public boolean add(Entity entity) {
            if (entity.getInvalidEntityIndex() != -1) // If it is already invalid
                return false;
            super.add(entity);
            entity.setInvalidEntityIndex(size() - 1);
            return true;
        }

        @Override
        public boolean removeSimilar(Entity entity) {
            throw new RuntimeException("No");
        }

        @Override
        public boolean containsSimilar(Entity entity) {
            throw new RuntimeException("No");
        }

        @Override
        public boolean removeExact(Entity entity) {
            if (!entity.isInvalid())
                return false;
            if (!entity.existsInCircuit())
                throw new RuntimeException("This entity doesn't exist in Circuit and therefore should not be marked invalid in the first place");
            if (get(indexOfExact(entity)) != entity)
                throw new RuntimeException("Index Discrepency. The entity at e's invalidEntityIndex is not == to e");
            if (super.removeExact(entity)) {
                entity.setInvalidEntityIndex(-1);
                return true;
            }
            return false;
        }

        @Override
        public int indexOfExact(Entity ent) {
            return ent.getInvalidEntityIndex();
        }

        @Override
        public void onSwap(Entity e, int toIndex) {
            e.setInvalidEntityIndex(toIndex);
        }
    }

    private EntityList<Entity> getInvalidEntities() {
        return invalidEntities.clone();
    }

    public void markValid(Entity e) {
        invalidEntities.removeExact(e);
    }

    public void markInvalid(Entity e) {
        invalidEntities.add(e);
    }

    public void drawInvalidEntities(GraphicsContext g) {
        boolean movingSelection = getEditorPanel().isMovingSelection();
        for (Entity invalidEntity : invalidEntities)
            for (CircuitPoint interceptPoint : invalidEntity.getInvalidInterceptPoints())
                if (!movingSelection || !invalidEntity.isSelected())
                  drawInvalidGridPoint(g, interceptPoint);
    }

    public void drawInvalidGridPoint(GraphicsContext g, CircuitPoint gridSnapped) {
        double circleSize = scale*0.75;
        double offset = circleSize / 2.0;
        PanelDrawPoint drawPoint = gridSnapped.toPanelDrawPoint();
        double drawX = drawPoint.x - offset;
        double drawY = drawPoint.y - offset;
        g.setFill(Color.rgb(255, 0, 0, 0.15));
        g.fillOval(drawX, drawY, circleSize, circleSize);
    }



    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * CIRCUIT SELECTION STUFF: SELECT ENTITIES TO MOVE/DELETE/COPY/PASTE/DUPLICATE/CHANGE SHARED PROPERTIES BETWEEN EM
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

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
        getEditorPanel().repaint();
    }

    public void selectionTableUpdate() {
        currSelection.selectionUpdate();
    }

    public class Selection extends ExactEntityList<Entity> {

        public Selection(Entity... entities) {
            if (entities != null)
                selectMultiple(Arrays.asList(entities));
        }

        public Selection(Collection<? extends Entity> list) {
            list.forEach(this::select);
        }

        @Override
        public void onSwap(Entity e, int toIndex) {
            e.setSelectionIndex(toIndex);
        }

        @Override
        public int indexOfExact(Entity ent) {
            return ent.getSelectionIndex();
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

        /**
         * The only method that adds elements to this list
         * @param entity
         * @return
         */
        public boolean select(Entity entity) {
            boolean added = false;
            if (!entity.isSelected()) {
                added = super.add(entity);
                entity.onSelect();
            }
            return added;
        }

        public void selectionUpdate() {
            updateConnectionView();
            if (isEmpty())
                project.getGUI().setPropertyTable(Circuit.this);
            else {
                PropertyList list = null;
                for (Entity e : this)
                    if (list == null)
                        list = e.getPropertyList();
                    else
                        list.addParent(e);
                project.getGUI().setPropertyTable(list);
            }
            // TODO update prop table
        }

        public boolean selectAndTrackStateOperation(Entity e) {
            Entity eBeforeSelect = e.getSimilarEntity();
            if (select(e)) {
                new SelectOperation(eBeforeSelect, true);
                return true;
            }
            return false;
        }

        public EntityList<Entity> selectMultiple(Collection<? extends Entity> list) {
            EntityList<Entity> selected = new EntityList<>();
            for (Entity e : list) {
                Entity beforeSelect = e.getSimilarEntity();
                if (select(e))
                    selected.add(beforeSelect);
            }
            return selected;
        }

        public EntityList<Entity> selectMultipleAndTrackOperation(Collection<? extends Entity> list) {
            EntityList<Entity> selected = new EntityList<>();
            for (Entity entity : list) {
                Entity beforeSelect = entity.getSimilarEntity();
                if (selectAndTrackStateOperation(entity))
                    selected.add(beforeSelect);
            }
            return selected;
        }

        public boolean deselect(Entity e) {
            boolean removed = removeExact(e);
            if (removed) {
                e.onDeselect();
                return true;
            }
            return false;
        }

        public boolean deselectAndTrackOperation(Entity e) {
            Entity eBeforeDeselect = e.getSimilarEntity();
            if (deselect(e)) {
                new DeselectOperation(eBeforeDeselect, true);
                return true;
            }
            return false;
        }

        public EntityList<Entity> deselectMultipleAndTrackStateOperation(Collection<? extends Entity> list) {
            disableTransforms();
            disableUpdate();
            EntityList<Entity> deselected = new EntityList<>();
            for (Entity entity : list) {
                Entity simSimma = entity.getSimilarEntity();
                if (deselectAndTrackOperation(entity))
                    deselected.add(simSimma);
            }
            enableTransforms();
            enableUpdate();
            return deselected;
        }

        /**
         * Deselects all entities in the selection that are in the supplied list (by reference).
         * @param list the list of entities that references (==) entities inside the selection that you want to be
         *             deselected
         * @return A List of entities that are <i>similar</i> to the one's that were deselected
         */
        public EntityList<Entity> deselectMultiple(Collection<? extends Entity> list) {
            disableTransforms();
            disableUpdate();
            EntityList<Entity> deselected = new EntityList<>();
            for (Entity entity : list)
                if (deselect(entity))
                    deselected.add(entity.getSimilarEntity());
            enableTransforms();
            enableUpdate();
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
            removing.forEach(Entity::removeWithTrackedStateOperation);
            appendCurrentStateChanges("Delete Selection Of " + removing.size() + " Entities");
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
            Selection clon = new Selection();
            for (Entity e : this)
                clon.superAdd(e);
            return clon;
        }

        public void superAdd(Entity e) {
            super.add(e);
        }

        public ExactEntityList<Entity> deepClone() {
            ExactEntityList<Entity> deepClone = new ExactEntityList<>();
            for (Entity e : this)
                deepClone.add(e.getSimilarEntity());
            return deepClone;
        }
    }

    public class ConnectionSelection extends EntityList<Entity> {

        public javax.swing.Timer blinkTimer;
        public boolean blinkState = true;

        public ConnectionSelection(Entity... entities) {
            super(Arrays.asList(entities));
            blinkTimer = new Timer(760, (e) -> {
                blinkState = !blinkState;
                if (size() > 0)
                    repaint();
            });
            blinkTimer.start();
        }

        public void draw(Entity inThisSelection, GraphicsContext g) {
            if (!blinkState)
                inThisSelection.draw(g);
            else {
                inThisSelection.draw(g, Color.ORANGE, 1);
           //     g.setLineWidth(3);
           //     g.setStroke(Color.ORANGE);
           //     inThisSelection.getBoundingBox().drawBorder(g);
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

        private boolean track;

        public StateChangeOperation(boolean track) {
            this.track = track;
            if (track)
                stateController.onOperationOccurrence(this);
        }

        public boolean isTracked() {
            return track;
        }

        public abstract StateChangeOperation getOpposite();

        public abstract void operate();
    }

    public void bridgeWires(CircuitPointList causingBridgeAt) {
        causingBridgeAt.forEach(this::bridgeWires);
    }

    public void bridgeWires(CircuitPoint p) {
        System.out.println("BRIDGE AT " + p.toParsableString());
        bridgeWires(p, Direction.VERTICAL);
        bridgeWires(p, Direction.HORIZONTAL);
    }

    private void bridgeWires(CircuitPoint p, Direction dir) {
        EntityList<Wire> recreating = new EntityList<>();
        for (Entity e : p.getInterceptingEntities())
            if (e instanceof Wire && ((Wire) e).getDirection() == dir && ((Wire) e).isEdgePoint(p))
                recreating.add((Wire) e);
        ExactEntityList<Wire> wasSelected = new ExactEntityList<>();
        for (Wire w : recreating)
            if (w.isSelected())
                wasSelected.add(w);
        disableUpdate();
        disableTransforms();
        recreating.forEach(Wire::remove);
        recreating.forEach(Wire::disableBisect);
        recreating.forEach(wire -> {
            if (wasSelected.containsExact(wire))
                wire.select();
        });
        enableUpdate();
        enableTransforms();
        p.getInterceptingEntities().forEach(Entity::update);
        recreating.forEach(Wire::add);
        recreating.forEach(Wire::enableBisect);
        recreating.forEach(Wire::update);
    }

    public boolean isWireBridgeAt(CircuitPoint location) {
        boolean foundHorizontalOne = false;
        for (Wire w : location.getInterceptingEntities().getWiresGoingInSameDirection(Direction.HORIZONTAL)) {
            if (!w.isEdgePoint(location)) {
                foundHorizontalOne = true;
                break;
            }
        }
        if (foundHorizontalOne)
            for (Wire w : location.getInterceptingEntities().getWiresGoingInSameDirection(Direction.VERTICAL))
                if (!w.isEdgePoint(location))
                    return true;
        return false;
    }

    private enum SelectiveFocus { SELECTED, NON_SELECTED, ANY }
    public static final SelectiveFocus SELECTED = SelectiveFocus.SELECTED;
    public static final SelectiveFocus NON_SELECTED = SelectiveFocus.NON_SELECTED;
    public static final SelectiveFocus ANY = SelectiveFocus.ANY;

    /**
     * Disables transform and update, must be re enabled when you are done using the method
     * @param mainOperand
     * @param focus
     * @return
     */
    public PieceList<Entity> getOperandsOnCircuit(Entity mainOperand, SelectiveFocus focus) {
        if (mainOperand.existsInCircuit())
            throw new RuntimeException("Operands should be similar but non-existing entities");
        disableTransforms();
        disableUpdate();
        PieceList<Entity> operands = new PieceList<>();
        if (!(mainOperand instanceof Wire)) {
            for (Entity e : mainOperand.getInterceptingEntities()) {
                if (e.isSimilar(mainOperand) && (focus == ANY || e.isSelected() == (focus == SelectiveFocus.SELECTED))) {
                    operands.add(e);
                    break;
                }
            }
        } else {
            Wire building = (Wire) mainOperand;
            PieceList<Wire> wirePieces;
            wirePieces = buildWireFromCircuit(building, focus);
            if (wirePieces == null || wirePieces.isEmpty()) {
                getEditorPanel().lastFuckedUpOperand = building;
                throw new RuntimeException("Could not get Wire pieces for " + building);
            }
            operands.addAll(wirePieces);
            operands.causingBisectHere = wirePieces.causingBisectHere;
        }
        if (operands.isEmpty())
            throw new RuntimeException("Could not find non-wire operand for " + mainOperand.toParsableString());
        return operands;
    }


    /**
     * Attempts to use Wires on the circuit to 'build' a potentially larger Wire. For example, if you use
     * an AddOperation to add a Wire with length 12, and it gets cut in two spots by 2 perpendicular wires after
     * being added/updated, then the respective EntityRemoveOperation needs to be able to 'build' back the Wire out
     * of the pieces on the circuit for the operation to work. So the remove operation would remove 3 wires.
     * @param building a similar Wire that isn't on the Circuit (typically used for state operation memory)
     * @param focus whether or not the Wire being built needs to be selected or needs to be not selected
     * @return an {@link ExactEntityList} containing the Wires on this circuit that would need to be merged together
     * to be equivalent to the similar building Wire.
     */
    public PieceList<Wire> buildWireFromCircuit(Wire building, SelectiveFocus focus) {
        PieceList<Wire> list = new PieceList<>();
        for (CircuitPoint p : new CircuitPoint[] { building.getStartLocation(), building.getEndLocation()}) {
            o: for (Wire w : p.getInterceptingEntities().getWiresGoingInSameDirection(building)) {
                if (!w.isEdgePoint(p)) {
                    w.split(p);
                    EntityList<Wire> wiresInOppositeDir = p.getInterceptingEntities().getWiresGoingInOppositeDirection(w);
                    if (wiresInOppositeDir.size() > 0) {
                        for (Wire oppDir : wiresInOppositeDir)
                            if (oppDir.isEdgePoint(p))
                                continue o;
                        // If we get to here, we know we are causing a bisect
                        list.causingBisectHere.add(p.getSimilar());
                    }
                }
            }
        }
        return getNextPiece(building.getLefterEdgePoint(), building, list, focus);
    }

    private static class PieceList<E extends Entity> extends ExactEntityList<E> {
        private CircuitPointList causingBisectHere = new CircuitPointList();

        @Override
        public PieceList<E> clone() {
            PieceList<E> clone = new PieceList<>();
            clone.addAll(this);
            clone.causingBisectHere = causingBisectHere;
            return clone;
        }

        @Override
        public ExactEntityList<E> deepClone() {
            throw new UnsupportedOperationException();
        }
    }

    // Look for Wire w with left edgePoint at lefter, add it to currPiecesClone, return first nonNull occurrence
    // of a recursive call on the right edge point of w.
    private PieceList<Wire> getNextPiece(CircuitPoint lefter,
                                         Wire building,
                                         PieceList<Wire> currPieces,
                                         SelectiveFocus focus) {
        for (Wire w : lefter.getInterceptingEntities().getWiresGoingInSameDirection(building))
            if (!w.isEdgePoint(lefter))
                w.split(lefter);
        if (building.getRighterEdgePoint().isSimilar(lefter)) {
            return currPieces;
        }
        Wire lastScanned = currPieces.isEmpty() ? null : currPieces.get(currPieces.size() - 1);
        for (Entity e : lefter.getInterceptingEntities()) {
            if (e instanceof Wire
                    && ((Wire) e).getDirection() == building.getDirection()
                    && (e.isSelected() == (focus == SelectiveFocus.SELECTED) || focus == ANY)
                    && ( lastScanned == null || !lastScanned.isSimilar(e)) ) {
                Wire w = (Wire) e;
                if (w.getLefterEdgePoint().intercepts(lefter)) {
                    PieceList<Wire> currOpsClone = currPieces.clone();
                    currOpsClone.add(w);
                    PieceList<Wire> possiblePath;
                    if ((possiblePath = getNextPiece(w.getRighterEdgePoint(), building, currOpsClone, focus)) != null)
                        return possiblePath;
                }
            }
        }
        return null;
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
    //        System.out.println(this.getClass().getSimpleName() + " .operate on " + selecting);
            PieceList<Entity> ops = getOperandsOnCircuit(selecting, SelectiveFocus.NON_SELECTED);
            ops.forEach(Entity::select);
            enableTransforms();
            enableUpdate();
            ops.forEach(Entity::update);
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
            SelectOperation oppa = new SelectOperation(deselecting, false);
            return oppa;
        }

        @Override
        public void operate() {
 //           System.out.println(this.getClass().getSimpleName() + " .operate on " + deselecting.toParsableString());
            PieceList<Entity> ops = getOperandsOnCircuit(deselecting, SELECTED);
            for (Entity e : ops)
                e.deselect();
            enableTransforms();
            enableUpdate();
            ops.forEach(Entity::update);
        }
    }

    public class EntityNegateOperation extends StateChangeOperation {

        private Entity mainOperand;
        private ArrayList<Integer> negatedIndices;
        private SelectiveFocus focus;
        private boolean input;

        private EntityList<Wire> similarCreating = new EntityList<>();
        private EntityList<Wire> similarDeleting = new EntityList<>();

        public EntityNegateOperation(Entity similarNegating, boolean input, ArrayList<Integer> indices, SelectiveFocus focus, boolean track) {
            super(track);
            this.input = input;
            if (input && !(similarNegating instanceof InputNegatable) || !input && !(similarNegating instanceof OutputNegatable))
                throw new RuntimeException("Cannot perform NegateOperation on this entity");
            this.mainOperand = similarNegating.getSimilarEntity();
            this.negatedIndices = new ArrayList<>(indices);
            this.focus = focus;
            if (track)
                indices.forEach(this::determineWireModificationForNegationOf);
        }

        private void determineWireModificationForNegationOf(int indexOfNode) {
            boolean wasNegatedBefore;
            Vector nonNegateToNegateVector;
            CircuitPoint startLoc;
            if (input) {
                InputNegatable mainOp = (InputNegatable) mainOperand;
                InputNode in = mainOp.getInputList().get(indexOfNode);
                wasNegatedBefore = in.isNegated();
                nonNegateToNegateVector = mainOp.getNonNegateToNegateUnitVectorFor(in);
                startLoc = in.getLocation();
            } else {
                OutputNegatable mainOp = (OutputNegatable) mainOperand;
                OutputNode out = mainOp.getOutputList().get(indexOfNode);
                wasNegatedBefore = out.isNegated();
                nonNegateToNegateVector = mainOp.getNonNegateToNegateUnitVectorFor(out);
                startLoc = out.getLocation();
            }

            if (!wasNegatedBefore) {
                CircuitPoint endLoc = startLoc.getIfModifiedBy(nonNegateToNegateVector);
                for (Wire w: startLoc.getInterceptingEntities().getWiresGoingInSameDirection(nonNegateToNegateVector.getGeneralDirection()))
                    if (w.intercepts(endLoc))
                        similarDeleting.add(new Wire(startLoc, endLoc));
            }
            else {
                CircuitPoint endLoc = startLoc.getIfModifiedBy(nonNegateToNegateVector.getMultiplied(-1));
                for (Wire w: startLoc.getInterceptingEntities().getWiresGoingInSameDirection(nonNegateToNegateVector.getGeneralDirection()))
                    if (w.isEdgePoint(startLoc) && !w.intercepts(endLoc))
                        similarCreating.add(new Wire(startLoc, endLoc));
            }

        }

        @Override
        public StateChangeOperation getOpposite() {
            Entity operandClone = mainOperand.getSimilarEntity();
            for (int negatedIndex : negatedIndices) {
                if (input)
                    ((InputNegatable) operandClone).negateInput(negatedIndex);
                else
                    ((OutputNegatable) operandClone).negateOutput(negatedIndex);
            }
            return new EntityNegateOperation(operandClone, input, negatedIndices, focus, false);
        }

        @Override
        public void operate() {
            Entity operand = getOperandsOnCircuit(mainOperand, focus).get(0);
            EntityList<Entity> usedToIntercept = operand.getInterceptingEntities();
            for (int negatedIndex : negatedIndices) {
                if (input)
                    ((InputNegatable) operand).negateInput(negatedIndex);
                else
                    ((OutputNegatable) operand).negateOutput(negatedIndex);
            }
            enableTransforms();
            enableUpdate();
            operand.update();
            usedToIntercept.forEach(Entity::update);

            // On the first occurrence of this operation, we also want to automatically move wires to fit the negate
            // if this operation is tracked, we will also track these wire modification operations so they will undo
            // when this is undone
            if (similarCreating != null)
                similarCreating.forEach(wire -> new EntityAddOperation(wire, isTracked()).operate());
            if (similarDeleting != null)
                similarDeleting.forEach(wire -> new EntityDeleteOperation(wire, isTracked()).operate());
            similarCreating = null;
            similarDeleting = null;
        }
    }



    // Must be selected
    public class SelectionMoveOperation extends StateChangeOperation {

        private Vector movement;

        public SelectionMoveOperation(Vector movement, boolean track) {
            super(track);
            this.movement = movement.clone();
        }

        @Override
        public SelectionMoveOperation getOpposite() {
            ExactEntityList<Entity> deepClone = currSelection.deepClone();
            for (Entity e : deepClone) {
                e.disableUpdate();
                e.move(movement);
            }
            return new SelectionMoveOperation(movement.getMultiplied(-1), false);
        }

        @Override
        public void operate() {
            Selection shallow = currSelection.clone();
            for (Entity e : shallow) {
                e.disableUpdate();
                if (e instanceof Wire)
                    ((Wire) e).disableTransformable();
            }
            for (Entity e : shallow)
                e.move(movement);
            for (Entity e : shallow) {
                e.enableUpdate();
                if (e instanceof Wire)
                    ((Wire) e).enableTransformable();
            }
            for (Entity e : shallow) {
                e.update();
                if (e.isInvalid())
                    e.spreadUpdate();
            }

           /* OperandList<Entity> megaOps = new OperandList<>();
            for (Entity e : moving)
                megaOps.addAll(getOperands(e, SELECTED));
            megaOps.forEach(Entity::disableUpdate);
            megaOps.forEach(entity -> entity.move(movement));
            megaOps.forEach(Entity::enableUpdate);

            clearNonTransformables();
            for (Entity e : megaOps)
                if (!(e instanceof Wire))
                    e.spreadUpdate(); // Wires were already updated with clearNonTransformables().*/
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
        public EntityAddOperation getOpposite() {
            EntityAddOperation oppa = new EntityAddOperation(deleting, false);
            oppa.causingBridgeAt = causingBisectMap;
            return oppa;
        }

        protected CircuitPointList causingBisectMap = new CircuitPointList();

        @Override
        public void operate() {
            PieceList<Entity> ops = getOperandsOnCircuit(deleting, NON_SELECTED);
            EntityList<Entity> usedToIntercept = deleting.getInterceptingEntities();
            causingBisectMap = ops.causingBisectHere;
            for (Entity e : ops) {
                if (e.isSelected())
                    throw new RuntimeException("WHAT THE FUCK YOU");
                e.remove();
            }
            enableTransforms();
            enableUpdate();
            usedToIntercept.forEach(Entity::update);
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

        protected CircuitPointList causingBridgeAt = new CircuitPointList();

        private Entity adding;

        public EntityAddOperation(Entity adding, boolean track) {
            super(track);
            adding = adding.getSimilarEntity();
            this.adding = adding;
        }


        @Override
        public StateChangeOperation getOpposite() {
            return new EntityDeleteOperation(adding, false);
        }

        @Override
        public void operate() {
  //          System.out.println(this.getClass().getSimpleName() + " .operate on " + adding.toParsableString());
            Entity parsed = Entity.parseEntity(Circuit.this, false, adding.toParsableString());
            if (parsed == null)
                throw new NullPointerException();
            parsed.add();
            if (adding instanceof Wire) {
                createBridges();
            }
        }

        public void createBridges() {
            bridgeWires(causingBridgeAt);
        }

        @Override
        public String toString() {
            return "EntityAddOperation{" +
                    "opCode=" + opCode +
                    ", adding=" + adding +
                    '}';
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
    private int scale = 14;

    public static final int SCALE_MAX = 50;
    public static final int SCALE_MIN = 4;
    public static final int SCALE_INC = 2;

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
            if (scale < 10)
                scale +=1;
            else
                scale += SCALE_INC;
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
                scale -= 1;
        }

    }

    public double getLineWidth() {
        return scale * 0.22;
    }

    public int getGridLineWidth() {
        int size = (int) (getScale() / 10);
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

    // Dynamic / 'Propetiable' Methods

    @Override
    public String getPropertyTableHeader() {
        return "Properties For: Circuit";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this, this);
        propList.add(new Property("Circuit Name", "", ""));
        return propList;
    }

    @Override
    public void onPropertyChange(String propertyName, String old, String newVal) {

    }

    @Override
    public void onPropertyChangeViaTable(String propertyName, String old, String newVal) {

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
            e.getCloned(this);
    }

    public Circuit cloneOntoProject(String newName) {
        return new Circuit(project, newName);
    }

    public void setScale(int scale) {
        this.scale = scale;
    }


    private ArrayList<Powerable> markedPowerables = new ArrayList<>();

    public void mark(Powerable marking) {
        marking.setMarked(true);
        markedPowerables.add(marking);
    }

    public void clearMarkedPowerables() {
        for (Powerable d : markedPowerables)
            d.setMarked(false);
        markedPowerables.clear();
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * BUFFERING POWER RESETS
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */



    private ArrayList<Powerable> powerUpdateBuffer = null;

    public void enablePowerUpdateBuffer() {
        powerUpdateBuffer = new ArrayList<>();
    }

    public void disableAndPollPowerUpdateBuffer() {
        Powerable.updateTreesAroundMultiple(powerUpdateBuffer);
        powerUpdateBuffer = null;
    }

    public boolean isBufferingPowerUpdates() {
        return powerUpdateBuffer != null;
    }

    public void powerUpdate(ConnectibleEntity ce) {
        if (isBufferingPowerUpdates()) {
            if (ce instanceof Wire)
                powerUpdateBuffer.add((Wire) ce);
            powerUpdateBuffer.addAll(ce.getInputNodes());
            powerUpdateBuffer.addAll(ce.getOutputNodes());
        } else
            ce.updateTreesNearMe();
    }


    public void recalculateTransmissions() {}
    public void recalculatePowerStatuses() {}




}
