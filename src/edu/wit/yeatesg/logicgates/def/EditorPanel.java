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
    public ConnectionSelection currConnectionView;

    public class Selection extends ArrayList<Entity> {

        public Selection(Entity... entities) {
            if (entities != null)
                addAll(Arrays.asList(entities));
        }

        public boolean intercepts(PanelDrawPoint p) {
            for (Entity e : this)
                if (e.getBoundingBox().intercepts(p))
                    return true;
            return false;
        }

        public boolean intercepts(CircuitPoint p) {
            return intercepts(p.toPanelDrawPoint());
        }
    }

    public class ConnectionSelection extends Selection {

        public Timer blinkTimer;
        public boolean blinkState;

        public ConnectionSelection(Entity... entities) {
            super(entities);
            blinkTimer = new Timer(1000, (e) -> blinkState = !blinkState);
        }

        public void draw(Entity inThisSelection, Graphics2D g) {
            if (blinkState)
                inThisSelection.draw(g);
        }
    }

    public void draw(Entity e, Graphics2D g) {
        if (currConnectionView == null || !currConnectionView.contains(e))
            e.draw(g);
        else
            currConnectionView.draw(e, g);
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
        new GateAND(new CircuitPoint(0, 10, currentCircuit)).setRotation(90);
        new GateAND(new CircuitPoint(-10, 10, currentCircuit)).setRotation(90);
        new Wire(new CircuitPoint(2, 2, currentCircuit), new CircuitPoint(2, 10, currentCircuit));
        new Wire(new CircuitPoint(2, 5, currentCircuit), new CircuitPoint(10, 5, currentCircuit));
        new Wire(new CircuitPoint(2, 2, currentCircuit), new CircuitPoint(2, -8, currentCircuit));
        new Wire(new CircuitPoint(0, 0, currentCircuit), new CircuitPoint(5, 0, currentCircuit));
        new Wire(new CircuitPoint(5, 0, currentCircuit), new CircuitPoint(5, 5, currentCircuit));
        new InputBlock(new CircuitPoint(-5, -5, currentCircuit));

        List<Wire.TheoreticalWire> ting = Wire.generateWirePath(new CircuitPoint(-25, -25, currentCircuit),
                new CircuitPoint(-35, -35, currentCircuit), 20);
        System.out.println(ting.size());
        for (Wire.TheoreticalWire w : ting)
            System.out.println(w);


       /* ArrayList<Wire.TheoreticalWire> thing = Wire.getShortestWirePath(new CircuitPoint(-5, -5, currentCircuit),
                new CircuitPoint(-15, -15, currentCircuit),
                new ArrayList<>(),
                Vector.UP,
                1, 5, false, 0);*/

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
            for (Entity e : currentCircuit.getAllEntities())
                draw(e, g);
            drawTheoreticalWires(false, g);

            if (theoreticalDeletion != null)
                drawCurrentDeletion(g);

            if (pullPoint != null && (!isPulling || mouseSnap.equals(pullPoint)))
                drawPullableCircle(g);

            for (Entity e : currSelection)
                e.getBoundingBox().paint(g);


            for (Entity e : currentCircuit.getAllEntities()) {
                if (e.getBoundingBox() != null)
                    e.getBoundingBox().paintSimple(g);
            }


      /*  new BezierCurve((new CurvePolygon(new CircuitPoint(5, 5, currentCircuit),
                new CircuitPoint(10, 8, currentCircuit), new CircuitPoint(15, 20, currentCircuit),
                new CircuitPoint(2, 30, currentCircuit)))).draw(g, currentCircuit.getStroke());*/
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

    private Color hypotheticalPullColor = Color.black;

    private void drawPullableCircle(Graphics2D g) {
        int strokeSize = (int) (currentCircuit.getStrokeSize() * 1.0);
        if (strokeSize % 2 == 0) strokeSize++;
        int circleSize = (int) (currentCircuit.getScale() / 2.5);
  //      System.out.println(canPullFrom + " yellow");

        PanelDrawPoint dp = pullPoint.toPanelDrawPoint();
        g.setStroke(new BasicStroke(strokeSize));
        g.setColor(hypotheticalPullColor);
        if (wireShortening != null)
            g.setColor(Color.red);
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
        if (!isPulling) {
            CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
            CircuitPoint prevCanPullFrom = pullPoint;
            pullPoint = null;
            findPotential: for (ConnectibleEntity ce : currentCircuit.getAllEntitiesOfType(ConnectibleEntity.class)) {
                if (ce.canPullConnectionFrom(gridSnapAtMouse)) {
                    for (Entity e : currSelection)
                        if (e.intercepts(gridSnapAtMouse))
                            break findPotential;
                    pullPoint = gridSnapAtMouse;
                    pullDir = null;
                    repaint();
                    break;
                }
            }
            boolean pullPointChanged = prevCanPullFrom == null && pullPoint != null
                    || prevCanPullFrom != null && pullPoint == null;
            if (pullPointChanged)
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
    public void determineSelecting() {
        PanelDrawPoint atMouse = panelDrawPointAtMouse();
        if (isPulling) {
            currSelection.clear();
        } else {
            if (!currSelection.isEmpty() && !currSelection.intercepts(atMouse)) {
                currSelection.clear();
                System.out.println("P E CONGLOMERATE");
            }
            else if (!currSelection.isEmpty())
                movingSelection = true;

            if (currSelection.isEmpty() || ctrl) {
                ArrayList<Entity> potentialClickSelection = new ArrayList<>();
                System.out.println("YA FUCK WHITE");
                for (Entity e : currentCircuit.getAllEntities())
                    if (e.getBoundingBox() != null && e.getBoundingBox().intercepts(atMouse))
                        potentialClickSelection.add(e);
                if (potentialClickSelection.size() > 0)
                    currSelection.addAll(potentialClickSelection);
                else if (!ctrl) {
                    // Start creating selection box
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent ev) {
        updateMousePos(ev);
        pressPointGrid = circuitPointAtMouse(true);
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

        if (pressPointGrid.equals(releasePointGrid) && (currSelection.isEmpty() || ctrl)) {
            for (Wire w : currentCircuit.getAllEntitiesOfType(Wire.class))
                if (w.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    currSelection.add(w);
            pullPoint = null;
        }


        repaint();

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        updateMousePos(e);
 //       LogicGates.debug("MOUSE CLICK", "", "PANEL", panelDrawPointAtMouse(), "CIRCUIT", circuitPointAtMouse(false),
  //              "SNAP", circuitPointAtMouse(true));
    }


    private CircuitPoint pullPoint = null;
    private boolean isPulling = false;
    private Vector pullDir = null;
    private boolean pressedOnEndpoint = false;
    private Wire wireShortening = null;
    private Wire.TheoreticalWire theoreticalDeletion = null;
    private List<Wire.TheoreticalWire> theoreticalCreations = new ArrayList<>();

    public void determineIfPullingWire() {
        pressedOnEndpoint = false;
        if (pullPoint != null) {
            onStartPulling();
        }
    }

    private void onStartPulling() {
        isPulling = true;
        for (Wire w : currentCircuit.getAllEntitiesOfType(Wire.class))
            if (w.isEdgePoint(pullPoint))
                pressedOnEndpoint = true;
    }

    private void onGridSnapChangeWhilePulling() {
        System.out.println("GS SNAP");
        CircuitPoint gridAtMouse = circuitPointAtMouse(true);
        if (pullDir == null) { // If a preferred pull dir hasn't been chosen by the user yet, set it
            Vector dir = new Vector(pullPoint, gridAtMouse);
            if (!(dir.equals(Vector.ZERO_VECTOR)) && Vector.getDirectionVecs().contains(dir))
                pullDir = dir;
        }

        boolean isSameEntity = false;
        for (Entity ent : currentCircuit.getAllEntities()) {
            if (ent.interceptsAll(gridAtMouse, pullPoint)) {
                isSameEntity = true;
                break;
            }
        }

        Wire prevShortening = wireShortening;
        wireShortening = null;
        if (pressedOnEndpoint  // Check to see if they are currently deleting or adding
                && pullDir != null)
            for (Wire w : currentCircuit.getAllEntitiesOfType(Wire.class))
                if (w.isEdgePoint(pullPoint) && w.interceptsAll(pullPoint, gridAtMouse)
                    && !gridAtMouse.equals(pullPoint))
                    wireShortening = w;

        if (wireShortening == null // If they move the pullpoint to another spot by mousing over an adjacent spot
                && pullDir != null // that is on the same entity that they originally pulled from
                && !gridAtMouse.equals(pullPoint)
                && pullPoint.is4AdjacentTo(gridAtMouse)
                && (isSameEntity)) {
            pullPoint = gridAtMouse;
            pullDir = null;
            theoreticalCreations = new ArrayList<>();
        }

        boolean wentFromDeletingToNotDeleting = prevShortening != null && wireShortening == null;
        if (wentFromDeletingToNotDeleting) {
            // Pullpoint is the endpoint of the wiere they were originally deleting from
            if (pullPoint.is4AdjacentTo(gridAtMouse)) {
                pullDir = new Vector(pullPoint, gridAtMouse);
            } else {
                // Find a new direction tht makes the most sense
            }
        }

        if (wireShortening == null)
            theoreticalDeletion = null;

        if (wireShortening != null && !gridAtMouse.equals(pullPoint)) {
            theoreticalCreations = new ArrayList<>();
            theoreticalDeletion = new Wire.TheoreticalWire(pullPoint, gridAtMouse);
        }

        if (wireShortening == null && pullPoint != null && !isSameEntity) {
            theoreticalCreations = Wire.generateWirePath(pullPoint, gridAtMouse, pullDir, 8);
            if (theoreticalCreations == null) { // If we couldn't do it in their preferred dir, try the other 3
                List<List<Wire.TheoreticalWire>> possiblePaths = new ArrayList<>();
                for (Vector v : Vector.getDirectionVecs())
                    if (!v.equals(pullDir))
                        possiblePaths.add(Wire.generateWirePath(pullPoint, gridAtMouse, v, 8));
                theoreticalCreations = Wire.getShortestPath(possiblePaths);
            }
            theoreticalCreations = theoreticalCreations == null ? new ArrayList<>() : theoreticalCreations;
            for (Wire w : theoreticalCreations) {
                System.out.println(w + "           NEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
            }
        }

        // If they went back to pull point with mouse after giving it an initial vec
        if (gridAtMouse.equals(pullPoint) && wireShortening == null && pullDir != null) {
            pullDir = null;
            theoreticalCreations = new ArrayList<>();
        }
        repaint();
    }

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
            if (wireShortening.isEdgePoint(edgePoint)) {
                theoreticalDeletion.setColor(backgroundColor);
                theoreticalDeletion.drawJunction(g, edgePoint);
                int numConnects = wireShortening.getNumWiresConnectedAt(edgePoint);
                for (Wire w : wireShortening.getWiresConnectedAt(edgePoint))
                    w.draw(g, numConnects == 3);
            }
        }

        // Next, draw the red deletion indication wire
        theoreticalDeletion.setColor(deletionColor);
        theoreticalDeletion.draw(g, false);

        // Next, overlap any possible white spaces with the original intercepting wires (don't draw the junctions tho)
        for (CircuitPoint edgePoint : new CircuitPoint[] { theoStart, theoEnd })
            if (wireShortening.isEdgePoint(edgePoint))
                for (Wire w : wireShortening.getWiresConnectedAt(edgePoint))
                    w.draw(g, false);

        for (Entity e : currentCircuit.getEntitiesThatIntercept(theoEnd))
            if (e instanceof Wire && ((Wire) e).getDirection() != wireShortening.getDirection()
                && ((Wire) e).getNumEntitiesConnectedAt(theoEnd) == 0)
                ((Wire) e).drawJunction(g, theoEnd);
    }

    private void onStopPulling() {
        CircuitPoint gridSnapAtMouse = circuitPointAtMouse(true);
        if (wireShortening != null) {
            if (wireShortening.intercepts(gridSnapAtMouse)) {
                wireShortening.set(pullPoint, gridSnapAtMouse);
                // Merge/Other checks need to occur at both endpoints of a modified wire if it was shortened
                // REMEMBER: Merge does a delete of its own, and that isn't going to be reflected in the
                // currentCircuit.getEntitiesThatIntercept(edgePoint) call below since the list is made before
                // the merge. So I added a field 'deleted' to wire. I'll probably move it to Entity because it
                // might have more uses later
                for (CircuitPoint edgePoint : new CircuitPoint[] {pullPoint, gridSnapAtMouse}) {
                    for (Entity e : currentCircuit.getEntitiesThatIntercept(edgePoint)) {
                        if (e instanceof ConnectibleEntity && !e.isDeleted()) {
                            ((ConnectibleEntity) e).connectCheck();
                            if (e instanceof Wire) {
                                ((Wire) e).bisectCheck();
                                ((Wire) e).mergeCheck();
                            }
                            ((ConnectibleEntity) e).connectCheck();
                        }
                    }
                }

            }
            wireShortening = null;
        }

        pressedOnEndpoint = false;
        wireShortening = null;
        isPulling = false;
        pullPoint = null;
        pullDir = null;
        theoreticalDeletion = null;
        if (theoreticalCreations != null && !theoreticalCreations.isEmpty()) {
            for (Wire.TheoreticalWire w : theoreticalCreations) {
                new Wire(w.getStartLocation(), w.getEndLocation());
                for (CircuitPoint edgePoint : new CircuitPoint[] { w.getStartLocation(), w.getEndLocation() }) {
                    for (Entity e : currentCircuit.getEntitiesThatIntercept(edgePoint)) {
                        if (e instanceof ConnectibleEntity) {
                            ((ConnectibleEntity) e).connectCheck();
                            if (e instanceof Wire) {
                                ((Wire) e).bisectCheck();
                                ((Wire) e).mergeCheck();
                            }
                            ((ConnectibleEntity) e).connectCheck();
                        }
                    }
                }
            }
            theoreticalCreations = new ArrayList<>();

            // do checks on endpoints of each theo
        }
        updatePossiblePullPoint();
    }

    public void onPoke() {
        for (Entity e : currentCircuit.getAllEntities()) {
            if (e instanceof Pokable) {
                if (e.getBoundingBox().intercepts(panelDrawPointAtMouse()))
                    ((Pokable) e).onPoke();
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


    public void onGridSnapChangeWhileDragging() {
        Circuit c = currentCircuit;
        CircuitPoint gridAtMouse = circuitPointAtMouse(true);

        // Everything under this if-block is associated with creating/deleting wires
        if (isPulling)
            onGridSnapChangeWhilePulling();


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
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == VK_SPACE)
            holdingSpace = false;
    }
}