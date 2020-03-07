package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.connectible.*;
import edu.wit.yeatesg.logicgates.entity.Pokable;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

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

    public void setListeners() {
        addEventFilter(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);
        addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
        addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMouseReleased);
        addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
        addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyPressed);
        addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyReleased);
    }

    public void onKeyPressed(KeyEvent e) {
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
        if (!canvas.isFocused())
            canvas.requestFocus();
        if (holdingSpace || e.isMiddleButtonDown()) {
            getCurrentCircuit().modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
            repaint();
        }

        if (selectionBoxStartPoint != null && middleClickedWhileSelectionBox) // Special case for when middle click is held
            onMouseDragWhileCreatingSelectionBox();

        updateMousePos(e);
        updatePossiblePullPoint();
    }

    public void onMouseClicked(MouseEvent e) {
        updateMousePos(e);
     /*   System.out.println("MOUSE CLICK:");
        System.out.println(" SCENE: " + e.getSceneX() + " " + e.getSceneY());
        System.out.println(" NORM: " + e.getX() + " " + e.getX());*/
    }

    private CircuitPoint middlePressPoint;
    private CircuitPoint middleReleasePoint;

    public void onMousePressed(MouseEvent e) {
        if (e.isMiddleButtonDown()) {
            middlePressPoint = circuitPointAtMouse(true);
            if (currSelectionBox != null)
                middleClickedWhileSelectionBox = true;
            middlePressedLastMousePress = true;
        }
        if (!middlePressedLastMousePress) {
            updateMousePos(e);
            middleClickedWhileSelectionBox = false;
            middlePressedLastMousePress = false;
            pressPointGrid = circuitPointAtMouse(true);
            pressedOnSelectedEntity = currSelection.intercepts(panelDrawPointAtMouse());
            determineIfPullingWire();
            determineSelecting();
            repaint();
        }

    }

    private boolean middleClickedWhileSelectionBox = true;

    boolean middlePressedLastMousePress = false;

    public void onMouseReleased(MouseEvent e) {
        updateMousePos(e);
        if (middlePressedLastMousePress) {
            // If we pressed middle click last mouse press
            middleReleasePoint = circuitPointAtMouse(true);
            if (middlePressPoint.equals(middleReleasePoint))
                onPoke();
            middleClickedWhileSelectionBox = false;
            middlePressedLastMousePress = false;
        } else {
            // Normal Mouse Release
            releasePointGrid = circuitPointAtMouse(true);
            selectionBoxStartPoint = null;
            determineSelectingMouseRelease();
            if (pullPoint != null)
                onStopPulling();
            if (currSelectionBox != null)
                onReleaseSelectionBox();
        }


        repaint();


    }

    public void onMouseDragged(MouseEvent e) {
        System.out.println("ASS");
        if (holdingSpace || e.isMiddleButtonDown()) {
            getCurrentCircuit().modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
            repaint();
        }
        updateMousePos(e);
        if (selectionBoxStartPoint != null)
            onMouseDragWhileCreatingSelectionBox();
        if (gridSnapChanged)
            onGridSnapChangeWhileDragging();
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

        public boolean intercepts(PanelDrawPoint p) {
            for (Entity e : this)
                if (e.getBoundingBox().intercepts(p, true))
                    return true;
            return false;
        }

        public boolean intercepts(CircuitPoint p) {
            return intercepts(p.toPanelDrawPoint());
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

    private Color hypotheticalPullColor = Color.ORANGE;

    private void drawPullableCircle(GraphicsContext g) {
        int strokeSize = (int) (getCurrentCircuit().getLineWidth() * 0.8);
        if (strokeSize % 2 == 0) strokeSize++;
        int circleSize = (int) (getCurrentCircuit().getScale() / 2.5);
        if (circleSize % 2 != 0) circleSize++;
        int bigCircleSize = (int) (circleSize * 1.5);
        if (bigCircleSize % 2 != 0) bigCircleSize++;

        PanelDrawPoint dp = pullPoint.toPanelDrawPoint();
        g.setLineWidth(strokeSize);
        g.setStroke(Color.BLACK);
        g.strokeOval(dp.x - bigCircleSize/2.00, dp.y - bigCircleSize/2.00, bigCircleSize, bigCircleSize);
        g.setStroke(hypotheticalPullColor);
        g.strokeOval(dp.x - circleSize/2.00, dp.y - circleSize/2.00, circleSize, circleSize);
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

    public void updatePossiblePullPoint() {
        if (!isPullingWire) {
            CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
            pullPoint = null;

            ArrayList<ConnectibleEntity> interceptingConnectibles = new ArrayList<>();
            for (ConnectibleEntity ce : getCurrentCircuit().getAllEntitiesOfType(ConnectibleEntity.class)) {
                if (ce.intercepts(gridSnapAtMouse)) {
                    if (ce instanceof Wire)
                        interceptingConnectibles.add(ce);
                    else {
                        for (ConnectionNode node : ce.getConnections())
                            if (node.getLocation().equals(gridSnapAtMouse))
                                interceptingConnectibles.add(ce);
                    }
                }
            }

            for (ConnectibleEntity ce : interceptingConnectibles)
                if (ce.canPullConnectionFrom(gridSnapAtMouse))
                    pullPoint = gridSnapAtMouse;

            if (pullPoint != null)
                for (ConnectibleEntity ce : interceptingConnectibles)
                    if (!ce.canPullConnectionFrom(gridSnapAtMouse))
                        pullPoint = null;

            for (Entity e : currSelection)
                if (e.intercepts(gridSnapAtMouse))
                    pullPoint = null;

            if (gridSnapChanged)
                repaint();
        }
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
        LogicGates.debug("Start Of DetermineSelecting() method", "", "CurrSelection", currSelection, "curr connection view", currConnectionView);
        selectionBoxStartPoint = null;
        selectedSomething = false;
        movingSelection = false;
        PanelDrawPoint atMouse = panelDrawPointAtMouse();
        if (isPullingWire) {
            currSelection.clear();
            currConnectionView.clear();
        } else {
            System.out.println("Not pullin wiah");
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
                } else { // Might not need the '!ctrl' boolean check
                    selectionBoxStartPoint = circuitPointAtMouse(false);
                }
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
             /*   System.out.println("DEPENDENCIES OF SELECTED: ");
                for (ConnectibleEntity ce : selectedConnectible.getDependencies())
                    System.out.println(ce);
                System.out.println("SUPER DEPENDENCIES OF SELECTED: ");
                for (ConnectibleEntity ce : selectedConnectible.getSuperDependencies())
                    System.out.println(ce);
                System.out.println("DEPENDENCIES OF INPUT NODES: ");
                for (InputNode in : selectedConnectible.getInputNodes())
                    for (Dependent o : in.getDependencyList())
                        System.out.println(in + " depends on " + o);
                System.out.println("SUPER DEPENDENCIES INPUT NODES: ");
                for (InputNode in : selectedConnectible.getInputNodes())
                    for (Dependent ce : in.getDependencyList())
                        System.out.println(in + " depends on " + ce);
                for (ConnectibleEntity ce : selectedConnectible.getSuperDependencies())
                    System.out.println(ce);*/
                currConnectionView.addAll(selectedConnectible.getConnectedEntities());
                currConnectionView.resetTimer();
            }
        }
        LogicGates.debug("End Of DetermineSelecting() method", "", "CurrSelection", currSelection, "curr connection view", currConnectionView);
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
                pullPoint = null;
                determineSelecting();
                updatePossiblePullPoint();
            }
        }
    }


    public CircuitPoint pressPointGrid;
    public CircuitPoint releasePointGrid;

    private CircuitPoint pullPoint = null;
    private boolean isPullingWire = false;
    private Direction pullDir = null;
    private boolean pressedOnEndpoint = false;
    private Wire wireBeingShortened = null;
    private Wire.TheoreticalWire theoreticalDeletion = null;
    private java.util.List<Wire.TheoreticalWire> theoreticalCreations = new ArrayList<>();

    public void determineIfPullingWire() {
        pressedOnEndpoint = false;
        if (pullPoint != null && !ctrl) {
            onStartPulling();
        }
    }

    private void onStartPulling() {
        isPullingWire = true;
        for (Wire w : getCurrentCircuit().getAllEntitiesOfType(Wire.class))
            if (w.isEdgePoint(pullPoint))
                pressedOnEndpoint = true;
    }

    private void onGridSnapChangeWhilePullingWire() {
        CircuitPoint gridAtMouse = circuitPointAtMouse(true);
        if (pullDir == null) { // If a preferred pull dir hasn't been chosen by the user yet, set it
            Vector dir = new Vector(pullPoint, gridAtMouse);
            if ((!dir.equals(Vector.ZERO_VECTOR)) && Vector.getDirectionVecs().contains(dir)) {
                System.out.println("b4 pull dir dir = " + dir);
                if (dir.equals(Vector.LEFT) || dir.equals(Vector.RIGHT))
                    pullDir = Direction.HORIZONTAL;
                else
                    pullDir = Direction.VERTICAL;
            }

        }

        // Whether or not the mouse pos is on the same entity that the pull point is on
        boolean isSameEntity = getCurrentCircuit().getAllEntities().thatInterceptAll(gridAtMouse, pullPoint).size() > 0;

        wireBeingShortened = null;
        if (pressedOnEndpoint  // Check to see if they are deleting wire
                && pullDir != null
                && !gridAtMouse.equals(pullPoint))
            for (Wire w : getCurrentCircuit().getAllEntitiesOfType(Wire.class).thatInterceptAll(pullPoint, gridAtMouse))
                if (w.isEdgePoint(pullPoint))
                    wireBeingShortened = w;

        if (wireBeingShortened == null // If they move the pullPoint to another spot by mousing over an adjacent spot
                && pullDir != null // that is on the same entity that they originally pulled from
                && !gridAtMouse.equals(pullPoint)
                && pullPoint.is4AdjacentTo(gridAtMouse)
                && (isSameEntity)) {
            pullPoint = gridAtMouse;
            pullDir = null;
            theoreticalCreations = new ArrayList<>();
        }

        if (wireBeingShortened == null)
            theoreticalDeletion = null;

        if (wireBeingShortened != null && !gridAtMouse.equals(pullPoint)) {
            theoreticalCreations = new ArrayList<>();
            theoreticalDeletion = new Wire.TheoreticalWire(pullPoint, gridAtMouse);
        }

        // If they aren't shortening wire and the mouse isn't on the same entity, display theoretical creations
        if (wireBeingShortened == null && !isSameEntity) {
            theoreticalCreations = Wire.genWirePathLenient(pullPoint, gridAtMouse, pullDir, 8);
            if (theoreticalCreations == null && pullDir != null) // If we couldn't do it in their preferred dir, try the other
                theoreticalCreations = Wire.genWirePathLenient(pullPoint, gridAtMouse, pullDir.getPerpendicular(), 8);
            theoreticalCreations = theoreticalCreations == null ? new ArrayList<>() : theoreticalCreations;
        } else if (wireBeingShortened == null) {
            theoreticalCreations.clear();
        }

        // If they went back to pull point with mouse after giving it an initial vec
        if (gridAtMouse.equals(pullPoint) && wireBeingShortened == null && pullDir != null) {
            pullDir = null;
            theoreticalCreations = new ArrayList<>();
        }
        repaint();
    }

    /**
     * Theoretical wires are drawn before (under) wires, and since we need theoretical junctions to be drawn above
     * wires, we need to keep track of them with this list so they can be drawn after.
     * @see EditorPanel#drawTheoreticalWires(boolean, GraphicsContext)
     */
    ArrayList<CircuitPoint> theoJuncsToDrawAfterWires = new ArrayList<>();

    private void drawTheoreticalWires(boolean calledBeforeDrawWires, GraphicsContext g) {
        ArrayList<Wire> allWires = getCurrentCircuit().getAllEntitiesOfType(Wire.class);
        if (calledBeforeDrawWires) {
            theoJuncsToDrawAfterWires = new ArrayList<>();
            for (Wire.TheoreticalWire t: theoreticalCreations) {
                t.draw(g);
                for (Wire w : allWires) {
                    for (CircuitPoint tEdgePoint : new CircuitPoint[] {t.getStartLocation(), t.getEndLocation()})
                        if (w.getPointsExcludingEdgePoints().intercepts(tEdgePoint)
                                || (w.isEdgePoint(tEdgePoint) && w.getNumEntitiesConnectedAt(tEdgePoint) > 0)
                                && w.getNumEntitiesConnectedAt(tEdgePoint) < 2)
                            theoJuncsToDrawAfterWires.add(tEdgePoint);
                    for (CircuitPoint wEdgePoint : new CircuitPoint[] {w.getStartLocation(), w.getEndLocation()}) {
                        if (t.getPointsExcludingEdgePoints().intercepts(wEdgePoint)
                                || (t.isEdgePoint(wEdgePoint) && t.getNumEntitiesConnectedAt(wEdgePoint) > 0)
                                && t.getNumEntitiesConnectedAt(wEdgePoint) < 2)
                            theoJuncsToDrawAfterWires.add(wEdgePoint);
                    }
                }
            }
        } else {
            Wire.TheoreticalWire toDrawJuncs = new Wire.TheoreticalWire(new CircuitPoint(0, 0, getCurrentCircuit()),
                    new CircuitPoint(0, 1, getCurrentCircuit()));
            for (CircuitPoint p : theoJuncsToDrawAfterWires)
                toDrawJuncs.drawJunction(g, p);
        }

    }

    private Color deletionColor = Color.RED;

    private void drawCurrentDeletion(GraphicsContext g) {
        if (theoreticalDeletion == null)
            throw new RuntimeException("Can't draw current deletion if there is none. Learn to code");

        CircuitPoint theoStart = theoreticalDeletion.getStartLocation();
        CircuitPoint theoEnd = theoreticalDeletion.getEndLocation();

        // First, white out any wire junctions at the end of the deletion
        // then draw the wire junctions that need to be added back
        for (CircuitPoint edgePoint : new CircuitPoint[] { theoStart, theoEnd }) {
            if (wireBeingShortened.isEdgePoint(edgePoint)) {
                theoreticalDeletion.setColor(backgroundColor);
                theoreticalDeletion.drawJunction(g, edgePoint);
                int numConnects = wireBeingShortened.getNumWiresConnectedAt(edgePoint);
                for (Wire w : wireBeingShortened.getWiresConnectedAt(edgePoint))
                    w.draw(g, numConnects == 3);
            }
        }

        // Next, draw the red deletion indication wire
        theoreticalDeletion.setColor(deletionColor);
        theoreticalDeletion.draw(g, false);

        // Next, overlap any possible white spaces with the original intercepting wires (don't draw the junctions tho)
        for (CircuitPoint edgePoint : new CircuitPoint[] { theoStart, theoEnd })
            if (wireBeingShortened.isEdgePoint(edgePoint))
                for (Wire w : wireBeingShortened.getWiresConnectedAt(edgePoint))
                    w.draw(g, false);

        // Draw junctions back
        for (Entity e : getCurrentCircuit().getAllEntities().thatIntercept(theoEnd))
            if (e instanceof Wire && ((Wire) e).getDirection() != wireBeingShortened.getDirection()
                    && ((Wire) e).getNumEntitiesConnectedAt(theoEnd) == 0)
                ((Wire) e).drawJunction(g, theoEnd);
    }

    private void onStopPulling() {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        if (wireBeingShortened != null) {
            if (wireBeingShortened.intercepts(gridSnapAtMouse)) {
                wireBeingShortened.set(pullPoint, gridSnapAtMouse);
            }
            wireBeingShortened = null;
        }

        if (theoreticalCreations != null && !theoreticalCreations.isEmpty()) {
            for (Wire.TheoreticalWire w : theoreticalCreations)
                new Wire(w.getStartLocation(), w.getEndLocation());
            theoreticalCreations = new ArrayList<>();
        }

        pressedOnEndpoint = false;
        wireBeingShortened = null;
        isPullingWire = false;
        pullPoint = null;
        pullDir = null;
        theoreticalDeletion = null;
        updatePossiblePullPoint();
    }

    public void onPoke() {
        System.out.println("POKE");
        for (Entity e : getCurrentCircuit().getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    ((Pokable) e).onPoke();
            }
        }
        getCurrentCircuit().refreshTransmissions();
    }


    public void onGridSnapChangeWhileDragging() {
        if (isPullingWire)
            onGridSnapChangeWhilePullingWire();
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

            try {
                CircuitPoint mouseSnap = circuitPointAtMouse(true);

                gc.setStroke(backgroundColor);
                gc.fillRect(0, 0, getWidth(), getHeight());

                drawGridPoints(gc);

                drawTheoreticalWires(true, gc);

                for (Entity e : getCurrentCircuit().getAllEntities()) {
                    if (!currConnectionView.contains(e))
                        e.draw(gc);
                }
                drawTheoreticalWires(false, gc);

                if (theoreticalDeletion != null)
                    drawCurrentDeletion(gc);

                for (Entity e : currConnectionView) {
                    e.getBoundingBox().drawBorder(gc);
                    currConnectionView.draw(e, gc);
                }

                for (Entity e : currSelection)
                    e.getBoundingBox().paint(gc);

                if (pullPoint != null && (!isPullingWire || mouseSnap.equals(pullPoint)))
                    drawPullableCircle(gc);

                if (currSelectionBox != null)
                    currSelectionBox.paint(gc);

            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        });
    }

}