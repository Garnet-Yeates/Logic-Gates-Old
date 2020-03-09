package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.*;
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

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

import static edu.wit.yeatesg.logicgates.entity.connectible.Wire.*;

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

    private Circuit theoreticalCircuit;

    public Circuit getTheoreticalCircuit() {
        return theoreticalCircuit;
    }

    public void setTheoreticalCircuit(Circuit theo) {
        theoreticalCircuit = theo;
    }

    public boolean hasTheoreticalCircuit() {
        return theoreticalCircuit != null;
    }


    public GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    public EntityList<Entity> getScreenScope() {
        BoundingBox b = new BoundingBox(panelDrawPointAt(0, 0), panelDrawPointAt(canvasWidth(), canvasHeight()), null);
        b.getExpandedBy(8);
        return b.getInterceptingEntities();
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

    public void modifyOffset(Vector off) {
        getCurrentCircuit().modifyOffset(off);
    }

    public void onKeyPressed(KeyEvent e) {
        if (e.getCode() == R)
            repaint();
        if (e.isControlDown() && (e.getCode() == EQUALS || e.getCode() == MINUS)) {
            CircuitPoint oldCenter = getCenter();
            if (e.getCode() == EQUALS && getCurrentCircuit().canScaleUp()) {
                getCurrentCircuit().scaleUp();
                view(oldCenter);
            } else if (e.getCode() == MINUS && getCurrentCircuit().canScaleDown()) {
                getCurrentCircuit().scaleDown();
                view(oldCenter);
            }
        }
        if (e.getCode() == SPACE)
            holdingSpace = true;
        if (e.getCode() == CONTROL)
            ctrl = true;
        if (e.getCode() == ESCAPE) {
            currSelection.clear();
            currConnectionView.clear();
        }
        if (e.getCode() == P)
            onPoke();
        repaint();
    }

    public void onKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE)
            holdingSpace = false;
        if (e.getCode() == KeyCode.CONTROL)
            ctrl = false;
    }

    public void onMouseMoved(MouseEvent e) {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        this.new PullPoint(gridSnapAtMouse, gridSnapAtMouse);
        if (!canvas.isFocused())
            canvas.requestFocus();
        if (holdingSpace)
            modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
        updateMousePos(e);
        updatePossiblePullPoint();
        repaint();
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
        System.out.println("MOUSE PRESS " + e.getButton());
        updateMousePos(e);
        if (e.getButton() == MouseButton.MIDDLE) {
            // Middle Click Processing
            draggedViaMiddlePress = false; // Reset this field
            middlePressPointGrid = circuitPointAtMouse(true);
        } else if (e.getButton() == MouseButton.PRIMARY) {
            // Normal Left Click Processing
            pressPointGrid = circuitPointAtMouse(true);
            pressedOnSelectedEntity = currSelection.intercepts(panelDrawPointAtMouse());
            this.new PullPoint(pressPointGrid, pressPointGrid);
            determineSelecting();
            repaint();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            rightPressPointGrid = circuitPointAtMouse(true);
            if (currentPullPoint != null)
                currentPullPoint.dropAndRestart();
        }
    }

    public void onMouseReleased(MouseEvent e) {
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
                currentPullPoint.onRelease(false);
            determineSelectingMouseRelease();
            this.new PullPoint(circuitPointAtMouse(true), circuitPointAtMouse(true));
            if (currSelectionBox != null)
                onReleaseSelectionBox();
            pressPointGrid = null;
        } else if (e.getButton() == MouseButton.SECONDARY) {
            rightReleasePointGrid = circuitPointAtMouse(true);
            rightPressPointGrid = null;
        }


        repaint();


    }

    public void onMouseDragged(MouseEvent e) {
        if (middleDown() || holdingSpace) {
            draggedViaMiddlePress = true;
            modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
        }
        updateMousePos(e);
        if (leftDown()) {
            if (selectionBoxStartPoint != null)
                onMouseDragWhileCreatingSelectionBox();
            if (gridSnapChanged)
                onGridSnapChangeWhileDragging();
        }

    }

    public Circuit getCurrentCircuit() {
        return currProject.getCurrentCircuit();
    }

    public Selection currSelection = new Selection();
    public ConnectionSelection currConnectionView = new ConnectionSelection();

    public static class Selection extends ArrayList<Entity> {

        public Selection(Entity... entities) {
            if (entities != null)
                addAll(Arrays.asList(entities));
        }

        public boolean intercepts(CircuitPoint p) {
            for (Entity e : this)
                if (e.getBoundingBox().intercepts(p, true))
                    return true;
            return false;
        }

        public boolean intercepts(PanelDrawPoint p) {
            return intercepts(p.toCircuitPoint());
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

    public Color backgroundColor = Color.WHITE;

    private void drawGridPoints(GraphicsContext g) {
        int w = canvasWidth(), h = canvasHeight();
        for (int x = (int)(-w*0.1); x < w + w*0.1; x += getCurrentCircuit().getScale()) {
            for (int y = (int)(-h*0.1); y < h + h*0.1; y += getCurrentCircuit().getScale()) {
                CircuitPoint gridPoint = circuitPointAt(x, y, true);
                PanelDrawPoint drawLoc = gridPoint.toPanelDrawPoint();
                g.setLineWidth(getCurrentCircuit().getGridLineWidth());

                g.setStroke(Circuit.COL_GRID);
                if (gridPoint.representsOrigin()) {
                    int strokeSize = getCurrentCircuit().getLineWidth();
                    strokeSize *= 1.5;
                    if (strokeSize == getCurrentCircuit().getLineWidth())
                        strokeSize++;
                    g.setLineWidth(strokeSize);
                    g.setStroke(Circuit.COL_ORIGIN);
                }
                g.strokeLine(drawLoc.x, drawLoc.y, drawLoc.x, drawLoc.y);
            }
        }
    }


    public void viewOrigin() {
        getCurrentCircuit().setXOffset(canvasWidth() / 2);
        getCurrentCircuit().setYOffset(canvasHeight() / 2);
    }

    public void view(CircuitPoint location) {
        viewOrigin();
        PanelDrawPoint viewingLoc = location.toPanelDrawPoint();
        PanelDrawPoint originLoc = new CircuitPoint(0, 0, getCurrentCircuit()).toPanelDrawPoint();
        getCurrentCircuit().modifyOffset(new Vector(viewingLoc, originLoc));
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
        return new PanelDrawPoint(x, y, getCurrentCircuit());
    }

    public CircuitPoint circuitPointAt(int x, int y, boolean gridSnap) {
        CircuitPoint cp = new PanelDrawPoint(x, y, getCurrentCircuit()).toCircuitPoint();
        return gridSnap ? cp.getGridSnapped() : cp;
    }


    private boolean movingSelection = false;
    private boolean ctrl = false;
    private CircuitPoint selectionBoxStartPoint;

    @SuppressWarnings("unchecked")
    public void determineSelecting() {
        selectionBoxStartPoint = null;
        selectedSomething = false;
        movingSelection = false;
        CircuitPoint atMouse = circuitPointAtMouse(false);
        if (currentPullPoint != null) {
            currSelection.clear();
            currConnectionView.clear();
        } else {
            if (!currSelection.isEmpty() && !currSelection.intercepts(atMouse) && !ctrl) {
                currSelection.clear();
                currConnectionView.clear();
            }
            ArrayList<Entity> deselected = new ArrayList<>();
            if (!currSelection.isEmpty() && currSelection.intercepts(atMouse)) {
                if (ctrl) {
                    boolean canDeselect = true;
                    for (Entity e : getCurrentCircuit().getAllEntities()) {
                        if (e.getBoundingBox() != null
                                && e.getBoundingBox().intercepts(atMouse, true)
                                && !currSelection.contains(e)) {
                            canDeselect = false; // Don't deselect if something isn't selected at where they ctrl clicked
                        }
                    }
                    for (Entity e : (ArrayList<Entity>) currSelection.clone()) {
                        if (canDeselect && e.getBoundingBox().intercepts(atMouse, true)) {
                            currSelection.remove(e);
                            deselected.add(e);
                        }
                    }
                }
            }
            if (currSelection.isEmpty() || ctrl) {
                ArrayList<Entity> potentialClickSelection = new ArrayList<>();
                for (Entity e : getCurrentCircuit().getAllEntities())
                    if (e.getBoundingBox() != null
                            && e.getBoundingBox().intercepts(atMouse, true)
                            && !deselected.contains(e))
                        potentialClickSelection.add(e);
                if (potentialClickSelection.size() > 0) {
                    for (Entity e : potentialClickSelection) {
                        if (!currSelection.contains(e)) {
                            currSelection.add(e);
                            selectedSomething = true;
                        }
                    }
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
            for (Entity e : this.getInterceptingEntities())
                if (!currSelection.contains(e))
                    currSelection.add(e);
        }
    }

    public void onGridSnapChangeWhileDraggingSelection() {
        System.out.println("GridSnapChangeWhileDraggingSelection");
    }

    private SelectionBox currSelectionBox = null;

    public void onMouseDragWhileCreatingSelectionBox() {
        currSelectionBox = new SelectionBox(selectionBoxStartPoint, circuitPointAtMouse(false));
        repaint();
    }

    public void onReleaseSelectionBox() {
        currSelectionBox.selectEntities();
        currSelectionBox = null;
    }

    boolean pressedOnSelectedEntity = false;
    boolean selectedSomething = false;

    private void determineSelectingMouseRelease() {
        if (pressPointGrid.equals(releasePointGrid) && (currSelection.isEmpty() || ctrl)) {
            if (!pressedOnSelectedEntity && !selectedSomething) {
                determineSelecting();
                updatePossiblePullPoint();
            }
        }
    }

    private void updatePossiblePullPoint() {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        this.new PullPoint(gridSnapAtMouse, gridSnapAtMouse);
    }


    // Mouse press: initialize pull point. it is only calculate on mouse press
    // Mouse release,, nullify pull point. therefore, inner class

    // Called on mouse press, or in special cases when you right click while pulling wire

    private PullPoint currentPullPoint;

    private enum PullPointState { DELETE_ONLY, DELETE_ONLY_BUT_CANT, CREATE_DELETE, CREATE }

    class PullPoint extends CircuitPoint {

        private CircuitPoint originalLoc;

        private boolean lock;

        private CircuitPoint pressPoint;
        private Direction pullDir;

        private boolean deleteOnly;

        private boolean canDelete;

        private EntityList<ConnectibleEntity> interceptingCes;

        public PullPoint(CircuitPoint location, CircuitPoint originalLoc) {
            super(location.x, location.y, originalLoc.getCircuit());
            pressPoint = circuitPointAtMouse(true);
            interceptingCes = getCurrentCircuit().getAllEntitiesOfType(ConnectibleEntity.class).thatIntercept(pressPoint);
            pullDir = null;
            this.originalLoc = originalLoc;
            if (!isPullableLocation(pressPoint) || currSelection.size() > 0) {
                currentPullPoint = null;
                repaint();
                return;
            }

            currentPullPoint = this;
            if (originalLoc.equals(pressPoint))
                for (Wire w : getCurrentCircuit().getAllEntitiesOfType(Wire.class))
                    if (w.isEdgePoint(pressPoint))
                        canDelete = true;
            if (!canCreateFromAll(interceptingCes))
                deleteOnly = true;
            repaint();
        }

        public boolean isPullableLocation(CircuitPoint location) {
            for (ConnectibleEntity ce : getCurrentCircuit().getAllEntitiesOfType(ConnectibleEntity.class))
                if (ce.isPullableLocation(location))
                    return true;
            return false;
        }

        public boolean canCreateFromAll(EntityList<ConnectibleEntity> list) {
            if (list.isEmpty())
                return false;
            for (ConnectibleEntity e : list)
                if (!e.canPullConnectionFrom(pressPoint))
                    return false;
            return true;
        }

        public void onDragGridSnapChange() {
            boolean deletionThread = false;
            boolean genThread = false;
            CircuitPoint potentialReleasePoint = circuitPointAtMouse(true);
            EntityList<Wire> thatInterceptStartAndRelease = interceptingCes.thatIntercept(potentialReleasePoint).ofType(Wire.class);
            Wire deleting = !potentialReleasePoint.equals(pressPoint) && thatInterceptStartAndRelease.size() > 0
                    ? thatInterceptStartAndRelease.get(0) : null;
            if (canDelete && deleting != null) {
                deletionThread = true;
            } else {
                EntityList<ConnectibleEntity> entitiesAtRelease = getCurrentCircuit().getAllEntitiesOfType(ConnectibleEntity.class)
                        .thatIntercept(potentialReleasePoint);
                ConnectibleEntity entityAtRelease = entitiesAtRelease.size() > 0 ? entitiesAtRelease.get(0) : null;

                if (!lock && (potentialReleasePoint.equals(pressPoint) // Handle the case of them sliding the pull
                        || (potentialReleasePoint.is4AdjacentTo(pressPoint) // point to an adjacent location, in which
                            && isPullableLocation(potentialReleasePoint) // a new PullPoint is assigned to the
                            && (interceptingCes.get(0).hasConnectionTo(entityAtRelease)) // EditorPanel
                                || interceptingCes.get(0).equals(entityAtRelease)))) {
                    EditorPanel.this.new PullPoint(potentialReleasePoint, originalLoc);
                }
                else if (!potentialReleasePoint.equals(pressPoint)
                        && interceptingCes.intersection(entitiesAtRelease).size() == 0) {
                    if (pullDir == null) {
                        Vector dir = new Vector(pressPoint, potentialReleasePoint);
                        if (Vector.getDirectionVecs().contains(dir)) {
                            if (dir.equals(Vector.LEFT) || dir.equals(Vector.RIGHT))
                                pullDir = Direction.HORIZONTAL;
                            else
                                pullDir = Direction.VERTICAL;
                        }
                    }
                    genThread = true;
                }
            }

            if ( (canDelete && deleting != null && deletionThread) || (genThread && !deleteOnly)) {
                Thread t = new PullPreviewThread(genThread ? PullPreviewType.CREATION : PullPreviewType.DELETION,
                        pressPoint.clone(), potentialReleasePoint.clone(), pullDir);
                t.start();
            }
        }


        public void onRelease(boolean calculateNextPullPoint) {
            if (getTheoreticalCircuit() != null) { // If we started dragging at all
                if (!releventPullPreviewThread.isComplete) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) { }
                }
                currProject.getCurrentCircuit().getEntityList(false).clear();
                currProject.getCurrentCircuit().deepCloneEntitiesFrom(theoreticalCircuit);
                setTheoreticalCircuit(null);
                currentPullPoint = null;
                if (calculateNextPullPoint)
                    updatePossiblePullPoint();
                repaint();
            }
        }

        public void dropAndRestart() {
            onRelease(true);
            EditorPanel.this.new PullPoint(circuitPointAtMouse(true), circuitPointAtMouse(true));
            if (currentPullPoint != null)
                currentPullPoint.lock = true;
        }

        private PullPointState getState() {
            if (deleteOnly && canDelete) {
                return PullPointState.DELETE_ONLY;
            }
            else if (deleteOnly) {
                return PullPointState.DELETE_ONLY_BUT_CANT;
            }
            else if (canDelete) { // Delete only is def false here, can delete is true
                return PullPointState.CREATE_DELETE;
            }
            else { // Delete only false, can delete false
                return PullPointState.CREATE;
            }
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
            if (col != null) {
                int strokeSize = (int) (getCurrentCircuit().getLineWidth() * 0.8);
                if (strokeSize % 2 == 0) strokeSize++;
                int circleSize = (int) (getCurrentCircuit().getScale() / 2.5);
                if (circleSize % 2 != 0) circleSize++;
                int bigCircleSize = (int) (circleSize * 1.5);
                if (bigCircleSize % 2 != 0) bigCircleSize++;
                PanelDrawPoint dp = currentPullPoint.toPanelDrawPoint();
                g.setLineWidth(strokeSize);
                g.setStroke(Color.BLACK);
                g.strokeOval(dp.x - bigCircleSize/2.00, dp.y - bigCircleSize/2.00, bigCircleSize, bigCircleSize);
                g.setStroke(col);
                g.strokeOval(dp.x - circleSize/2.00, dp.y - circleSize/2.00, circleSize, circleSize);
            }
        }

        private Color deletionColor = Color.RED;


        /**
         * Theoretical wires are drawn before (under) wires, and since we need theoretical junctions to be drawn above
         * wires, we need to keep track of them with this list so they can be drawn after.
         */
        ArrayList<CircuitPoint> theoJuncsToDrawAfterWires = new ArrayList<>();
    }

    private PullPreviewThread releventPullPreviewThread;

    enum PullPreviewType { CREATION, DELETION }

    private class PullPreviewThread extends Thread {

        private boolean isComplete = false;

        private CircuitPoint start;
        private CircuitPoint end;

        private PullPreviewType type;
        private Direction pullDir;

        public PullPreviewThread(PullPreviewType type, CircuitPoint start, CircuitPoint end, Direction pullDir) {
            releventPullPreviewThread = this;
            this.start = start;
            this.end = end;
            this.type = type;
            this.pullDir = pullDir;
        }

        @Override
        public void run() {
            System.out.println("better run better run");
            Circuit circuitForThread = getCurrentCircuit().cloneOntoProject("pull theoretical");
            circuitForThread.deepCloneEntitiesFrom(getCurrentCircuit());
            start = start.clone(circuitForThread);
            end = end.clone(circuitForThread);
            if (type == PullPreviewType.CREATION) {
                ArrayList<TheoreticalWire> theos = new ArrayList<>();
                theos = Wire.genWirePathLenient(start, end, pullDir, 8);
                if (theos == null && pullDir != null) // If we couldn't do it in their preferred dir, try the other
                    theos = Wire.genWirePathLenient(start, end, pullDir.getPerpendicular(), 8);
                theos = theos == null ? new ArrayList<>() : theos;
                for (Wire w : theos) { // 'new Wire' automatically adds it to the theoretical circuit
                    CircuitPoint theoStart = w.getStartLocation().clone(circuitForThread);
                    CircuitPoint theoEnd = w.getEndLocation().clone(circuitForThread);
                    Wire added = new Wire(theoStart, theoEnd);
                }
            } else {
                Wire toModify = circuitForThread.getAllEntitiesOfType(Wire.class).thatInterceptAll(start, end).get(0);
                toModify.set(start, end);
            }

            if (this == releventPullPreviewThread) {
                EditorPanel.this.setTheoreticalCircuit(circuitForThread);
                repaint();
                isComplete = true;
            }
        }
    }


    public void onPoke() {
        for (Entity e : getCurrentCircuit().getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    ((Pokable) e).onPoke();
            }
        }
        getCurrentCircuit().refreshTransmissions();
    }


    public void onGridSnapChangeWhileDragging() {
        if (currentPullPoint != null)
            currentPullPoint.onDragGridSnapChange();
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
            repaint();
        }
    }

    public int canvasWidth() {
        return (int) canvas.getWidth();
    }

    public int canvasHeight() {
        return (int) canvas.getHeight();
    }


    public void repaint() {
        Project p = currProject;
        Circuit c = hasTheoreticalCircuit() ? theoreticalCircuit : p.getCurrentCircuit();
        if (c == theoreticalCircuit) {
            c.setOffset(p.getCurrentCircuit().getOffset());
            c.setScale((int) p.getCurrentCircuit().getScale());
        }
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

}