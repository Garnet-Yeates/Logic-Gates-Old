package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.Pokable;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import javax.swing.Timer;
import java.util.*;

import static edu.wit.yeatesg.logicgates.entity.connectible.Wire.*;
import static edu.wit.yeatesg.logicgates.def.Circuit.*;

import static javafx.scene.input.KeyCode.*;

public class EditorPanel extends Pane {

    private final Canvas canvas = new Canvas();
    public Project currProject;

    public EditorPanel(Project p) {
        getChildren().add(canvas);
        setListeners();
        currProject = p;
        viewOrigin();
    }

    public GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    public void setListeners() {
        addEventFilter(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);
        addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyReleased);
    }

    public BoundingBox getScreenBoundaries() {
        return new BoundingBox(new PanelDrawPoint(0, 0, c()), new PanelDrawPoint(canvasWidth(), canvasHeight(), c()), null);
    }

    public void modifyOffset(Vector off) {
        c().modifyOffset(off);
    }

    public void onKeyPressed(KeyEvent e) {
        Circuit c = c();
        CircuitStateChain stateController = c.stateController();
        if (e.getCode() == Z && ctrl && stateController.hasLeft()) {
            undo();
            c.refreshTransmissions();
        }
        else if (e.getCode() == Y && ctrl && stateController.hasRight()) {
            redo();
            c.refreshTransmissions();
        }
        if (e.getCode() == DELETE)
            currSelection.deleteSelection();
        if (e.getCode() == B)
            if (currentPullPoint != null)
                currentPullPoint.dropAndRestart();
        if (e.getCode() == R)
            repaint(c());
        if (e.isControlDown() && (e.getCode() == EQUALS || e.getCode() == MINUS)) {
            CircuitPoint oldCenter = getCenter();
            if (e.getCode() == EQUALS && c().canScaleUp()) {
                c().scaleUp();
                view(oldCenter);
            } else if (e.getCode() == MINUS && c().canScaleDown()) {
                c().scaleDown();
                view(oldCenter);
            }
        }
        if (e.getCode() == SPACE)
            holdingSpace = true;
        if (e.getCode() == CONTROL) {
            ctrl = true;
            currentPullPoint = null;
        }
        if (e.getCode() == ESCAPE) {
            currSelection.clear();
            currConnectionView.clear();
        }
        if (e.getCode() == P)
            onPoke();
        repaint(c());
    }

    public void onKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE)
            holdingSpace = false;
        if (e.getCode() == KeyCode.CONTROL)
            ctrl = false;
    }

    public void onMouseMoved(MouseEvent e) {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        this.new PullPoint(gridSnapAtMouse);
        if (!canvas.isFocused())
            canvas.requestFocus();
        if (holdingSpace)
            modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
        updateMousePos(e);
        repaint(c());
    }

    public void onMouseClicked(MouseEvent e) {
        updateMousePos(e);
     /*   System.out.println("MOUSE CLICK:");
        System.out.println(" SCENE: " + e.getSceneX() + " " + e.getSceneY());
        System.out.println(" NORM: " + e.getX() + " " + e.getX());*/
    }

    private CircuitPoint middlePressPointGrid;
    private CircuitPoint middleReleasePointGrid;
    private boolean draggedViaMiddlePress;

    private CircuitPoint pressPointGrid;
    private CircuitPoint releasePointGrid;

    private CircuitPoint rightPressPointGrid;
    private CircuitPoint rightReleasePointGrid;

    private boolean leftDown() {
        return pressPointGrid != null;
    }

    private boolean rightDown() {
        return rightReleasePointGrid != null;
    }

    private boolean middleDown() {
        return middlePressPointGrid != null;
    }


    public void onMousePressed(MouseEvent e) {
        canUserShiftState = false;
        System.out.println("MOUSE PRESS " + e.getButton());
        updateMousePos(e);
        System.out.println(circuitPointAtMouse(true) + " AT MOUSE");
        System.out.println("INTERCEPT MAP ENTRIES AT MOUSE: ");
        for (Entity ent : c().getInterceptMap().get(circuitPointAtMouse(true))) {
            System.out.println("  " + ent);
        }

        if (e.getButton() == MouseButton.MIDDLE) {
            // Middle Click Processing
            draggedViaMiddlePress = false; // Reset this field
            middlePressPointGrid = circuitPointAtMouse(true);
        } else if (e.getButton() == MouseButton.PRIMARY) {
            // Normal Left Click Processing
            pressPointGrid = circuitPointAtMouse(true);
            pressedOnSelectedEntity = currSelection.intercepts(panelDrawPointAtMouse());
            determineSelecting();
            repaint(c());
        } else if (e.getButton() == MouseButton.SECONDARY) {
            rightPressPointGrid = circuitPointAtMouse(true);
            if (currentPullPoint != null)
                currentPullPoint.dropAndRestart();
        }
    }


    public void onMouseReleased(MouseEvent e) {
        canUserShiftState = true;
        System.out.println("MOUSE RELEASE " + e.getButton());
        updateMousePos(e);
        if (e.getButton() == MouseButton.MIDDLE) {
            middleReleasePointGrid = circuitPointAtMouse(true);
            if (middlePressPointGrid.equals(middleReleasePointGrid))
                onPoke();
            middlePressPointGrid = null;
        } else if (e.getButton() == MouseButton.PRIMARY) {
            // Normal Mouse Release
            releasePointGrid = circuitPointAtMouse(true);
            selectionBoxStartPoint = null;
            if (currentPullPoint != null)
                currentPullPoint.onRelease();
            determineSelectingMouseRelease();
            if (currSelectionBox != null)
                onReleaseSelectionBox();
            pressPointGrid = null;
            this.new PullPoint(releasePointGrid);
        } else if (e.getButton() == MouseButton.SECONDARY) {
            rightReleasePointGrid = circuitPointAtMouse(true);
            rightPressPointGrid = null;
        }
        repaint(c());
    }

    public void onMouseDragged(MouseEvent e) {
        if (middleDown() || holdingSpace) {
            draggedViaMiddlePress = true;
            modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
            repaint(c());
        }
        System.out.println("MOUSE DRAG E " + leftDown());
        updateMousePos(e);
        if (leftDown()) {
            if (selectionBoxStartPoint != null)
                onMouseDragWhileCreatingSelectionBox();
            if (gridSnapChanged)
                onGridSnapChangeWhileDragging();
        }
    }

    public Circuit c() {
        return currProject.getCurrentCircuit();
    }

    public Selection currSelection = new Selection();
    public ConnectionSelection currConnectionView = new ConnectionSelection();

    public Selection getCurrentSelection() {
        return currSelection;
    }

    public class Selection extends EntityList<Entity> {

        public Selection(Entity... entities) {
            if (entities != null)
                addAll(Arrays.asList(entities));
        }

        public Selection(Collection<? extends Entity> list) {
            super(list);
        }

        public boolean intercepts(CircuitPoint p) {
            for (Entity e : this)
                if (e.getBoundingBox().intercepts(p))
                    return true;
            return false;
        }

        @Override
        public boolean add(Entity entity) {
            throw new UnsupportedOperationException("Use addWithoutOperation(Entity)");
        }

        public boolean selectWithoutOperation(Entity entity) {
            if (!contains(entity))
                return super.add(entity);
            return false;
        }

        public boolean selectWithStateOperation(Entity e, boolean pushImmediate) {
            if (selectWithoutOperation(e)) {
                c().new SelectOperation(e);
                if (pushImmediate)
                    c().appendCurrentStateChanges();
                return true;
            }
            return false;
        }

        public boolean selectMultipleWithOperation(Collection<? extends Entity> list, boolean pushImmediate) {
            boolean selectedSomething = false;
            for (Entity entity : list)
                if (selectWithStateOperation(entity, false))
                    selectedSomething = true;
            if (selectedSomething) {
                if (pushImmediate)
                    c().appendCurrentStateChanges();
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Use deselectWithoutOperation(Entity)");
        }

        public boolean deselectWithoutOperation(Entity e) {
            return super.remove(e);
        }

        public boolean deselectWithStateOperation(Entity e, boolean pushImmediate) {
            if (deselectWithoutOperation(e)) {
                c().new DeselectOperation(e);
                if (pushImmediate)
                    c().appendCurrentStateChanges();
                return true;
            }
            return false;
        }

        public boolean deselectMultipleWithStateOperation(Collection<? extends Entity> list, boolean pushImmediate) {
            boolean deselectedSomething = false;
            for (Entity entity : list) {
                if (deselectWithStateOperation(entity, false))
                    deselectedSomething = true;
            }
            if (deselectedSomething) {
                if (pushImmediate)
                    c().appendCurrentStateChanges();
                return true;
            }
            return false;
        }

        public boolean deselectAllWithStateOperation(boolean pushImmediae) {
            return deselectMultipleWithStateOperation(this.clone(), pushImmediae);
        }

        /**
         * Always going to push 1 set of 2 types operations: deselect all, then delete all
         */
        public void deleteSelection() {
            Selection deepClone = new Selection(this);
            if (deselectAllWithStateOperation(false)) {
                for (Entity e : deepClone)
                    c().new EntityDeleteOperation(e).operate();
                c().appendCurrentStateChanges();
            }
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Use deselectAllWithStateOperation");
        }

        public boolean intercepts(PanelDrawPoint p) {
            return intercepts(p.toCircuitPoint());
        }

        public Selection clone() {
            return new Selection(this);
        }

        public Selection deepClone() {
            return new Selection(super.deepClone());
        }
    }

    public class ConnectionSelection extends Selection {

        public Timer blinkTimer;
        public boolean blinkState = true;

        public ConnectionSelection(Entity... entities) {
            super(entities);
            blinkTimer = new Timer(750, (e) -> {
                blinkState = !blinkState;
                if (size() > 0)
                    repaint(c());
            });
            blinkTimer.start();
        }

        @Override
        public boolean add(Entity entity) {
            return selectWithoutOperation(entity);
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

        @Override
        public void clear() {
            for (Entity e : clone())
                deselectWithoutOperation(e);
        }
    }

    public Color backgroundColor = Color.WHITE;

    private void drawGridPoints(GraphicsContext g) {
        int w = canvasWidth(), h = canvasHeight();
        for (int x = (int)(-w*0.1); x < w + w*0.1; x += c().getScale()) {
            for (int y = (int)(-h*0.1); y < h + h*0.1; y += c().getScale()) {
                CircuitPoint gridPoint = circuitPointAt(x, y, true);
                PanelDrawPoint drawLoc = gridPoint.toPanelDrawPoint();
                g.setLineWidth(c().getGridLineWidth());

                g.setStroke(Circuit.COL_GRID);
                if (gridPoint.representsOrigin()) {
                    int strokeSize = c().getLineWidth();
                    strokeSize *= 1.5;
                    if (strokeSize == c().getLineWidth())
                        strokeSize++;
                    g.setLineWidth(strokeSize);
                    g.setStroke(Circuit.COL_ORIGIN);
                }
                g.strokeLine(drawLoc.x, drawLoc.y, drawLoc.x, drawLoc.y);
            }
        }
    }


    public void viewOrigin() {
        c().setXOffset(canvasWidth() / 2);
        c().setYOffset(canvasHeight() / 2);
    }

    public void view(CircuitPoint location) {
        viewOrigin();
        PanelDrawPoint viewingLoc = location.toPanelDrawPoint();
        PanelDrawPoint originLoc = new CircuitPoint(0, 0, c()).toPanelDrawPoint();
        c().modifyOffset(new Vector(viewingLoc, originLoc));
    }

    public CircuitPoint getCenter() {
        return getCenter(false);
    }

    public CircuitPoint getCenter(boolean gridSnap) {
        return circuitPointAt(canvasWidth() / 2, canvasHeight() / 2, gridSnap);
    }


    private int mouseX;
    private int mouseY;

    public void updateMousePos(MouseEvent e) {
        mouseX = (int) e.getX();
        mouseY = (int) e.getY();
        updateDidGridSnapChange();
    }

    private CircuitPoint lastGridSnap;
    boolean gridSnapChanged = false;

    public void updateDidGridSnapChange() {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        if (!gridSnapAtMouse.equals(lastGridSnap)) {
            lastGridSnap = gridSnapAtMouse;
            gridSnapChanged = true;
        } else {
            gridSnapChanged = false;
        }
    }

    public CircuitPoint circuitPointAtMouse(boolean gridSnap) {
        return circuitPointAt(mouseX, mouseY, gridSnap);
    }

    public PanelDrawPoint panelDrawPointAtMouse() {
        return panelDrawPointAt(mouseX, mouseY);
    }

    public PanelDrawPoint panelDrawPointAt(int x, int y) {
        return new PanelDrawPoint(x, y, c());
    }

    public CircuitPoint circuitPointAt(int x, int y, boolean gridSnap) {
        CircuitPoint cp = new PanelDrawPoint(x, y, c()).toCircuitPoint();
        return gridSnap ? cp.getGridSnapped() : cp;
    }


    private boolean movingSelection = false;
    private boolean ctrl = false;
    private CircuitPoint selectionBoxStartPoint;

    @SuppressWarnings("unchecked")
    public void determineSelecting() {
        System.out.println("determine selecting. curr selection size before: " + currSelection.size());
        selectionBoxStartPoint = null;
        selectedSomething = false;
        movingSelection = false;
        CircuitPoint atMouse = circuitPointAtMouse(false);
        if (currentPullPoint != null) {
            System.out.println("non null pp");
            currSelection.deselectAllWithStateOperation(false);
            currConnectionView.clear();
        } else {
            System.out.println("null pp");
            if (!currSelection.isEmpty() && !currSelection.intercepts(atMouse) && !ctrl) {
                currSelection.deselectAllWithStateOperation(false);
                currConnectionView.clear();
            }
            ArrayList<Entity> deselected = new ArrayList<>();
            if (!currSelection.isEmpty() && currSelection.intercepts(atMouse)) {
                if (ctrl) {
                    boolean canDeselect = true;
                    for (Entity e : c().getAllEntities()) {
                        if (e.getBoundingBox() != null
                                && e.getBoundingBox().intercepts(atMouse)
                                && !currSelection.contains(e)) {
                            canDeselect = false; // Don't deselect if something isn't selected at where they ctrl clicked
                        }
                    }
                    for (Entity e : currSelection.clone()) {
                        if (canDeselect && e.getBoundingBox().intercepts(atMouse)) {
                            deselected.add(e);
                        }
                    }
                }
                currSelection.deselectMultipleWithStateOperation(deselected, false);
            }

            if (currSelection.isEmpty() || ctrl) {
                ArrayList<Entity> potentialClickSelection = new ArrayList<>();
                for (Entity e : atMouse.getInterceptingEntities()) {
                    if (e.getBoundingBox() != null
                            && e.getBoundingBox().intercepts(atMouse)
                            && !deselected.contains(e))
                        potentialClickSelection.add(e);
                }
                if (potentialClickSelection.size() > 0) {
                    selectedSomething = currSelection.selectMultipleWithOperation(potentialClickSelection, false);
                } else
                    selectionBoxStartPoint = circuitPointAtMouse(false);
            }

            if (!currSelection.isEmpty() && currSelection.intercepts(atMouse) && !ctrl) {
                movingSelection = true;
            }

            if (currSelection.size() != 1)
                currConnectionView.clear();

            if (currSelection.size() == 1
                    && currConnectionView.isEmpty()
                    && currSelection.get(0) instanceof ConnectibleEntity) {
                ConnectibleEntity selectedConnectible = (ConnectibleEntity) currSelection.get(0);
                currConnectionView.addAll(selectedConnectible.getConnectedEntities());
                currConnectionView.resetTimer();
            }
            c().appendCurrentStateChanges();
        }
   }

    public class SelectionBox extends BoundingBox {

        public SelectionBox(CircuitPoint corner1, CircuitPoint corner2) {
            super(corner1, corner2, null);
        }

        @Override
        public void paint(GraphicsContext g) {
            g.setLineWidth(2);
            g.setStroke(Color.rgb(0, 100, 230, 1));
            g.setFill(Color.rgb(0, 100, 200, 0.3));
            PanelDrawPoint p1 = this.p1.toPanelDrawPoint();
            g.fillRect(p1.x, p1.y, getDrawWidth(), getDrawHeight());
            g.strokeRect(p1.x, p1.y, getDrawWidth(), getDrawHeight());
            // super.paint(g);
        }

        public void selectEntities() {
            currSelection.selectMultipleWithOperation(this.getInterceptingEntities(), true);
        }
    }

    public void onGridSnapChangeWhileDraggingSelection() {
        System.out.println("GridSnapChangeWhileDraggingSelection");
    }

    private SelectionBox currSelectionBox = null;

    public void onMouseDragWhileCreatingSelectionBox() {
        currSelectionBox = new SelectionBox(selectionBoxStartPoint, circuitPointAtMouse(false));
        repaint(c());
    }

    public void onReleaseSelectionBox() {
        currSelectionBox.selectEntities();
        currSelectionBox = null;
    }

    boolean pressedOnSelectedEntity = false;
    boolean selectedSomething = false;

    private void determineSelectingMouseRelease() {
        System.out.println("mouse releese selecting " + currSelection.size());
        if (pressPointGrid.equals(releasePointGrid) && (currSelection.isEmpty() || ctrl)) {
            if (!pressedOnSelectedEntity && !selectedSomething) {
                determineSelecting();
            }
        }
    }

    public void onPoke() {
        for (Entity e : c().getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    ((Pokable) e).onPoke();
            }
        }
        c().refreshTransmissions();
    }

    public void onGridSnapChangeWhileDragging() {
        System.out.println(currentPullPoint + " POIL");
        if (currentPullPoint != null) {
            System.out.println(currentPullPoint);
            currentPullPoint.onDragGridSnapChange();
        }
        if (movingSelection)
            onGridSnapChangeWhileDraggingSelection();
    }

    private boolean holdingSpace;

    @Override
    protected void layoutChildren() {
        final int top = (int)snappedTopInset();
        final int right = (int)snappedRightInset();
        final int bottom = (int)snappedBottomInset();
        final int left = (int)snappedLeftInset();
        final int w = (int)getWidth() - left - right;
        final int h = (int)getHeight() - top - bottom;
        canvas.setLayoutX(left);
        canvas.setLayoutY(top);
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            repaint(c());
        }
    }

    public int canvasWidth() {
        return (int) canvas.getWidth();
    }

    public int canvasHeight() {
        return (int) canvas.getHeight();
    }

    public void repaint(Circuit calledFrom) {
        if (calledFrom != null && calledFrom.getCircuitName().contains("theoretical"))
            return;
        Project p = currProject;
        Circuit c = c();
        Platform.runLater(() -> {
            double width = canvasWidth();
            double height = canvasHeight();

            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.clearRect(0, 0, width, height);
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvasWidth(), canvasHeight());

            gc.setStroke(Color.PINK);
            gc.setLineWidth(3);
            gc.strokeRect(0, 0, canvasWidth(), canvasHeight());

            gc.setFill(backgroundColor);
            gc.fillRect(0, 0, getWidth(), getHeight());

            drawGridPoints(gc);

            for (Entity e : c.getAllEntities())
                if (!currConnectionView.contains(e))
                    e.draw(gc);

            for (Entity e : currConnectionView) {
                e.getBoundingBox().drawBorder(gc);
                currConnectionView.draw(e, gc);
            }

            for (Entity e : currSelection)
                e.getBoundingBox().paint(gc);

            if (currentPullPoint != null)
                currentPullPoint.drawPullPoint(gc);

            if (currSelectionBox != null)
                currSelectionBox.paint(gc);
        });
    }


    // Mouse press: initialize pull point. it is only calculate on mouse press
    // Mouse release,, nullify pull point. therefore, inner class

    // Called on mouse press, or in special cases when you right click while pulling wire

    private PullPoint currentPullPoint;

    private enum PullPointState { DELETE_ONLY, DELETE_ONLY_BUT_CANT, CREATE_DELETE, CREATE }

    private boolean canUserShiftState = true;

    private boolean getCanUserShiftState() {
        return canUserShiftState;
    }

    private void undo() {
        if (canUserShiftState)
            c().stateController().goLeft();
    }

    private void redo() {
        if (canUserShiftState)
            c().stateController().goRight();
    }

    private int ppStateShift = 0;

    class PullPoint extends CircuitPoint {

        private CircuitPoint originalLoc;

        private boolean lock;

        private CircuitPoint pressPoint;
        private Direction pullDir;

        private boolean deleteOnly;
        private boolean canDelete;

        public PullPoint(CircuitPoint location, CircuitPoint originalLoc, boolean lock) {
            super(location.x, location.y, originalLoc.getCircuit());
            this.lock = lock;
            pressPoint = circuitPointAtMouse(true);
            if (!canBePlacedHere(pressPoint) || !currSelection.isEmpty() || ctrl) {
                currentPullPoint = null;
                repaint(c());
                return;
            }
            EntityList<ConnectibleEntity> cesAtStart = c().getEntitiesThatIntercept(pressPoint).ofType(ConnectibleEntity.class);
            pullDir = null;
            this.originalLoc = originalLoc;
            currentPullPoint = this;
            if (originalLoc.equals(pressPoint) && !lock)
                for (Wire w : c().getAllEntitiesOfType(Wire.class))
                    if (w.isEdgePoint(pressPoint))
                        canDelete = true;
            if (!canCreateFromAll(cesAtStart))
                deleteOnly = true;
            repaint(c());
        }

        public PullPoint(CircuitPoint location) {
            this(location, location, false);
        }

        public boolean canBePlacedHere(CircuitPoint location) {
            for (ConnectibleEntity ce : c().getAllEntitiesOfType(ConnectibleEntity.class))
                if (ce.canPullPointGoHere(location))
                    return true;
            return false;
        }

        public boolean canCreateFromAll(EntityList<ConnectibleEntity> list) {
            if (list.isEmpty())
                return false;
            for (ConnectibleEntity e : list)
                if (!e.canCreateWireFrom(pressPoint))
                    return false;
            return true;
        }

        public void onDragGridSnapChange() {
            Circuit c = c();
            CircuitPoint start = pressPoint.clone();
            CircuitPoint end = circuitPointAtMouse(true);

            if (ppStateShift != 0) {
                ppStateShift--;
                c.stateController().goLeft();
            } else
                c.stateController().clearBuffer();

            // Re-update intercepting connectibles after the state went back
            EntityList<ConnectibleEntity> cesAtStart = c().getEntitiesThatIntercept(start).ofType(ConnectibleEntity.class);
            EntityList<ConnectibleEntity> cesAtEnd = c.getEntitiesThatIntercept(end).ofType(ConnectibleEntity.class);
            EntityList<Wire> startAndEndWires = cesAtStart.thatIntercept(end).ofType(Wire.class);
            if (startAndEndWires.size() > 1 && !start.isSimilar(end))
                throw new RuntimeException("Should not intercept 2 wires twice");
            Wire deleting = !end.isSimilar(start) && startAndEndWires.size() > 0 ? startAndEndWires.get(0) : null;

            if (canDelete && deleting != null && !lock) {
                // DO THE OPERATION FIRST SO IT CAN PROPERLY CHECK THE SPECIAL CASE WHERE THE DELETED WIRE CAUSES A BISECT
                c().new EntityDeleteOperation(new Wire(start.clone(), end.clone(), false));
                deleting.set(start, end);
            } else {
                boolean canStillCreate = true;
                boolean canSlide = false; // If any entity at the start loc of the pullpoint can connect to any entity
                for (ConnectibleEntity startEntity : cesAtStart) // at the end loc of the pullpoint, or if they are
                    for (ConnectibleEntity endEntity : cesAtEnd) // equal, we can slide
                        if (startEntity.hasConnectionTo(endEntity) || startEntity.isSimilar(endEntity))
                            canSlide = true;
                if (end.isSimilar(start) // <- Case where they switch preferred dir of Wire path finding
                        || (!lock // <- Case where they slide the PullPoint to an adjacent connectible
                            && end.is4AdjacentTo(start)
                            && PullPoint.this.canBePlacedHere(end))
                            && canSlide) {
                    EditorPanel.this.new PullPoint(end, originalLoc, lock);
                    canStillCreate = false;
                }
                // by here, we would have returned if the PullPoint is being slid. So this point and forward
                // covers the case of the user creating wires
                if (canStillCreate && !end.equals(start) && cesAtStart.intersection(cesAtEnd).size() == 0) {
                    if (pullDir == null) {
                        Vector dir = new Vector(start, end);
                        if (Vector.getDirectionVecs().contains(dir)) {
                            if (dir.equals(Vector.LEFT) || dir.equals(Vector.RIGHT))
                                pullDir = Direction.HORIZONTAL;
                            else
                                pullDir = Direction.VERTICAL;
                        }
                    } // <- Update pull dir if it's null
                    WireGenerator generator = new WireGenerator(1);
                    ArrayList<TheoreticalWire> theos = generator.genWirePathLenient(start, end, pullDir, 8);
                    if (theos == null && pullDir != null) // If we couldn't do it in their preferred dir, try the other
                        theos = generator.genWirePathLenient(start, end, pullDir.getPerpendicular(), 8);
                    theos = theos == null ? new ArrayList<>() : theos;
                    for (Wire t : theos) { // 'new Wire' automatically adds it to the theoretical circuit
                        System.out.println("ADDACIOUS ADD: " + "[[ " + t.getStartLocation().toParsableString() + " " + t.getEndLocation().toParsableString() + " ]]");;
                        new Wire(t.getStartLocation().clone(), t.getEndLocation().clone());
                        // Separate add operation from new Wire. because the new wire may be merged etc and make the delete operation invalid
                        c.new EntityAddOperation(new Wire(t.getStartLocation().clone(), t.getEndLocation().clone(), false));
                    }
                }
            }

            if (c.stateController().getBufferLength() > 0) {
                c.appendCurrentStateChanges();
                ppStateShift++;
            }


            c.refreshTransmissions();
            repaint(c);
        }

        public void onRelease() {
            ppStateShift = 0;
            currentPullPoint = null;
            repaint(c());
        }

        boolean droppingAndRestarting;

        public void dropAndRestart() {
            if (ppStateShift > 0) {
                droppingAndRestarting = true;
                CircuitPoint releasePoint = circuitPointAtMouse(true);
                onRelease();
                EditorPanel.this.new PullPoint(releasePoint, releasePoint, true);
                currentPullPoint.onDragGridSnapChange();
            }
        }

        private PullPointState getState() {
            if (deleteOnly && canDelete)
                return PullPointState.DELETE_ONLY;
            else if (deleteOnly)
                return PullPointState.DELETE_ONLY_BUT_CANT;
            else if (canDelete)
                return PullPointState.CREATE_DELETE;
            else
                return PullPointState.CREATE;
        }

        private void drawPullPoint(GraphicsContext g) {
            Color col = null;
            switch (getState()) {

                case DELETE_ONLY:
                    col = Color.rgb(220, 0, 0);
                    break;
                case DELETE_ONLY_BUT_CANT:
                    break;
                case CREATE_DELETE:
                    col = Color.rgb(217, 223, 0, 1);
                    break;
                case CREATE:
                    col = Color.rgb(60, 200, 0, 1);
                    break;
            }
                int strokeSize = (int) (c().getLineWidth() * 0.8);
                if (strokeSize % 2 == 0) strokeSize++;
                int circleSize = (int) (c().getScale() / 2.5);
                if (circleSize % 2 != 0) circleSize++;
                int bigCircleSize = (int) (circleSize * 1.5);
                if (bigCircleSize % 2 != 0) bigCircleSize++;
                PanelDrawPoint dp = currentPullPoint.toPanelDrawPoint();
                g.setLineWidth(strokeSize);
                g.setStroke(Color.BLACK);
                g.strokeOval(dp.x - bigCircleSize/2.00, dp.y - bigCircleSize/2.00, bigCircleSize, bigCircleSize);
                if (col != null) {
                    g.setStroke(col);
                    g.strokeOval(dp.x - circleSize/2.00, dp.y - circleSize/2.00, circleSize, circleSize);
                }
        }
    }

}