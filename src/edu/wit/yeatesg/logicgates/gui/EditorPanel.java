package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.InputBlock;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.circuit.entity.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
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
import static edu.wit.yeatesg.logicgates.circuit.Circuit.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire.*;

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
        return new BoundingBox(new PanelDrawPoint(0, 0, c()).toCircuitPoint(),
                new PanelDrawPoint(canvasWidth(), canvasHeight(), c()).toCircuitPoint(), null).getExpandedBy(17);
    }

    public void modifyOffset(Vector off) {
        c().modifyOffset(off);
    }

    CircuitPoint LL = null;
    CircuitPoint RR = null;

    public boolean debugInv = false;

    public Entity lastFuckedUpOperand;
    private int fuckedDrawState = -1;

    private Timer blinkTimer = new Timer(500, (e -> { fuckedDrawState *= -1; repaint(); }));
    {
        blinkTimer.start();
    }

    private void onKeyPressDetermineSelectionModifications(KeyEvent event) {
        KeyCode co = event.getCode();
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        if (co == S // Displays whats in selection
                || co == R // Rotates selection
                || co == LEFT || co == RIGHT || co == DOWN || co == UP  // Moves selection
                || co == D && event.isControlDown() // Duplicates selection
                || co == ESCAPE) { // Deselects
            if (co == ESCAPE) {
                if (placingAtCursor != null)
                    placingAtCursor.cancel();
                else if (currSelection.size() > 0) {
                    if (movingSelection)
                        cancelMovingSelection();
                    else {
                        currSelection.deselectAllAndTrack();
                        c.appendCurrentStateChanges("Deselect All Via Esc");
                    }
                }
            } else if (placingAtCursor == null && !movingSelection && !currSelection.isEmpty()) { // If we have entities on the cursor, don't do any of this below

                if (co == S) {
                    System.out.println("SELECTION:");
                    for (Entity en : currSelection)
                        System.out.println(" " + en.toParsableString());
                }
                else if (co == R) {
                    for (Entity selected : currSelection.clone()) {
                        if (selected.hasProperty("facing")) {
                            System.out.println(selected.getPropertyValue("facing"));
                            int rotation = Direction.rotationFromCardinal(selected.getPropertyValue("facing"));
                            int nextRotation = Integer.parseInt(Rotatable.getNextRotation(rotation) + "");
                            c.new PropertyChangeOperation(selected, "facing", Direction.cardinalFromRotation(nextRotation), true).operate();
                        }
                    }
                    c.appendCurrentStateChanges("Rotate " + currSelection.size() + " Entit" + (currSelection.size() == 1 ? "y" : "ies" ) + " 90 degrees");
                }
                else if (co == LEFT || co == RIGHT || co == DOWN || co == UP) {
                    Vector vec;
                    if ((vec = Vector.directionVectorFrom(co.getName())) != null)
                        userMoveSelection(vec, co.getName());
                }
                else /* if co == D and ctrl held */ {
                    duplicateSelection();
                }

            }
        }
    }


    public void onKeyPressed(KeyEvent e) {
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        KeyCode code = e.getCode();
        onKeyPressDetermineSelectionModifications(e);

        if (code == OPEN_BRACKET)
            autoPokeTimer.setDelay(autoPokeTimer.getDelay() + 50);
        if (code == CLOSE_BRACKET)
            if (autoPokeTimer.getDelay() >= 100)
                autoPokeTimer.setDelay(autoPokeTimer.getDelay() - 50);
        if (code == BACK_SLASH);
          //  c.getOperands(new Wire(LL, RR));
        if (code == DELETE)
            currSelection.deleteAllEntitiesAndTrack();
        if (code == B)
            if (currentPullPoint != null)
                currentPullPoint.dropAndRestart();
        if (code == N) {
            System.out.println("NUM ENTITIES: " + c().getNumEntities());
        }
        if (code == F) {
            c.bridgeWires(circuitPointAtMouse(true));
        }
        if (e.isControlDown() && e.getCode() == Z) {
            if (e.isShiftDown())
                megaUndo(true);
            else
                undo(true);
        }
        if (e.isControlDown() && e.getCode() == Y ) {
            if (e.isShiftDown())
                megaRedo(true);
            else
                redo(true);
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
            if (ppStateShift > 0) {
                c().stateController().goLeft(); // Undo() wont work
                c().stateController().clip();
                currentPullPoint = null;
                this.new PullPoint(circuitPointAtMouse(true));
            }
        }
        if (code == P) {
            onPoke();
            repaint(c);
        }
        if (code == G) {
            toggleDrawGridPoints();
        }
        if (code == N) {
            CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
            int negatedIndex = -1;
            for (Entity intercepting : gridSnapAtMouse.getInterceptingEntities()) {
                if (intercepting instanceof OutputNegatable &&
                        (negatedIndex = ((OutputNegatable) intercepting).indexOfOutput(gridSnapAtMouse)) != -1) {
                    ArrayList<Integer> negatedIndices = new ArrayList<>();
                    negatedIndices.add(negatedIndex);
                    c.new EntityNegateOperation(intercepting, false, negatedIndices, intercepting.isSelected() ? SELECTED : NON_SELECTED, true).operate();
                    break;
                }
                if (intercepting instanceof InputNegatable)
                System.out.println("INDEX OF INPUT = " + ((InputNegatable) intercepting).indexOfInput(gridSnapAtMouse));
                if (intercepting instanceof InputNegatable &&
                        (negatedIndex = ((InputNegatable) intercepting).indexOfInput(gridSnapAtMouse)) != -1) {
                    ArrayList<Integer> negatedIndices = new ArrayList<>();
                    negatedIndices.add(negatedIndex);
                    c.new EntityNegateOperation(intercepting, true, negatedIndices, intercepting.isSelected() ? SELECTED : NON_SELECTED, true).operate();
                    break;
                }
            }
            if (negatedIndex != -1)
                c.appendCurrentStateChanges("Negate ConnectionNode at " + gridSnapAtMouse.toParsableString());
        }
        e.consume();
        repaint(c());
        if (code != SPACE)
            c.recalculateTransmissions();
        currSelection.updateConnectionView();
    }

    public void userMoveSelection(Vector vec, String dir) {
        Circuit c = c();
        BoundingBox oldLocationBox = new BoundingBox(c.currSelection);
        BoundingBox newLocationBox = new BoundingBox(oldLocationBox.p1.getIfModifiedBy(vec),
                oldLocationBox.p4.getIfModifiedBy(vec), null);
        if (newLocationBox.isInMapRange()) {
            Circuit.Selection currSelection = c.currentSelectionReference();
            c.new SelectionMoveOperation(vec, true).operate();
            // Move Operations are different; they don't call circuit.clrarnontrans
            c.appendCurrentStateChanges("Move " + currSelection.size() + " Entit"
                    + (currSelection.size() == 1 ? "y" : "ies") + " " + dir);
        }
    }

    public void duplicateSelection() {
        this.new PlaceableEntityList(c().currSelection);
        placingAtCursor.placePreview(circuitPointAtMouse(true));
    }

    public void onKeyReleased(KeyEvent e) {
        if (e.getCode() == KeyCode.SPACE)
            holdingSpace = false;
        if (e.getCode() == SHIFT)
            shift = false;
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        currSelection.updateConnectionView();
    }

    public void onMouseMoved(MouseEvent e) {
  //      CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
  //      this.new PullPoint(gridSnapAtMouse);
        if (!canvas.isFocused())
            canvas.requestFocus();
        if (holdingSpace)
            modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
        updateMousePos(e);
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        this.new PullPoint(gridSnapAtMouse);
        if (gridSnapJustChanged && placingAtCursor != null)
            placingAtCursor.onGridSnapChange();
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
            if (placingAtCursor != null)
                placingAtCursor.cancel();
            draggedViaMiddlePress = false; // Reset this field
            middlePressPointGrid = circuitPointAtMouse(true);
        } else if (e.getButton() == MouseButton.PRIMARY) {
            // Normal Left Click Processing
            pressPointGrid = circuitPointAtMouse(true);
            pressedOnSelectedEntity = currSelection.intercepts(panelDrawPointAtMouse());
            if (placingAtCursor != null)
                placingAtCursor.place(circuitPointAtMouse(true));
            else
                determineSelecting();
            repaint(c());
        } else if (e.getButton() == MouseButton.SECONDARY) {
            if (placingAtCursor != null)
                placingAtCursor.cancel();
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
        repaint();
        updateConnectionView();
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
        }
        updateMousePos(e);
        if (leftDown()) {
            if (selectionBoxStartPoint != null)
                onMouseDragWhileCreatingSelectionBox();
            if (gridSnapJustChanged)
                onGridSnapChangeWhileDragging();
        }
        repaint(c());
    }

    public Circuit c() {
        return currProject.getCurrentCircuit();
    }

    private Timer pasteFailedTimer = new Timer(400, new PasteFailedListener());

    private class PasteFailedListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (placingAtCursor != null)
                placingAtCursor.placeFailedBox = null;
            pasteFailedTimer.stop();
        }
    }

    public PlaceableEntityList placingAtCursor;

    public class PlaceableEntityList {

        /** Should be similar entities not on Circuit */
        private EntityList<? extends Entity> entities;

        private BoundingBox box;

        public PlaceableEntityList(EntityList<? extends Entity> entities) {
            if (entities != null && entities.size() > 0) {
                this.entities = entities.deepClone();
                box = new BoundingBox(this.entities);
                EditorPanel.this.placingAtCursor = this;
            }
        }

        public void onGridSnapChange() {
            if (isPreviewingPlace())
                onGridSnapChangeWhilePreviewPlace();
        }

        public void draw(GraphicsContext g) {
            if (placePreview != null)
                drawPlacePreview(g);
            if (placeFailedBox != null)
                placeFailedBox.drawBorder(g, Color.RED);
        }

        private EntityList<? extends Entity> placePreview;
        public BoundingBox placePreviewBox = null;

        public void placePreview(CircuitPoint location) {
            place(location.getGridSnapped(), true);
            placePreviewBox = new BoundingBox(placePreview);
            System.out.println("PASTE PREVIEW BOX: " + placePreviewBox);
        }

        public void drawPlacePreview(GraphicsContext g) {
            for (Entity e : placePreview)
                e.draw(g, Color.GRAY, 1);
            for (Entity e : placePreview)
                for (CircuitPoint invalidPoint : e.getInvalidInterceptPoints()) {
                    c().drawInvalidGridPoint(g, invalidPoint);
                    c().drawInvalidGridPoint(g, invalidPoint); // Double draw, so you can see it easier
                }
            if (!placePreviewBox.isInMapRange() && placeFailedBox == null)
                placePreviewBox.drawBorder(g, Color.RED);
        }

        public void onGridSnapChangeWhilePreviewPlace() {
            CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
            Vector movementVec = new Vector(placePreviewBox.p1, gridSnapAtMouse);
            placePreviewBox = new BoundingBox(placePreviewBox.p1.getIfModifiedBy(movementVec)
                    , placePreviewBox.p4.getIfModifiedBy(movementVec), null);
            placePreview.forEach(entity -> entity.move(movementVec));
            for (Entity e : placePreview)
                e.updateInvalidInterceptPoints();
        }

        public boolean isPreviewingPlace() {
            return placePreview != null;
        }

        public void cancel() {
            EditorPanel.this.placingAtCursor = null; // Throw out this reference
        }

        public boolean place(CircuitPoint location) {
            return place(location, false);
        }

        private BoundingBox placeFailedBox = null;

        public boolean place(CircuitPoint location, boolean preview) {
            Circuit c = location.getCircuit();
            // Since we want paste to go between Circuits we will clone it onto the one supplied in params
            EntityList<? extends Entity> clone = entities.deepCloneOnto(c);
            BoundingBox originalBox = box.clone(c);
            Vector vectorToLoc = new Vector(originalBox.p1, location);
            BoundingBox newBox = new BoundingBox(
                    originalBox.p1.getIfModifiedBy(vectorToLoc)
                    , originalBox.p4.getIfModifiedBy(vectorToLoc)
                    , null);
            for (Entity e : clone)
                e.move(vectorToLoc);
            if (preview)
                 placePreview = clone;
            else {
                if (!newBox.isInMapRange()) {
                    placeFailedBox = newBox;
                    pasteFailedTimer.start();
                    return false;
                }
                c.currentSelectionReference().deselectAllAndTrack();
                for (Entity e : clone)
                    c.new EntityAddOperation(e, true).operate();
                for (Entity e : clone)
                    c.new SelectOperation(e, true).operate();
                c.appendCurrentStateChanges("Add " + entities.size() + " entities at " + location.toParsableString());
                cancel(); // We are done with this CursorEntityList now
            }
            return true;
        }

    }

    private boolean drawGridPoints = true;

    private void toggleDrawGridPoints() {
        drawGridPoints = !drawGridPoints;
    }

    private void drawGridPoints(GraphicsContext g) {
        int w = canvasWidth(), h = canvasHeight();
        Color col = COL_GRID;
        double whiteWeight = 0;
        if (c().getScale() < 10)
            whiteWeight = 1 - (c().getScale() / 10.0);
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
        CircuitPoint atMouse = circuitPointAtMouse(false);
        CircuitPoint gridSnapped = atMouse.getGridSnapped();
        ArrayList<Entity> deselecting = new ArrayList<>();
        EntityList<Entity> selected = new EntityList<>();

        if (currentPullPoint != null) {
            deselecting.addAll(currSelection);
            currSelection.deselectAllAndTrack();
        } else {
            // Condition where they have a current selection, but clicked out of the selection without holding shift
            if (!currSelection.isEmpty() && !currSelection.intercepts(atMouse) && !shift) {
                deselecting.addAll(currSelection);
                currSelection.deselectAllAndTrack();
            }
            if (!currSelection.isEmpty() && currSelection.intercepts(atMouse)) {
                if (shift) {
                    boolean canDeselect = true;
                    for (Entity e : atMouse.getInterceptingEntities()) {
                        if (e.getBoundingBox() != null
                                && e.getBoundingBox().intercepts(atMouse)
                                && !currSelection.containsExact(e)) {
                            canDeselect = false; // Don't deselect if something isn't selected at where they shift clicked
                        }
                    }
                    for (Entity e : currSelection.clone()) {
                        if (canDeselect && e.getBoundingBox().intercepts(atMouse)) {
                            deselecting.add(e);
                            break; // If there were no de-selected entities at cursor, then we can deselect one by one (hence break)
                        }
                    }
                }
                currSelection.deselectMultipleAndTrackStateOperation(deselecting);
            }

            if (currSelection.isEmpty() || shift) {
                ArrayList<Entity> potentialClickSelection = new ArrayList<>();
                for (Entity e : atMouse.getInterceptingEntities()) {
                    if (e.getBoundingBox() != null
                            && e.getBoundingBox().intercepts(atMouse)
                            && !deselecting.contains(e)
                            && !e.isSelected()) {
                        potentialClickSelection.add(e);
                        if (shift) // When shift clicking, only one can be selected at a time
                            break;
                    }
                }
                if (potentialClickSelection.size() > 0) {
                    selected.addAll(currSelection.selectMultipleAndTrackOperation(potentialClickSelection));
                } else
                    selectionBoxStartPoint = circuitPointAtMouse(false);
            }
            selectedSomethingLastPress = selected.size() > 0;
        }

        determineMovingSelection(atMouse.getGridSnapped());

        String undoMsg;
        int numDes = deselecting.size();
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
        public void draw(GraphicsContext g) {
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
    private CircuitPoint movingSelectionStartPoint = null;
    private CircuitPoint lastSelectionMovePoint = null;
    private EntityList<Entity> movingSelectionPreviewEntities;

    public boolean isMovingSelection() {
        return movingSelection;
    }

    public CircuitPoint getMovingSelectionStartPoint() {
        return movingSelectionStartPoint == null ? null : movingSelectionStartPoint.getSimilar();
    }

    public CircuitPoint getLastSelectionMovePoint() {
        return lastSelectionMovePoint == null ? null : lastSelectionMovePoint.getSimilar();
    }

    private void determineMovingSelection(CircuitPoint mousePressedAt) {
        cancelMovingSelection();
        Circuit.Selection currSelection = c().currentSelectionReference();
        if (currentPullPoint == null
                && !currSelection.isEmpty()
                && currSelection.intercepts(mousePressedAt)
                && !shift)
            onStartMovingSelection(mousePressedAt);
    }

    private void onStartMovingSelection(CircuitPoint mousePressedAt) {
        movingSelectionStartPoint = mousePressedAt.clone();
        lastSelectionMovePoint = movingSelectionStartPoint.clone();
        movingSelectionPreviewEntities = c().currentSelectionReference().deepClone();
    }

    private void onGridSnapChangeWhileDraggingSelection() {
        movingSelection = true;
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        Vector movementVector = new Vector(lastSelectionMovePoint, gridSnapAtMouse);
        BoundingBox selectionBoundingBox = new BoundingBox(movingSelectionPreviewEntities);
        if (!selectionBoundingBox.getIfModifiedBy(movementVector).isInMapRange())
            return;
        for (Entity e : movingSelectionPreviewEntities)
            e.move(new Vector(lastSelectionMovePoint, gridSnapAtMouse));
        for (Entity e : movingSelectionPreviewEntities)
            e.updateInvalidInterceptPoints(c().currentSelectionReference());
        lastSelectionMovePoint = gridSnapAtMouse.getSimilar();
    }

    private void cancelMovingSelection() {
        onStopMovingSelection(true);
    }

    private void onStopMovingSelection() {
        onStopMovingSelection(false);
    }

    private void onStopMovingSelection(boolean cancel) {
        if (!cancel) {
            Vector movementVec = new Vector(movingSelectionStartPoint, lastSelectionMovePoint);
            userMoveSelection(movementVec, movementVec.toParsableString());
        }
        movingSelection = false;
        movingSelectionStartPoint = null;
        movingSelectionPreviewEntities = null;
        lastSelectionMovePoint = null;
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
        if (pressPointGrid.equals(releasePointGrid) && (currSelection.isEmpty() || shift) && !gridSnapChangedSinceLastPress) {
            if (!pressedOnSelectedEntity && !selectedSomethingLastPress) {
                determineSelecting();
            }
        }
        if (movingSelection)
            onStopMovingSelection();
        else
            cancelMovingSelection(); // Reset fields such as movingSelectionStartPoint etc
    }


    public void onGridSnapChangeWhileDragging() {
        if (currentPullPoint != null) {
            currentPullPoint.onDragGridSnapChange();
        }
        if (movingSelectionStartPoint != null)
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


            if (drawGridPoints)
                drawGridPoints(gc);

            EntityList<Entity> drawOrder = new EntityList<>();
            EntityList<Entity> drawFirst = new EntityList<>();
            EntityList<Entity> drawSecond = new EntityList<>();
            for (Entity e : c.getAllEntities()) {
                if (!currConnectionView.contains(e) && !e.isSelected()) {
                    if (e instanceof Wire)
                        drawFirst.add(e);
                    else
                        drawSecond.add(e);
                }
            }
            drawOrder.addAll(drawFirst);
            drawOrder.addAll(drawSecond);

            ArrayList<ConnectionNode> oscillated = new ArrayList<>();
            for (Entity e : drawOrder) {
                if (e instanceof ConnectibleEntity)
                    for (ConnectionNode n : ((ConnectibleEntity) e).getConnections())
                        if (n.isOscillated())
                            oscillated.add(n);
                if (e instanceof Wire) {
                    if (getScreenBoundaries().simpleTouches(e))
                        e.draw(gc);
                } else if (getScreenBoundaries().touches(e))
                        e.draw(gc);

            }

            oscillated.forEach(node -> node.drawOscillationNumber(gc));

            if (currentPullPoint != null)
                currentPullPoint.drawPullPoint(gc);

            if (currSelectionBox != null)
                currSelectionBox.draw(gc);

            c.drawInvalidEntities(gc);

            for (Entity e : currConnectionView) {
                currConnectionView.draw(e, gc);
            }

            if (!movingSelection || lastSelectionMovePoint.isSimilar(movingSelectionStartPoint)) {
                for (Entity e : currSelection)
                    e.draw(gc);
                for (Entity e : currSelection)
                    e.getBoundingBox().draw(gc);
            }
            else {
                for (Entity e : movingSelectionPreviewEntities) {
                    e.drawPreview(gc);
                    for (CircuitPoint invalidPoint : e.getInvalidInterceptPoints()) {
                        c.drawInvalidGridPoint(gc, invalidPoint);
                        c.drawInvalidGridPoint(gc, invalidPoint); // Double draw, so you can see it easier
                    }
                }
            }

            if (placingAtCursor != null)
                placingAtCursor.draw(gc);


            if (fuckedDrawState == 1 && lastFuckedUpOperand != null)
                lastFuckedUpOperand.draw(gc, Color.PINK, 0.75);

        });
    }






    private ArrayList<AutoInputPoker> autoPokers;

    public void onPoke() {
        boolean manuallyPokedAnything = false;
        for (Entity e : c().getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse())) {
                    ((Pokable) e).onPoke();
                    manuallyPokedAnything = true;
                }
            }
        }
        if (!manuallyPokedAnything) {
            autoPokers.forEach(AutoInputPoker::resetToZeroState);
            ArrayList<InputBlock> autoPoking = new ArrayList<>();
            for (Entity e : c().currentSelectionReference())
                if (e instanceof InputBlock)
                    autoPoking.add((InputBlock) e);
            if (!autoPoking.isEmpty())
                this.new AutoInputPoker(autoPoking);
        } else {
            autoPokers.clear();
        }
    }

    private Timer autoPokeTimer = new Timer(750, (e -> {
        Platform.runLater(() -> {
            if (!autoPokers.isEmpty()) {
                autoPokers.forEach(AutoInputPoker::nextState);
                autoPokers.forEach(AutoInputPoker::setPowerStatuses);
                repaint();
            }
        });
    }));

    // Init Auto poke stuff
    {
        autoPokers = new ArrayList<>();
        autoPokeTimer.start();
    }

    private class AutoInputPoker {

        private InputBlock[] poking;

        private String[] states;
        private int stateIndex;

        public AutoInputPoker(InputBlock... poking) {
            this.poking = poking;
            int numStates = (int) Math.pow(2, poking.length);
            states = new String[numStates];
            for (int i = 0; i < numStates; i++) {
                StringBuilder binaryString = new StringBuilder(Integer.toBinaryString(i));
                while (binaryString.length() < poking.length) // add leading zeros
                    binaryString.insert(0, "0");
                states[i] = binaryString.toString();
            }
            autoPokers.add(this);
        }

        public AutoInputPoker(ArrayList<InputBlock> poking) {
            this(poking.toArray(InputBlock[]::new));
        }

        public void resetToZeroState() {
            stateIndex = 0;
        }

        public void nextState() {
            stateIndex = (stateIndex + 1) % (states.length);
        }

        public void setPowerStatuses() {
            String stateString = states[stateIndex];
            System.out.println("CURR STATE STRING: " + stateString);
            for (InputBlock poking : poking) {
                if (poking.getPowerBoolean())
                    poking.onPoke();
            }
            for (int i = 0; i < stateString.length(); i++) {
                InputBlock poking = this.poking[i];
                if (stateString.charAt(i) == '1')
                    poking.onPoke();
            }
        }

    }



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
                   c.removeSimilarEntityAndTrackOperation(new Wire(start.clone(), end.clone()));
                   System.out.println("SHORT");
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
                        for (Wire t : theos)
                            c.new EntityAddOperation(t, true).operate();
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

        public void dropAndRestart() {
            if (ppStateShift > 0 && !shift) {
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