package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.ExactEntityList;
import edu.wit.yeatesg.logicgates.entity.Pokable;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
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

import static edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire.*;
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
        Circuit.Selection currSelection = c.currentSelectionReference();
        KeyCode code = e.getCode();
        if (code == DELETE)
            currSelection.deleteAllEntitiesAndTrack();
        if (code == B)
            if (currentPullPoint != null)
                currentPullPoint.dropAndRestart();
        if (code == N) {
            System.out.println("NUM ENTITIES: " + c().getNumEntities());
        }
        if ((code == RIGHT || code == LEFT || code == UP || code == DOWN )
                && !currSelection.isEmpty()) {
            System.out.println(code.getName());
            for (Entity ent : currSelection) {
                Vector vec = Vector.directionVectorFrom(code.getName());
                ent.move();
                new Circuit.EntityMoveOperation(ent, )
            }
            repaint(c);
            c.recalculateTransmissions();
        }

        if (code == R) {
            for (Entity selected : currSelection) {
                if (selected.hasProperty("rotation")) {
                    int rotation = Integer.parseInt(selected.getPropertyValue("rotation"));
                    selected.onPropertyChange("rotation", rotation + "", LogicGates.getNextRotation(rotation) + "");
                }
            }
        }
        if (e.isControlDown() && (code == EQUALS || code == MINUS)) {
            CircuitPoint oldCenter = getCenter();
            if (code == EQUALS && c().canScaleUp()) {
                c().scaleUp();
                view(oldCenter);
                repaint(c);
            } else if (code == MINUS && c().canScaleDown()) {
                c().scaleDown();
                view(oldCenter);
                repaint(c);
            }
        }
        if (code == SPACE)
            holdingSpace = true;
        if (code == SHIFT) {
            shift = true;
            if (ppStateShift == 0)
                currentPullPoint = null;
            repaint(c);
        }
        if (code == ESCAPE) {
            if (currSelection.size() > 0) {
                currSelection.deselectAllAndTrack();
                c.appendCurrentStateChanges("Deselect All Via Esc");
            }
            if (ppStateShift > 0) {
                c().stateController().goLeft(); // Undo() wont work
                c().stateController().clip();
                currentPullPoint = null;
                this.new PullPoint(circuitPointAtMouse(true));
            }
            c.recalculateTransmissions();
            repaint(c);
        }
        if (code == P) {
            onPoke();
            repaint(c);
        }
        e.consume();
    //    repaint(c());
        c.recalculateTransmissions();
        currSelection.updateConnectionView();
        repaint(c);
    }

    public void onKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE)
            holdingSpace = false;
        if (e.getCode() == SHIFT)
            shift = false;
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        c.recalculateTransmissions();
        currSelection.updateConnectionView();
        repaint(c);
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
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        canUserShiftState = false;
        gridSnapJustChanged = false;
        gridSnapChangedSinceLastPress = false;
        System.out.println("MOUSE PRESS " + e.getButton());
        updateMousePos(e);
        System.out.println("MOUSE PRESS: GRID SNAP CHANGE? " + gridSnapJustChanged);
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
        c.recalculateTransmissions();
        currSelection.updateConnectionView();
        repaint(c());

    }

    public void onMouseReleased(MouseEvent e) {
        canUserShiftState = true;
        updateMousePos(e);
        System.out.println("MOUSE RELEASE " + e.getButton() + " did gridsnap change? " + gridSnapJustChanged);
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
        c().recalculateTransmissions();
        updateConnectionView();
        repaint(c());

        gridSnapJustChanged = false;
    }

    public void repaint() {
        repaint(c());
    }

    public void updateConnectionView() {
        c().currentSelectionReference().updateConnectionView();
    }

    public void onMouseDragged(MouseEvent e) {
        if (middleDown() || holdingSpace) {
            draggedViaMiddlePress = true;
            modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
            repaint(c());
        }
        updateMousePos(e);
        if (leftDown()) {
            if (selectionBoxStartPoint != null)
                onMouseDragWhileCreatingSelectionBox();
            if (gridSnapJustChanged)
                onGridSnapChangeWhileDragging();
        }
    }

    public Circuit c() {
        return currProject.getCurrentCircuit();
    }


    private void drawGridPoints(GraphicsContext g) {
        int w = canvasWidth(), h = canvasHeight();
        Color col = COL_GRID;
        double whiteWeight = 0;
        if (c().getScale() == 5)
            whiteWeight = 0.3;
        double normWeight = 1 - whiteWeight;
        int white = (int) (255.0*whiteWeight);
        Color paintCol = Color.rgb((int) (white + col.getRed()*255*normWeight),
                (int) (white + col.getGreen()*255*normWeight),
                (int) (white + col.getBlue()*255*normWeight), 1);
        for (int x = (int)(-w*0.1); x < w + w*0.1; x += c().getScale()) {
            for (int y = (int)(-h*0.1); y < h + h*0.1; y += c().getScale()) {
                CircuitPoint gridPoint = circuitPointAt(x, y, true);
                if (gridPoint.isInMapRange()) {
                    PanelDrawPoint drawLoc = gridPoint.toPanelDrawPoint();
             //       System.out.println(paintCol.getRed() + " " + paintCol.getGreen() + " " + paintCol.getBlue());
                    g.setLineWidth(c().getGridLineWidth());
                    g.setStroke(paintCol);
                    if (gridPoint.representsOrigin()) {
                        double strokeSize = c().getLineWidth();
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
    private boolean gridSnapChangedSinceLastPress = false;
    private boolean gridSnapJustChanged = false;

    public void updateDidGridSnapChange() {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        if (!gridSnapAtMouse.equals(lastGridSnap)) {
            lastGridSnap = gridSnapAtMouse;
            gridSnapJustChanged = true;
            gridSnapChangedSinceLastPress = true;
        } else {
            gridSnapJustChanged = false;
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

    private boolean shift = false;
    private CircuitPoint selectionBoxStartPoint;
    private boolean pressedOnSelectedEntity = false;
    private boolean selectedSomethingLastPress = false;

    @SuppressWarnings("unchecked")
    public void determineSelecting() {
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        System.out.println("determine selecting. curr selection size before: " + currSelection.size());
        selectionBoxStartPoint = null;
        selectedSomethingLastPress = false;
        movingSelection = false;
        CircuitPoint atMouse = circuitPointAtMouse(false);
        ArrayList<Entity> deselected = new ArrayList<>();
        EntityList<Entity> selected = new EntityList<>();

        if (currentPullPoint != null) {
            deselected.addAll(currSelection);
            currSelection.deselectAllAndTrack();
        } else {
            // Condition where they have a current selection, but clicked out of the selection without holding shift
            if (!currSelection.isEmpty() && !currSelection.intercepts(atMouse) && !shift) {
                deselected.addAll(currSelection);
                currSelection.deselectAllAndTrack();
            }
            if (!currSelection.isEmpty() && currSelection.intercepts(atMouse)) {
                if (shift) {
                    boolean canDeselect = true;
                    for (Entity e : c().getAllEntities()) {
                        if (e.getBoundingBox() != null
                                && e.getBoundingBox().intercepts(atMouse)
                                && !currSelection.contains(e)) {
                            canDeselect = false; // Don't deselect if something isn't selected at where they shift clicked
                        }
                    }
                    for (Entity e : currSelection.clone()) {
                        if (canDeselect && e.getBoundingBox().intercepts(atMouse)) {
                            deselected.add(e);
                        }
                    }
                }
                currSelection.deselectMultipleAndTrackStateOperation(deselected);
            }

            if (currSelection.isEmpty() || shift) {
                ArrayList<Entity> potentialClickSelection = new ArrayList<>();
                for (Entity e : atMouse.getInterceptingEntities()) {
                    if (e.getBoundingBox() != null
                            && e.getBoundingBox().intercepts(atMouse)
                            && !deselected.contains(e))
                        potentialClickSelection.add(e);
                }
                if (potentialClickSelection.size() > 0) {
                    selected.addAll(currSelection.selectMultipleAndTrackOperation(potentialClickSelection));
                } else
                    selectionBoxStartPoint = circuitPointAtMouse(false);
            }
            selectedSomethingLastPress = selected.size() > 0;

            if (!currSelection.isEmpty() && currSelection.intercepts(atMouse) && !shift) {
                onStartMovingSelection();
            }
        }

        String undoMsg;
        int numDes = deselected.size();
        int numSel = selected.size();
        String desMsg = "Deselect " + numDes + " entit" + (numDes > 1 ? "ies" : "y");
        String selMsg = "Select " + numSel + " entit" + (numSel > 1 ? "ies" : "y");

        // buff len > 0 should be implied, but just in case I put it here
        if ((numDes > 0 || numSel > 0) && c().stateController().getBufferLength() > 0) {
            if (numDes > 0 && numSel == 0)
                undoMsg = desMsg;
            else if (numDes == 0)
                undoMsg = selMsg;
            else
                undoMsg = desMsg + " and " + selMsg;
            c().appendCurrentStateChanges(undoMsg);
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
        }

        public void selectEntities() {
            Circuit c = c();
            Circuit.Selection currSelection = c.currentSelectionReference();
            EntityList<Entity> selected = currSelection.selectMultipleAndTrackOperation(this.getInterceptingEntities());
            if (selected.size() > 0)
                c().appendCurrentStateChanges("Select " + selected.size() + " entit" + (selected.size() == 1 ? "y" : "ies"));
        }
    }

    private boolean movingSelection = false;
    private CircuitPoint movingSelectionStartPoint;
    private int moveStateChange = 0;

    private void onStartMovingSelection() {
        movingSelection = true;
        moveStateChange = 0;
    }

    public void onGridSnapChangeWhileDraggingSelection() {
        System.out.println("GridSnapChangeWhileDraggingSelection");
    }

    private void onStopMovingSelection() {

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


    private void determineSelectingMouseRelease() {
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        System.out.println("mouse releese selecting " + currSelection.size());
        if (pressPointGrid.equals(releasePointGrid) && (currSelection.isEmpty() || shift) && !gridSnapChangedSinceLastPress) {
            if (!pressedOnSelectedEntity && !selectedSomethingLastPress) {
                determineSelecting();
            }
        }
        System.out.println(currSelection.isEmpty());
    }

    public void onPoke() {
        for (Entity e : c().getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    ((Pokable) e).onPoke();
            }
        }
        c().recalculateTransmissions();
    }

    public void onGridSnapChangeWhileDragging() {
        if (currentPullPoint != null) {
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
        Circuit.Selection currSelection = c.currentSelectionReference();
        Circuit.ConnectionSelection currConnectionView = c.currentConnectionViewReference();

        Platform.runLater(() -> {
            double width = canvasWidth();
            double height = canvasHeight();
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.rgb(240, 240, 240, 1));
            gc.fillRect(0, 0, width, height);

            gc.setFill(COL_BG);
            BoundingBox rangeBox = c.getInterceptMap().getBoundingBox().getExpandedBy(0.3);
            PanelDrawPoint tl = rangeBox.p1.toPanelDrawPoint();
            PanelDrawPoint tr = rangeBox.p2.toPanelDrawPoint();
            PanelDrawPoint bl = rangeBox.p3.toPanelDrawPoint();
            PanelDrawPoint br = rangeBox.p4.toPanelDrawPoint();

            gc.fillRect(tl.x, tl.y, tr.x - tl.x, br.y - tr.y);

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeLine(tl.x, tl.y, bl.x, bl.y); // Top left to bottom left
            gc.strokeLine(tl.x, tl.y, tr.x, tr.y); // Top left to top right
            gc.strokeLine(tr.x, tr.y, br.x, br.y); // Top right to bottom right
            gc.strokeLine(bl.x, bl.y, br.x, br.y); // Bottom left to bottom right

            gc.setStroke(Color.PINK);
            gc.setLineWidth(3);
            //      gc.strokeRect(0, 0, canvasWidth(), canvasHeight());

            drawGridPoints(gc);

            EntityList<Entity> drawOrder = new EntityList<>();
            EntityList<Entity> drawFirst = new EntityList<>();
            EntityList<Entity> drawSecond = new EntityList<>();
            for (Entity e : c.getAllEntities()) {
                if (!currConnectionView.contains(e)) {
                    if (e instanceof Wire)
                        drawFirst.add(e);
                    else
                        drawSecond.add(e);
                }
            }
            drawOrder.addAll(drawFirst);
            drawOrder.addAll(drawSecond);

            for (Entity e : drawOrder) {
                e.draw(gc);
            }


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

            c.drawInvalidEntities();
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

    public boolean undo(boolean updateAfter) {
        boolean shifted = false;
        if (canUserShiftState && ppStateShift == 0)
            shifted = c().stateController().goLeft();
        if (shifted && updateAfter)
            postStateChangeUpdate();
        return shifted;
    }

    public boolean redo(boolean updateAfter) {
        boolean shifted = false;
        if (canUserShiftState && ppStateShift == 0)
            shifted = c().stateController().goRight();
        if (shifted && updateAfter)
                postStateChangeUpdate();
        return shifted;
    }

    public void megaUndo(boolean updateAfter) {
        while (true)
            if (!undo(false))
                break;
        if (updateAfter)
            postStateChangeUpdate();
    }

    public void megaRedo(boolean updateAfter) {
        while (true)
            if (!redo(false))
                break;
        if (updateAfter)
            postStateChangeUpdate();
    }

    public void postStateChangeUpdate() {
        c().recalculateTransmissions();
        repaint(c());
        new PullPoint(circuitPointAtMouse(true));
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
            Circuit c = c();
            Circuit.Selection currSelection = c.currentSelectionReference();
            this.lock = lock;
            ppStateShift = 0;
            pressPoint = circuitPointAtMouse(true);
            PullPoint old = currentPullPoint;
            if (!canBePlacedHere(pressPoint) || !currSelection.isEmpty() || shift) {
                currentPullPoint = null;
                if (old != null)
                    repaint(c);
                return;
            }
            EntityList<ConnectibleEntity> cesAtStart = c.getEntitiesThatIntercept(pressPoint).thatExtend(ConnectibleEntity.class);
            pullDir = null;
            this.originalLoc = originalLoc;
            currentPullPoint = this;
            if (originalLoc.equals(pressPoint) && !lock)
                for (Wire w : c.getAllEntitiesOfType(Wire.class))
                    if (w.isEdgePoint(pressPoint))
                        canDelete = true;
            if (!canCreateFromAll(cesAtStart))
                deleteOnly = true;
            if (old == null)
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

            String undoMsg = null;

            // Re-update intercepting connectibles after the state went back
            EntityList<ConnectibleEntity> cesAtStart = c().getEntitiesThatIntercept(start).thatExtend(ConnectibleEntity.class);
            EntityList<ConnectibleEntity> cesAtEnd = c.getEntitiesThatIntercept(end).thatExtend(ConnectibleEntity.class);
            EntityList<Wire> startAndEndWires = cesAtStart.thatIntercept(end).thatExtend(Wire.class);
            if (startAndEndWires.size() > 1 && !start.isSimilar(end))
                throw new RuntimeException("Should not intercept 2 wires twice");
            Wire deleting = !end.isSimilar(start) && startAndEndWires.size() > 0 ? startAndEndWires.get(0) : null;

            if (canDelete && deleting != null && !lock) {
                // DO THE OPERATION FIRST SO IT CAN PROPERLY CHECK THE SPECIAL CASE WHERE THE DELETED WIRE CAUSES A BISECT
               if (new Wire(start, end).isSimilar(deleting)) {
                   c.removeSimilarEntityAndTrackOperation(new Wire(start.clone(), end.clone()));
                   undoMsg = "Delete Wire";
                    // TODO replace with c.deleteWithStateOperation
               } else {
                   c().new WireShortenOperation(deleting, start, end).operate();
                   undoMsg = "Shorten Wire";
               }
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
                    // Update pull dir if its null
                    if (pullDir == null) {
                        Vector dir = new Vector(start, end);
                        if (Vector.getDirectionVecs().contains(dir)) {
                            if (dir.equals(Vector.LEFT) || dir.equals(Vector.RIGHT))
                                pullDir = Direction.HORIZONTAL;
                            else
                                pullDir = Direction.VERTICAL;
                        }
                    }
                    WireGenerator generator = new WireGenerator(1);
                    ArrayList<TheoreticalWire> theos = generator.genWirePathLenient(start, end, pullDir, 8);
                    if (theos == null && pullDir != null) // If we couldn't do it in their preferred dir, try the other
                        theos = generator.genWirePathLenient(start, end, pullDir.getPerpendicular(), 8);
                    if (theos != null && !theos.isEmpty()) {
                        for (Wire t : theos) // 'new Wire' automatically adds it to the theoretical circuit
                            c.addEntityAndTrackOperation(new Wire(t.getStartLocation(), t.getEndLocation()));
                        undoMsg = theos.size() == 1 ? "Create Wire" : "Create " + theos.size() + " Wires";
                    }

                }
            }

            if (c.stateController().getBufferLength() > 0) {
                c.appendCurrentStateChanges(undoMsg);
                ppStateShift++;
            }

            c.recalculateTransmissions();
            repaint(c);
        }

        public void onRelease() {
            boolean repaint = ppStateShift != 0;
            ppStateShift = 0;
            currentPullPoint = null;
            if (repaint)
                repaint(c());
            c().stateController().clip();
        }

        boolean droppingAndRestarting;

        public void dropAndRestart() {
            if (ppStateShift > 0 && !shift) {
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

            for (int i = 0; i < 2; i++) {
                double strokeSize = (c().getLineWidth() * 0.9);
                double circleSize = (c().getScale() / 1.6);
                double bigCircleSize = (circleSize * 1.5);
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

}