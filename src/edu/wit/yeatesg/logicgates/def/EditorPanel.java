package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.*;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.awt.event.KeyEvent.*;

public class EditorPanel extends JPanel implements MouseListener, MouseMotionListener, KeyListener {

    private Circuit currentCircuit;

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        EditorPanel panel = new EditorPanel(frame);
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

        public void draw(Entity inThisSelection, Graphics2D g) {
            inThisSelection.draw(g);
            if (blinkState) {
                Stroke stroke = new BasicStroke(3);
                g.setStroke(stroke);
                g.setColor(Color.orange);
                inThisSelection.getBoundingBox().drawBorder(g);
            }
        }

        public void resetTimer() {
            blinkState = true;
            blinkTimer.restart();
        }
    }

    public Color backgroundColor = Color.white;

    public EditorPanel(JFrame frame) {
        frame.add(this);
        frame.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        frame.setSize(800, 800);
        frame.setVisible(true);
        currentCircuit = new Circuit();
        currentCircuit.setEditorPanel(this);
        viewOrigin();
        new GateAND(new CircuitPoint(0, 10, currentCircuit), 90);
        new GateAND(new CircuitPoint(-10, 10, currentCircuit), 180);

        new Wire(new CircuitPoint(0, 0, currentCircuit), new CircuitPoint(0, -5, currentCircuit));
        new Wire(new CircuitPoint(0, 0, currentCircuit), new CircuitPoint(-5, 0, currentCircuit));
        new Wire(new CircuitPoint(0, 0, currentCircuit), new CircuitPoint(5, 0, currentCircuit));

        new InputBlock(new CircuitPoint(-5, -5, currentCircuit), 180);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        try {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            CircuitPoint mouseSnap = circuitPointAtMouse(true);

            g.setColor(backgroundColor);
            g.fillRect(0, 0, getWidth(), getHeight());

            drawGridPoints(g);

            drawTheoreticalWires(true, g);

            for (Entity e : currentCircuit.getAllEntities()) {
                if (!currConnectionView.contains(e))
                    e.draw(g);
            }
            drawTheoreticalWires(false, g);

            if (theoreticalDeletion != null)
                drawCurrentDeletion(g);

            for (Entity e : currConnectionView) {
                e.getBoundingBox().drawBorder(g);
                currConnectionView.draw(e, g);
            }

            for (Entity e : currSelection)
                e.getBoundingBox().paint(g);

            if (pullPoint != null && (!isPullingWire || mouseSnap.equals(pullPoint)))
                drawPullableCircle(g);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void drawGridPoints(Graphics2D g) {
        for (int x = (int)(-getWidth()*0.1); x < getWidth() + getWidth()*0.1; x += currentCircuit.getScale()) {
            for (int y = (int)(-getHeight()*0.1); y < getHeight() + getHeight()*0.1; y += currentCircuit.getScale()) {
                CircuitPoint gridPoint = circuitPointAt(x, y, true);
                PanelDrawPoint drawLoc = gridPoint.toPanelDrawPoint();
                g.setStroke(currentCircuit.getGridStroke());
                g.setColor(Circuit.COL_GRID);
                if (gridPoint.representsOrigin()) {
                    int strokeSize = currentCircuit.getStrokeSize();
                    strokeSize *= 1.5;
                    if (strokeSize == currentCircuit.getStrokeSize())
                        strokeSize++;
                    g.setStroke(new BasicStroke(strokeSize));
                    g.setColor(Circuit.COL_ORIGIN);
                }
                g.drawLine(drawLoc.x, drawLoc.y, drawLoc.x, drawLoc.y);
            }
        }
    }

    private Color hypotheticalPullColor = Color.orange;

    private void drawPullableCircle(Graphics2D g) {
        int strokeSize = (int) (currentCircuit.getStrokeSize() * 0.8);
        if (strokeSize % 2 == 0) strokeSize++;
        int circleSize = (int) (currentCircuit.getScale() / 2.5);
        if (circleSize % 2 != 0) circleSize++;
        int bigCircleSize = (int) (circleSize * 1.5);
        if (bigCircleSize % 2 != 0) bigCircleSize++;

        PanelDrawPoint dp = pullPoint.toPanelDrawPoint();
        g.setStroke(new BasicStroke(strokeSize));
        g.setColor(Color.black);
        g.drawOval(dp.x - bigCircleSize/2, dp.y - bigCircleSize/2, bigCircleSize, bigCircleSize);
        g.setColor(hypotheticalPullColor);
        g.drawOval(dp.x - circleSize/2, dp.y - circleSize/2, circleSize, circleSize);
    }

    public void viewOrigin() {
        currentCircuit.setXOffset(getWidth() / 2);
        currentCircuit.setYOffset(getHeight() / 2);
    }

    public void view(CircuitPoint location) {
        viewOrigin();
        PanelDrawPoint viewingLoc = location.toPanelDrawPoint();
        PanelDrawPoint originLoc = new CircuitPoint(0, 0, currentCircuit).toPanelDrawPoint();
        currentCircuit.modifyOffset(new Vector(viewingLoc, originLoc));
    }

    public CircuitPoint getCenter() {
        return getCenter(false);
    }

    public CircuitPoint getCenter(boolean gridSnap) {
        return circuitPointAt(getWidth() / 2, getHeight() / 2, gridSnap);
    }

    public void updatePossiblePullPoint() {
        if (!isPullingWire) {
            CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
            pullPoint = null;

            ArrayList<ConnectibleEntity> interceptingConnectibles = new ArrayList<>();
            for (ConnectibleEntity ce : currentCircuit.getAllEntitiesOfType(ConnectibleEntity.class))
                if (ce.intercepts(gridSnapAtMouse))
                    if (ce instanceof Wire)
                        interceptingConnectibles.add(ce);
                    else
                        for (ConnectionNode node : ce.getConnections())
                            if (node.getLocation().equals(gridSnapAtMouse))
                                interceptingConnectibles.add(ce);

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

    @Override
    public void mouseMoved(MouseEvent e) {
        if (holdingSpace) {
            currentCircuit.modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
            repaint();
        }
        updateMousePos(e);
        updatePossiblePullPoint();
    }

    private int mouseX;
    private int mouseY;

    public void updateMousePos(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
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
        return new PanelDrawPoint(x, y, currentCircuit);
    }

    public CircuitPoint circuitPointAt(int x, int y, boolean gridSnap) {
        CircuitPoint cp = new PanelDrawPoint(x, y, currentCircuit).toCircuitPoint();
        return gridSnap ? cp.getGridSnapped() : cp;
    }


    boolean movingSelection = false;
    boolean ctrl = false;

    @SuppressWarnings("unchecked")
    public void determineSelecting() {
        selectedSomething = false;
        PanelDrawPoint atMouse = panelDrawPointAtMouse();
        if (isPullingWire) {
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
                    for (Entity e : (ArrayList<Entity>) currSelection.clone()) {
                        if (e.getBoundingBox().intercepts(atMouse, true)) {
                            currSelection.remove(e);
                            deselected.add(e);
                        }
                    }
                }
                else
                    movingSelection = true;
            }

            if (currSelection.isEmpty() || ctrl) {
                ArrayList<Entity> potentialClickSelection = new ArrayList<>();
                for (Entity e : currentCircuit.getAllEntities())
                    if (e.getBoundingBox() != null
                            && e.getBoundingBox().intercepts(atMouse, true)
                            && !deselected.contains(e))
                        potentialClickSelection.add(e);
                if (potentialClickSelection.size() > 0) {
                    currSelection.addAll(potentialClickSelection);
                    selectedSomething = true;
                } else if (!ctrl) {
                    // Start creating selection box
                }

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
            /*
            SELECT DEBUG
            for (Entity e : currSelection) {
                if (e instanceof ConnectibleEntity) {
                    ConnectibleEntity ce = (ConnectibleEntity) e;
                    System.out.println("SELECTED " + e);
                    for (ConnectibleEntity connected : ce.getConnectedEntities())
                        System.out.println("  connected to " + connected + " at " + ce.getConnectionTo(connected).getLocation());
                }
            }
             */
        }

        LogicGates.debug("End Of DetermineSelecting() method", "", "CurrSelection", currSelection, "curr connection view", currConnectionView);
     //   repaint();
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


    @Override
    public void mousePressed(MouseEvent ev) {
        updateMousePos(ev);
        pressPointGrid = circuitPointAtMouse(true);
        pressedOnSelectedEntity = currSelection.intercepts(panelDrawPointAtMouse());
        determineIfPullingWire();
        determineSelecting();
        System.out.println("PRESSED END? " + pressedOnEndpoint);
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (holdingSpace) {
            currentCircuit.modifyOffset(new Vector(mouseX, mouseY, e.getX(), e.getY()));
            repaint();
        }
        updateMousePos(e);
        if (gridSnapChanged)
            onGridSnapChangeWhileDragging();
    }

    public CircuitPoint pressPointGrid;
    public CircuitPoint releasePointGrid;

    boolean multiSelecting = false;

    @Override
    public void mouseReleased(MouseEvent ev) {
        updateMousePos(ev);
        for (Wire w : currentCircuit.getAllEntitiesOfType(Wire.class))
            w.setOverrideColor(null);

        releasePointGrid = circuitPointAtMouse(true);
        if (pullPoint != null)
            onStopPulling();

        determineSelectingMouseRelease();
        repaint();

    }


    @Override
    public void mouseClicked(MouseEvent e) {
        updateMousePos(e);
 //       LogicGates.debug("MOUSE CLICK", "", "PANEL", panelDrawPointAtMouse(), "CIRCUIT", circuitPointAtMouse(false),
  //              "SNAP", circuitPointAtMouse(true));
    }


    private CircuitPoint pullPoint = null;
    private boolean isPullingWire = false;
    private Vector pullDir = null;
    private boolean pressedOnEndpoint = false;
    private Wire wireBeingShortened = null;
    private Wire.TheoreticalWire theoreticalDeletion = null;
    private List<Wire.TheoreticalWire> theoreticalCreations = new ArrayList<>();

    public void determineIfPullingWire() {
        pressedOnEndpoint = false;
        if (pullPoint != null && !ctrl) {
            onStartPulling();
        }
    }

    private void onStartPulling() {
        isPullingWire = true;
        for (Wire w : currentCircuit.getAllEntitiesOfType(Wire.class))
            if (w.isEdgePoint(pullPoint))
                pressedOnEndpoint = true;
    }

    private void onGridSnapChangeWhilePullingWire() {
        CircuitPoint gridAtMouse = circuitPointAtMouse(true);
        if (pullDir == null) { // If a preferred pull dir hasn't been chosen by the user yet, set it
            Vector dir = new Vector(pullPoint, gridAtMouse);
            if (!(dir.equals(Vector.ZERO_VECTOR)) && Vector.getDirectionVecs().contains(dir))
                pullDir = dir;
        }

        // Whether or not the mouse pos is on the same entity that the pull point is on
        boolean isSameEntity = currentCircuit.getAllEntities().thatInterceptAll(gridAtMouse, pullPoint).size() > 0;

        Wire prevShortening = wireBeingShortened;
        wireBeingShortened = null;
        if (pressedOnEndpoint  // Check to see if they are deleting wire
                && pullDir != null
                && !gridAtMouse.equals(pullPoint))
            for (Wire w : currentCircuit.getAllEntitiesOfType(Wire.class).thatInterceptAll(pullPoint, gridAtMouse))
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
            theoreticalCreations = Wire.generateWirePath(pullPoint, gridAtMouse, pullDir, 8);
            if (theoreticalCreations == null) { // If we couldn't do it in their preferred dir, try the other 3
                List<List<Wire.TheoreticalWire>> possiblePaths = new ArrayList<>();
                for (Vector v : Vector.getDirectionVecs())
                    if (!v.equals(pullDir))
                        possiblePaths.add(Wire.generateWirePath(pullPoint, gridAtMouse, v, 8));
                theoreticalCreations = Wire.getShortestPath(possiblePaths);
            }
            theoreticalCreations = theoreticalCreations == null ? new ArrayList<>() : theoreticalCreations;
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
     * @see EditorPanel#drawTheoreticalWires(boolean, Graphics2D)
     */
    ArrayList<CircuitPoint> theoJuncsToDrawAfterWires = new ArrayList<>();

    private void drawTheoreticalWires(boolean calledBeforeDrawWires, Graphics2D g) {
        ArrayList<Wire> allWires = currentCircuit.getAllEntitiesOfType(Wire.class);
        if (calledBeforeDrawWires) {
            theoJuncsToDrawAfterWires = new ArrayList<>();
            for (Wire.TheoreticalWire t: theoreticalCreations) {
                t.draw(g);
                for (Wire w : allWires) {
                    for (CircuitPoint tEdgePoint : new CircuitPoint[] {t.getStartLocation(), t.getEndLocation()})
                        if (w.getPointsExcludingEndpoints().intercepts(tEdgePoint)
                                || (w.isEdgePoint(tEdgePoint) && w.getNumEntitiesConnectedAt(tEdgePoint) > 0)
                                    && w.getNumEntitiesConnectedAt(tEdgePoint) < 2)
                            theoJuncsToDrawAfterWires.add(tEdgePoint);
                    for (CircuitPoint wEdgePoint : new CircuitPoint[] {w.getStartLocation(), w.getEndLocation()}) {
                        if (t.getPointsExcludingEndpoints().intercepts(wEdgePoint)
                                || (t.isEdgePoint(wEdgePoint) && t.getNumEntitiesConnectedAt(wEdgePoint) > 0)
                                    && t.getNumEntitiesConnectedAt(wEdgePoint) < 2)
                            theoJuncsToDrawAfterWires.add(wEdgePoint);
                    }
                }
            }
        } else {
            Wire.TheoreticalWire toDrawJuncs = new Wire.TheoreticalWire(new CircuitPoint(0, 0, currentCircuit),
                    new CircuitPoint(0, 1, currentCircuit));
            for (CircuitPoint p : theoJuncsToDrawAfterWires)
                toDrawJuncs.drawJunction(g, p);
        }

    }

    private Color deletionColor = Color.RED;

    private void drawCurrentDeletion(Graphics2D g) {
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
        for (Entity e : currentCircuit.getAllEntities().thatIntercept(theoEnd))
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
        for (Entity e : currentCircuit.getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    ((Pokable) e).onPoke();
            }
        }
        currentCircuit.refreshTransmissions();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    public void onGridSnapChangeWhileDragging() {
        if (isPullingWire)
            onGridSnapChangeWhilePullingWire();
        // if (movingWire...) { ... ]
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private boolean holdingSpace;

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && (e.getKeyCode() == VK_EQUALS || e.getKeyCode() == VK_MINUS)) {
            CircuitPoint oldCenter = getCenter();
            if (e.getKeyCode() == VK_EQUALS && currentCircuit.canScaleUp()) {
                currentCircuit.scaleUp();
                view(oldCenter);
            } else if (e.getKeyCode() == VK_MINUS && currentCircuit.canScaleDown()) {
                currentCircuit.scaleDown();
                view(oldCenter);
            }
        }
        if (e.getKeyCode() == VK_SPACE)
            holdingSpace = true;
        if (e.getKeyCode() == VK_CONTROL)
            ctrl = true;
        if (e.getKeyCode() == VK_ESCAPE) {
            currSelection.clear();
            currConnectionView.clear();
        }
        if (e.getKeyCode() == VK_P)
            onPoke();
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == VK_SPACE)
            holdingSpace = false;
        if (e.getKeyCode() == VK_CONTROL)
            ctrl = false;
    }
}