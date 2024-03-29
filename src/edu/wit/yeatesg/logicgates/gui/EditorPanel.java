package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.InputBlock;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.circuit.entity.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import javax.swing.Timer;
import static edu.wit.yeatesg.logicgates.circuit.Circuit.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import static edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire.*;

import static javafx.scene.input.KeyCode.*;

public class EditorPanel extends StackPane {

    private final Canvas canvas;
    public Project currProject;

    public EditorPanel(Project p) {
        canvas = new Canvas();
        canvas.focusedProperty().addListener((observableValue, focusedBefore, focusedNow) -> { if (focusedNow) onFocusGained(); });
        getChildren().add(canvas);
        setListeners();
        currProject = p;
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
        return new BoundingBox(new PanelDrawPoint(0, 0, c()).toCircuitPoint().getGridSnapped(),
                new PanelDrawPoint(canvasWidth(), canvasHeight(), c()).toCircuitPoint().getGridSnapped(), null);
    }

    public static void drawArrow(GraphicsContext g, Color col, double distOut, double lineWidth, CircuitPoint start, CircuitPoint end) {
        Vector v = new Vector(start, end);
        Vector unit = v.getUnitVector();

        Circuit c = start.getCircuit();

        Vector n90 = unit.getRotated(90).getMultiplied(distOut);
        Vector n270 = unit.getRotated(270).getMultiplied(distOut);

        double a = distOut * 2;
        // h = (1/2) * sqrt(3) * a;
        Vector h = unit.getMultiplied(-(1.0/2.0)*Math.sqrt(3)*a);
        CircuitPoint triTop = end.getIfModifiedBy(unit.getMultiplied(0.1));
        CircuitPoint triBotMid = triTop.getIfModifiedBy(h);
        CircuitPoint triBotLeft = triBotMid.getIfModifiedBy(n90);
        CircuitPoint triBotRight = triBotMid.getIfModifiedBy(n270);

        Color strokeCol = col == null ? Color.BLACK : col;
        g.setStroke(strokeCol);
        g.setFill(strokeCol);
        g.setLineWidth(lineWidth);

        PanelDrawPoint tBL = triBotLeft.toPanelDrawPoint();
        PanelDrawPoint tBR = triBotRight.toPanelDrawPoint();
        PanelDrawPoint endPanel  = end.toPanelDrawPoint();
        PanelDrawPoint tT = triTop.toPanelDrawPoint();
        PanelDrawPoint s = start.toPanelDrawPoint();

        g.strokeLine(s.x, s.y, endPanel.x, endPanel.y);

        double[] ex, why;
        ex = new double[]{ tBL.x, tBR.x, tT.x, };
        why = new double[]{ tBL.y, tBR.y, tT.y, };

        g.fillPolygon(ex, why, 3);
        g.strokePolygon(ex, why, 3);


      /*  g.beginPath();
        g.moveTo(tBL.x, tBL.y);
        g.lineTo(tBR.x, tBR.y);
        g.lineTo(tT.x, tT.y);
        g.lineTo(tBL.x, tBL.y);
        g.fill();
        g.stroke();*/



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
        c.enablePowerUpdateBuffer();
        if (co == S // Displays whats in selection
                || co == R // Rotates selection
                || co == LEFT || co == RIGHT || co == DOWN || co == UP  // Moves selection
                || co == D && event.isControlDown() // Duplicates selection
                || co == ESCAPE // Deselects / Cancels selection move / entities at cursor
                || co.isDigitKey()
                || co == MINUS && !event.isControlDown()
                || co == EQUALS && !event.isControlDown()
                || co == DELETE
                || co == N) {
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
                else if (co == D && event.isControlDown()) {
                    duplicateSelection();
                }
                else if (co.isDigitKey()) {
                    if (Integer.parseInt(co.getName()) >= 2)
                        for (Entity selected : currSelection.clone())
                            if (selected.hasProperty("num inputs"))
                                selected.onPropertyChangeViaTable("num inputs", co.getName());
                    c.appendCurrentStateChanges("Change Num Inputs Of " + currSelection.size() + " Entit" + (currSelection.size() == 1 ? "y" : "ies" ) + " to " + co.getName());
                }
                else if (co == MINUS) {
                    for (Entity selected : currSelection.clone())
                        if (selected.hasProperty("size") && selected.getSize() == Entity.NORMAL)
                            c.new PropertyChangeOperation(selected, "size", "SMALL", true).operate();
                    c.appendCurrentStateChanges("Size Down " + currSelection.size() + " Entit" + (currSelection.size() == 1 ? "y" : "ies" ));
                } else if (co == EQUALS){
                    for (Entity selected : currSelection.clone())
                        if (selected.hasProperty("size") && selected.getSize() == Entity.SMALL)
                            c.new PropertyChangeOperation(selected, "size", "NORMAL", true).operate();
                    c.appendCurrentStateChanges("Size Up " + currSelection.size() + " Entit" + (currSelection.size() == 1 ? "y" : "ies" ));
                } else if (co == DELETE)
                    currSelection.deleteAllEntitiesAndTrack();
                else if (co == N) {
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
            }
            currSelection.selectionTableUpdate();

        }
        c.disableAndPollPowerUpdateBuffer();
    }


    public void onKeyPressed(KeyEvent e) {
        if (canvas.isFocused()) {
            Circuit c = c();
            Circuit.Selection currSelection = c.currentSelectionReference();
            KeyCode code = e.getCode();
            onKeyPressDetermineSelectionModifications(e);

            if (code == OPEN_BRACKET) {
                if (autoPokeTimer.getDelay() == 25)
                    autoPokeTimer.setDelay(50);
                else
                    autoPokeTimer.setDelay(autoPokeTimer.getDelay() + 50);
            }
            if (code == CLOSE_BRACKET) {
                if (autoPokeTimer.getDelay() >= 100)
                    autoPokeTimer.setDelay(autoPokeTimer.getDelay() - 50);
                if (autoPokeTimer.getDelay() == 50)
                    autoPokeTimer.setDelay(25);
            }
            if (code == BACK_SLASH);
            //  c.getOperands(new Wire(LL, RR));
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
                userCTRLZ(e.isShiftDown());
            }
            if (e.isControlDown() && e.getCode() == Y ) {
                userCTRLY(e.isShiftDown());
            }

            if (e.isControlDown() && (code == EQUALS || code == MINUS)) {
                CircuitPoint oldCenter = getCenter();
                if (code == EQUALS && c().canScaleUp()) {
                    c().scaleUp();
                    view(oldCenter);
                    repaint();
                } else if (code == MINUS && c().canScaleDown()) {
                    c().scaleDown();
                    view(oldCenter);
                    repaint();
                }
            }
            if (code == SPACE)
                holdingSpace = true;
            if (code == SHIFT) {
                shift = true;
                if (ppStateShift == 0)
                    currentPullPoint = null;
                repaint();
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
                repaint();
            }
            if (code == O) {
                viewOrigin();
            }
            if (code == G) {
                toggleDrawGridPoints();
            }
            e.consume();
            repaint();
        }
    }

    public void userCTRLY(boolean shiftDown) {
        Circuit c = c();
        c.enablePowerUpdateBuffer();
        if (shiftDown)
            megaRedo(true);
        else
            redo(true);
        c.disableAndPollPowerUpdateBuffer();
        c.selectionTableUpdate();
    }

    public void userCTRLZ(boolean shiftDown) {
        Circuit c = c();
        c.enablePowerUpdateBuffer();
        if (shiftDown)
            megaUndo(true);
        else
            undo(true);
        c.disableAndPollPowerUpdateBuffer();
        c.selectionTableUpdate();
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
    }

    public void onKeyReleased(KeyEvent e) {
        if (canvas.isFocused()) {
            if (e.getCode() == KeyCode.SPACE)
                holdingSpace = false;
            if (e.getCode() == SHIFT)
                shift = false;
            Circuit c = c();
            Circuit.Selection currSelection = c.currentSelectionReference();
            currSelection.updateConnectionView();
        }
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
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        CircuitPoint gridAtMouse = circuitPointAtMouse(true);
        canUserShiftState = false;
        gridSnapJustChanged = false;
        gridSnapChangedSinceLastPress = false;
        System.out.println("DIM: " + getWidth() + " h: " + getHeight());
        System.out.println("MOUSE PRESS " + e.getButton());
        updateMousePos(e);
        System.out.println("MOUSE PRESS: GRID SNAP CHANGE? " + gridSnapJustChanged);
        System.out.println(circuitPointAtMouse(true) + " AT MOUSE");
        CircuitPoint chunkCoords = gridAtMouse.getChunkCoords();
        int chunkX = (int) chunkCoords.x;
        int chunkY = (int) chunkCoords.y;
        System.out.println(chunkCoords + "CHUNK COORDS: " + chunkX + "," + chunkY);
        System.out.println(c.getChunkAt(chunkX, chunkY) + " = getChunkAt(mouse)");
        c.getChunkAt(circuitPointAtMouse(true)).forEach(ent -> System.out.println("  " + ent));

        BoundingBox screenBox = getScreenBoundaries();
        System.out.println("SCREEN TOP LEFT CP: " + screenBox.p1);
        System.out.println("SCREEN BOT RIGHT CP: " + screenBox.p4);

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
            if (placingAtCursor != null) {
                c.enablePowerUpdateBuffer();
                placingAtCursor.place(circuitPointAtMouse(true));
                c.disableAndPollPowerUpdateBuffer();
            }
            else
                determineSelecting();
            repaint();
        } else if (e.getButton() == MouseButton.SECONDARY) {
            if (placingAtCursor != null)
                placingAtCursor.cancel();
            rightPressPointGrid = circuitPointAtMouse(true);
            if (currentPullPoint != null)
                currentPullPoint.dropAndRestart();
        }
        c.selectionTableUpdate();
        repaint();

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
        repaint();
        updateConnectionView();
        gridSnapJustChanged = false;
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
        repaint();
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

    public void cancelCurrentPlacement() {
        placingAtCursor = null;
    }

    public class PlaceableEntityList {

        /** Should be similar entities not on Circuit */
        private EntityList<? extends Entity> entities;

        private BoundingBox box;

        public PlaceableEntityList(EntityList<? extends Entity> entities) {
            if (entities != null && entities.size() > 0) {
                this.entities = entities.deepClone();
                box = new BoundingBox(this.entities);
                EditorPanel.this.placingAtCursor = this;
                placePreview(circuitPointAtMouse(true));
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
                placeFailedBox.drawBorder(g, Color.RED, c().getLineWidth());
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
                placePreviewBox.drawBorder(g, Color.RED, c().getLineWidth());
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

    public Entity previewNextFocusGained;

    public void previewPlacementNextFocusGain(Entity e) {
        previewNextFocusGained = e;
    }

    private void onFocusGained() {
        if (previewNextFocusGained != null) {
            this.new PlaceableEntityList(new EntityList<>(previewNextFocusGained));
            previewNextFocusGained = null;
        }
    }



    private boolean drawGridPoints = true;

    private void toggleDrawGridPoints() {
        drawGridPoints = !drawGridPoints;
    }

    private void drawGridPoints(GraphicsContext g) {
        Circuit c = c();
        Color col = COL_GRID;
        double whiteWeight = 0;
        if (c.getScale() < 60)
            whiteWeight = (1 - (c().getScale() / 60));
        double normWeight = 1 - whiteWeight;
        int white = (int) (255.0*whiteWeight);
        Color paintCol = Color.rgb((int) (white + col.getRed()*255*normWeight),
                (int) (white + col.getGreen()*255*normWeight),
                (int) (white + col.getBlue()*255*normWeight), 1);
        g.setFill(paintCol);
        g.fillRect(0, 0, canvasWidth(), canvasHeight());
        int scale = (int) c.getScale();

        int gridWidth = c.getGridLineWidth();
   //     int widthOut = gridWidth % 2 == 0 ? gridWidth / 2 : (gridWidth - 1) / 2;
        double widthOut = gridWidth / 2.0;

        g.setFill(COL_BG);
        for (int x = -3*scale; x < canvasWidth() + 3*scale; x+= scale) {
            PanelDrawPoint dp = new PanelDrawPoint(x, 0, c).toCircuitPoint().getGridSnapped().toPanelDrawPoint();
            dp.x += widthOut + 1;
            g.fillRect(dp.x, 0, scale - 2*widthOut - 1, canvasHeight());
        }
        for (int y = -3*scale; y < canvasHeight() + 3*scale; y+= scale) {
            PanelDrawPoint dp = new PanelDrawPoint(0, y, c).toCircuitPoint().getGridSnapped().toPanelDrawPoint();
            dp.y += widthOut + 1;
            g.fillRect(0, dp.y, canvasWidth(), scale - 2*widthOut - 1);
        }

        PanelDrawPoint o = new CircuitPoint(0, 0, c).toPanelDrawPoint();
        double circleSize = (c.getScale() * 0.2);
        double radius = circleSize / 2.0;

        g.setFill(Color.RED);
        g.fillOval(o.x - radius, o.y - radius, circleSize, circleSize);

       // int w = canvasWidth(), h = canvasHeight();

      /*  for (int x = (int)(-w*0.1); x < w + w*0.1; x += c().getScale()) {
            for (int y = (int)(-h*0.1); y < h + h*0.1; y += c().getScale()) {
                CircuitPoint gridPoint = circuitPointAt(x, y, true);
                if (gridPoint.isInMapRange()) {
                    PanelDrawPoint drawLoc = gridPoint.toPanelDrawPoint();
             //       System.out.println(paintCol.getRed() + " " + paintCol.getGreen() + " " + paintCol.getBlue());
                    g.setLineWidth(c().getGridLineWidth());
                    g.setStroke(paintCol);
                    if (gridPoint.representsOrigin()) {
                        double strokeSize = c().getGridLineWidth();
                        strokeSize *= 1.5;
                        if (strokeSize == c().getGridLineWidth())
                            strokeSize++;
                        g.setLineWidth(strokeSize);
                        g.setStroke(Circuit.COL_ORIGIN);
                    }
                    g.strokeLine(drawLoc.x, drawLoc.y, drawLoc.x, drawLoc.y);
                }
            }
        }*/
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
        c.selectionTableUpdate();
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
            currSelection.selectionTableUpdate();
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
        repaint();
    }

    public void onReleaseSelectionBox() {
        c().enablePowerUpdateBuffer();
        currSelectionBox.selectEntities();
        currSelectionBox = null;
        c().disableAndPollPowerUpdateBuffer();
    }

    private void determineSelectingMouseRelease() {
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        if (pressPointGrid.equals(releasePointGrid) && (currSelection.isEmpty() || shift) && !gridSnapChangedSinceLastPress) {
            if (!pressedOnSelectedEntity && !selectedSomethingLastPress) {
                determineSelecting();
            }
        }
        if (movingSelection) {
            c.enablePowerUpdateBuffer();
            onStopMovingSelection();
            c.disableAndPollPowerUpdateBuffer();
        }
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
            repaint();
        }
    }

    public int canvasWidth() {
        return (int) canvas.getWidth();
    }

    public int canvasHeight() {
        return (int) canvas.getHeight();
    }


    public DrawMark currDrawMark;

    public class DrawMark {

        private boolean active;

        public DrawMark() {
            currDrawMark = this;
            active = true;
        }

        public void deactivate() {
            active = false;
        }

        public boolean isActive() {
            return active;
        }
    }

    public void repaint() {
        Project p = currProject;
        Circuit c = c();
        Circuit.Selection currSelection = c.currentSelectionReference();
        Circuit.ConnectionSelection currConnectionView = c.currentConnectionViewReference();


        Platform.runLater(() -> {
            this.new DrawMark();
            try {
                double width = canvasWidth();
                double height = canvasHeight();
                GraphicsContext gc = canvas.getGraphicsContext2D();


                gc.setFill(Color.rgb(240, 240, 240, 1));
                gc.fillRect(0, 0, width, height);

                gc.setFill(COL_BG);
                BoundingBox rangeBox = c.getInterceptMap().getBoundingBox().getExpandedBy(0.5);
                PanelDrawPoint tl = rangeBox.p1.toPanelDrawPoint();
                PanelDrawPoint tr = rangeBox.p2.toPanelDrawPoint();
                PanelDrawPoint bl = rangeBox.p3.toPanelDrawPoint();
                PanelDrawPoint br = rangeBox.p4.toPanelDrawPoint();

                gc.fillRect(tl.x, tl.y, tr.x - tl.x, br.y - tr.y);

                if (drawGridPoints)
                    drawGridPoints(gc);

                gc.setStroke(Color.BLACK);
                gc.setLineWidth(c.getScale() * 0.2);
                gc.strokeLine(tl.x, tl.y, bl.x, bl.y); // Top left to bottom left
                gc.strokeLine(tl.x, tl.y, tr.x, tr.y); // Top left to top right
                gc.strokeLine(tr.x, tr.y, br.x, br.y); // Top right to bottom right
                gc.strokeLine(bl.x, bl.y, br.x, br.y); // Bottom left to bottom right


                BoundingBox screenBounds = getScreenBoundaries();
                EntityList<Entity> drawing = new EntityList<>();
                c.getChunksBetween(screenBounds.p1, screenBounds.p4).getEntityIterator().forEachRemaining(drawing::add);

                EntityList<Entity> drawOrder = new EntityList<>();
                EntityList<Entity> drawFirst = new EntityList<>();
                EntityList<Entity> drawSecond = new EntityList<>();

                EntityList<Entity> selectedDrawFirst = new EntityList<>();
                EntityList<Entity> selectedDrawSecond = new EntityList<>();

                for (Entity e : drawing) {
                    if (!e.isDrawMarked()) {
                        e.setDrawMark(currDrawMark);
                        if (!currConnectionView.contains(e)) {
                            if (e instanceof Wire) {
                                if (e.isSelected()) {
                                    selectedDrawFirst.add(e);
                                    continue;
                                }
                                drawFirst.add(e);
                            }
                            else {
                                if (e.isSelected()) {
                                    selectedDrawSecond.add(e);
                                    continue;
                                }
                                drawSecond.add(e);

                            }
                        }
                    }
                }
                drawOrder.addAll(drawFirst);
                drawOrder.addAll(drawSecond);

                EntityList<Entity> selected = new EntityList<>();
                selected.addAll(selectedDrawFirst);
                selected.addAll(selectedDrawSecond);

                ArrayList<ConnectionNode> oscillated = new ArrayList<>();
                for (Entity e : drawOrder) {
                    if (e instanceof ConnectibleEntity)
                        for (ConnectionNode n : ((ConnectibleEntity) e).getConnections())
                            if (n.isOscillated())
                                oscillated.add(n);
                    e.draw(gc);
                }

                oscillated.forEach(node -> node.drawOscillationNumber(gc));

                if (currentPullPoint != null)
                    currentPullPoint.drawPullPoint(gc);

                if (currSelectionBox != null)
                    currSelectionBox.draw(gc);

                for (Entity e : currConnectionView) {
                    currConnectionView.draw(e, gc);
                }

                if (!currSelection.isEmpty() && (!movingSelection || lastSelectionMovePoint.isSimilar(movingSelectionStartPoint))) {
                    for (Entity e : selected)
                        e.draw(gc);
                    for (Entity e : selected)
                        e.getBoundingBox().draw(gc);
                } else if (movingSelection && !lastSelectionMovePoint.isSimilar(movingSelectionStartPoint)){
                    for (Entity e : movingSelectionPreviewEntities) {
                        e.drawPreview(gc);
                    }
                    for (Entity e : movingSelectionPreviewEntities) {
                        for (CircuitPoint invalidPoint : e.getInvalidInterceptPoints()) {
                            c.drawInvalidGridPoint(gc, invalidPoint);
                            c.drawInvalidGridPoint(gc, invalidPoint); // Double draw, so you can see it easier
                        }
                    }
                }

                c.drawInvalidEntities(gc);


                if (placingAtCursor != null)
                    placingAtCursor.draw(gc);

                if (fuckedDrawState == 1 && lastFuckedUpOperand != null)
                    lastFuckedUpOperand.draw(gc, Color.PINK, 0.75);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                currDrawMark.deactivate();
            }
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
        repaint();
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
                    repaint();
                return;
            }
            EntityList<ConnectibleEntity> cesAtStart = c.getEntitiesThatIntercept(pressPoint).thatExtend(ConnectibleEntity.class);
            pullDir = null;
            this.originalLoc = originalLoc;
            currentPullPoint = this;
            if (originalLoc.equals(pressPoint) && !lock)
                for (Wire w : pressPoint.getInterceptingEntities().thatExtend(Wire.class))
                    if (w.isEdgePoint(pressPoint))
                        canDelete = true;
            if (!canCreateFromAll(cesAtStart))
                deleteOnly = true;
            if (old == null)
                repaint();
        }

        public PullPoint(CircuitPoint location) {
            this(location, location, false);
        }

        public boolean canBePlacedHere(CircuitPoint location) {
            for (ConnectibleEntity ce : location.getInterceptingEntities().thatExtend(ConnectibleEntity.class))
                if (ce.isPullableLocation(location))
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
            c.enablePowerUpdateBuffer();

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
            if (startAndEndWires.size() > 1 && !start.isSimilar(end)) {
                c.disableAndPollPowerUpdateBuffer();
                throw new RuntimeException("Should not intercept 2 wires twice");
            }
            Wire deleting = !end.isSimilar(start) && startAndEndWires.size() > 0 ? startAndEndWires.get(0) : null;

            if (canDelete && deleting != null && !lock) {
                // DO THE OPERATION FIRST SO IT CAN PROPERLY CHECK THE SPECIAL CASE WHERE THE DELETED WIRE CAUSES A BISECT
                if (new Wire(start, end).isSimilar(deleting)) {
                   c.removeSimilarEntityAndTrackOperation(new Wire(start.clone(), end.clone()));
                   undoMsg = "Delete Wire";
                    // TODO replace with c.deleteWithStateOperation
                } else {
                   c.removeSimilarEntityAndTrackOperation(new Wire(start.clone(), end.clone()));
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

            c.disableAndPollPowerUpdateBuffer();
            repaint();
        }

        public void onRelease() {
            boolean repaint = ppStateShift != 0;
            ppStateShift = 0;
            currentPullPoint = null;
            if (repaint)
                repaint();
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