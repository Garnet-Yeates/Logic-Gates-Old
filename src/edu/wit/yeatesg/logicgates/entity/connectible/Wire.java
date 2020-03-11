package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.entity.PropertyList;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.*;

public class Wire extends ConnectibleEntity implements Dependent {

    protected CircuitPoint startLocation;
    protected CircuitPoint endLocation;


    public Wire(CircuitPoint startLocation, CircuitPoint endLocation, boolean addToCircuit) {
        super(startLocation.getCircuit(), addToCircuit);
        System.out.println("new wire: " + startLocation + " " + endLocation + " add to circ? " + addToCircuit);
        if ((startLocation.x != endLocation.x && startLocation.y != endLocation.y) || startLocation.equals(endLocation))
            throw new RuntimeException("Invalid Wire");
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        updateInterceptPoints();
        postInit(addToCircuit);
    }

    @Override
    public void postInit(boolean addToCircuit) {
        if (addToCircuit) {
            for (Wire w : c.getAllEntitiesOfType(Wire.class)) {
                if (isSimilar(w))
                    throw new RuntimeException("Duplicate/Similar Wire on Circuit " + startLocation.getCircuit().getCircuitName() + " "
                            + startLocation + " " + endLocation);
            }
        }
        super.postInit(addToCircuit);
    }


    /**
     * Returns the lefter edge point of this wire is horizontal, returns the higher edge point of this wire if
     * vertical
     * @return
     */
    public CircuitPoint getFirstEdgePoint() {
        return getDirection() == Direction.HORIZONTAL ? (
                startLocation.x < endLocation.x ? startLocation : endLocation)
                : (startLocation.y < endLocation.y ? startLocation : endLocation);
    }

    public CircuitPoint getSecondEdgePoint() {
        return getOppositeEdgePoint(getFirstEdgePoint());
    }

    @Override
    public Wire getSimilarEntity() {
        return new Wire(startLocation.clone(c), endLocation.clone(c), false);
    }

    public int getLength() {
        return interceptPoints.size();
    }

    public boolean eats(Wire other) {
        return intercepts(other.getStartLocation()) && intercepts(other.getEndLocation()) && getLength() > other.getLength();
    }

    public Wire(CircuitPoint startLocation, CircuitPoint endLocation) {
        this(startLocation, endLocation, true);
    }

    protected Wire(Circuit c) {
        super(c, false);
    }

    @Override
    public Wire clone(Circuit onto) {
        return new Wire(startLocation.clone(onto), endLocation.clone(onto));
    }


    // INHERITED FROM DEPENDENT CLASS

    protected State state;
    private DependentParentList dependencyList = new DependentParentList(this);

    @Override
    public State getState() {
        return state;
    }

    @Override
    public DependentParentList getDependencyList() {
        return dependencyList;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public void determinePowerStateOf(OutputNode outputNode) { /* Not Implemented For Wire, Has Volatile Nodes */ }

    @Override
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return intercepts(gridSnap);
    }

    @Override
    protected void determineOutputDependencies() { /* Not Implemented For Wire, Has Volatile Nodes */ }


    @Override
    public String getDisplayName() {
        return "Wire";
    }

    /**
     * Obtains a direct reference to this Wire's interceptPoints
     * @return a PointSet representing all of the intercept points of this Wire
     */
    @Override
    public PointSet getInterceptPointsRef() {
        return interceptPoints;
    }

    private PointSet interceptPoints;

    public void updateInterceptPoints() {
        interceptPoints = new PointSet();
        if (startLocation.x == endLocation.x) {
            CircuitPoint lower = startLocation.y > endLocation.y ? startLocation : endLocation;
            CircuitPoint higher = lower == startLocation ? endLocation : startLocation;
            for (int y = (int) higher.y; y <= lower.y; y++)
                interceptPoints.add(new CircuitPoint(higher.x, y, c));
        } else {
            CircuitPoint lefter = startLocation.x > endLocation.x ? endLocation : startLocation;
            CircuitPoint righter = lefter == startLocation ? endLocation : startLocation;
            for (int x = (int) lefter.x; x <= righter.x; x++)
                interceptPoints.add(new CircuitPoint(x, lefter.y, c));
        }
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        PointSet interceptPoints = getInterceptPoints(e);
        PointSet invalidPoints = new PointSet();
        if (interceptPoints.size() > 1)
            return interceptPoints; // If it hits the same entity twice, ALL points are invalid
        if (e instanceof Wire) {
            Wire other = (Wire) e;
            if (other.getDirection() != getDirection()) {
                for (CircuitPoint interceptPoint : interceptPoints)
                    if (!(other.isEdgePoint(interceptPoint) || isEdgePoint(interceptPoint)))
                        invalidPoints.add(interceptPoint);
            } else {
                for (CircuitPoint interceptPoint : interceptPoints)
                    if (!(other.isEdgePoint(interceptPoint) && isEdgePoint(interceptPoint)))
                        invalidPoints.add(interceptPoint);
            }
            return invalidPoints;
        } else if (e instanceof ConnectibleEntity) {
            ConnectibleEntity ce = (ConnectibleEntity) e;
            if (interceptPoints.size() == 1)
                if (!canConnectTo(ce, interceptPoints.get(0)) || !ce.canConnectTo(this, interceptPoints.get(0)))
                    return interceptPoints; // If it hits the entity once, but cant connect, it's invalid
        }
        // TODO if i add non connectible entitties (like labels and stuff) if i want them to be invalid at some points add it here
        return invalidPoints; // InvalidPoints is empty here
    }

    @Override
    public PointSet getInterceptPoints(Entity other) {
        if (other instanceof Wire) {
            if (((Wire) other).getDirection() == getDirection())
                return other.getInterceptPoints(false).intersection(getInterceptPoints(false));
            else {
                PointSet interceptPoints = new PointSet();
                for (CircuitPoint edgePoint : getEdgePoints())
                    for (CircuitPoint otherPoint : other.getInterceptPoints(false))
                        if (edgePoint.equals(otherPoint) && !interceptPoints.contains(otherPoint))
                            interceptPoints.add(edgePoint);
                for (CircuitPoint otherEdgePoint : ((Wire) other).getEdgePoints())
                    for (CircuitPoint thisPoint : getInterceptPoints(false))
                        if (otherEdgePoint.equals(thisPoint) && !interceptPoints.contains(thisPoint))
                            interceptPoints.add(thisPoint);
                return interceptPoints;
            }
        }
        return super.getInterceptPoints(other);
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(startLocation, endLocation, this);

    }

    public Direction getDirection() {
        return startLocation.x == endLocation.x ? Direction.VERTICAL : Direction.HORIZONTAL;
    }

    public PointSet getPointsExcludingEdgePoints() {
        PointSet pts = getInterceptPoints(true);
        pts.remove(0);
        pts.remove(pts.size() - 1);
        return pts;
    }


    // Connect checking stuff

    @Override
    public void connectCheck() {
        if (deleted || isInvalid())
            return;
        bisectCheck();
        mergeCheck();
        super.connectCheck();
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {
        if (deleted || e.isDeleted())
            return;
        for (CircuitPoint edgePoint : getEdgePoints()) {
            if (canConnectTo(e, edgePoint) && e.canConnectTo(this, edgePoint) && !deleted && !e.isDeleted())
                connect(e, edgePoint);
        }
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        if (isEdgePoint(at)
                && !hasConnectionTo(e)
                && connections.size() < 4
                && !e.isDeleted()) {
            if (e instanceof Wire) {
                Wire other = (Wire) e;
                return getDirection() != other.getDirection() || other.getNumOtherEdgePointsAt(at) > 1;
            } else
                return true;
        }
        return false;
    }


    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (isInvalid() || deleted || e.isInvalid() || e.isDeleted())
            throw new RuntimeException("Cannot connect. At least one of these wires is deleted/invalid/preview");
        if (!atLocation.equals(startLocation) && !atLocation.equals(endLocation))
            throw new RuntimeException("Wires can only be connected at end points! (w1)");
        if (hasConnectionTo(e) || e.hasConnectionTo(this))
            throw new RuntimeException(this + " is Already Connected To " + e);
        if (!canConnectTo(e, atLocation) || !e.canConnectTo(this, atLocation))
            throw new RuntimeException("canConnectTo returned false");
        if (e instanceof Wire) {
            Wire w = (Wire) e;
            if (!atLocation.equals(w.startLocation) && !atLocation.equals(w.endLocation))
                throw new RuntimeException("Wires can only be connected at end points! (w2)");
            if (w.getDirection() == getDirection() && w.getNumOtherEdgePointsAt(atLocation) == 1)
                throw new RuntimeException("These wires cannot connect because they need to be merged");
            connections.add(new ConnectionNode(atLocation, this, e));
            e.connections.add(new ConnectionNode(atLocation, e, this));
        } else if (e instanceof InputBlock) {
            InputBlock block = (InputBlock) e;
            connections.add(new ConnectionNode(atLocation, this, e));
            block.getNodeAt(atLocation).connectedTo = this;
            // Handle other cases...
        } else if (e instanceof SimpleGateAND) {
            SimpleGateAND gate = (SimpleGateAND) e;
            connections.add(new ConnectionNode(atLocation, this, e));
            gate.getNodeAt(atLocation).connectedTo = this;
        }
        c.refreshTransmissions();
    }


    /**
     *  Special case for added wires: If the added wire w1 has an edge point that touches w2 at any non-edge point,
     *  then w2 should be split into 2 wires around that point
     */
    public static class WireBisect {

        private Circuit c;
        private Wire w1;
        private Wire w2;
        private CircuitPoint bisectPoint;
        private Wire[] resultingWires = new Wire[2];

        public WireBisect(Wire w1, Wire w2, CircuitPoint bisectPoint) {
            if (w1.isInvalid() || w1.deleted || w2.isInvalid() || w2.deleted)
                throw new RuntimeException("At least one of the Wires is deleted or invalid or any combo of those");
            if (w1.getCircuit() != w2.getCircuit()
                    || !w1.getEdgePoints().contains(bisectPoint)
                    || w2.getEdgePoints().contains(bisectPoint)
                    || !w2.interceptsExcludingEdgePoints(bisectPoint))
                throw new RuntimeException("Invalid Wire Bisect");
            this.w1 = w1;
            this.w2 = w2;
            this.bisectPoint = bisectPoint;
            this.c = w1.getCircuit();
        }

        public void doBisect() {
            CircuitPoint oldStartLoc = w2.startLocation;
            w2.disconnectAll();
            w2.set(w2.startLocation, bisectPoint, false);
            resultingWires[0] = new Wire(w2.getStartLocation(), w2.getEndLocation(), false);
            Wire created = new Wire(bisectPoint, oldStartLoc);
            resultingWires[1] = new Wire(created.getStartLocation(), created.getEndLocation(), false);
            w2.connectCheck();
            w1.connectCheck();
            c.refreshTransmissions();
            c.getEditorPanel().repaint(c);
        }

        public LinkedList<Wire> getSimilarResultingWires() {
            return new LinkedList<>(Arrays.asList(resultingWires));
        }

    }

    public WireBisect getBisect(Wire other) {
        if (isInvalid() || deleted || other.isInvalid() || other.deleted)
            return null;
        for (CircuitPoint thisWiresEndpoint : new CircuitPoint[]{startLocation, endLocation}) {
            if (other.interceptsExcludingEdgePoints(thisWiresEndpoint)
                    && other.getDirection() != getDirection()) // This means it is bisecting the wire
                return new WireBisect(this, other, thisWiresEndpoint);
        }
        return null;
    }

    public void bisectCheck() {
        if (deleted || isInvalid())
            return;
        ArrayList<Wire> allWires = c.getAllEntitiesOfType(Wire.class, false);
        for (Wire w : allWires) {
            if (!w.deleted && !w.equals(this)) {
                bisectCheck(w);
                w.bisectCheck(this);
            }
        }

    }

    public WireBisect bisectCheck(Wire other) {
        if (isInvalid() || deleted || other.isInvalid() || other.deleted)
            return null;
        for (CircuitPoint thisWiresEndpoint : new CircuitPoint[]{startLocation, endLocation}) {
            if (other.getPointsExcludingEdgePoints().contains(thisWiresEndpoint)
                    && other.getDirection() != getDirection()) {// This means it is bisecting the wire
                WireBisect bisect = new WireBisect(this, other, thisWiresEndpoint);
                bisect.doBisect();
                return bisect;
            }
        }
        return null;
    }

    public boolean isSimilar(Entity e) {
        if (!(e instanceof Wire))
            return false;
        Wire w = (Wire) e;
        return (w.startLocation.equals(startLocation) && w.endLocation.equals(endLocation))
                || (w.startLocation.equals(endLocation) && w.endLocation.equals(startLocation));
    }


    // Merge checks for Wires

    public static class WireMerge {

        private Circuit c;

        private Wire w1;
        private Wire w2;

        private CircuitPoint common;

        private CircuitPoint w1p;
        private CircuitPoint w2p;

        public WireMerge(Wire w1, Wire w2, CircuitPoint w1p, CircuitPoint common, CircuitPoint w2p) {
            if (w1.c != w2.c)
                throw new RuntimeException("These 2 wires are on a different Circuit in memory");
            if (!w1.getEdgePoints().contains(common) || !w2.getEdgePoints().contains(common))
                throw new RuntimeException("These 2 wires do not share a common edge point");
            if (w1.isInvalid() || w1.deleted || w2.isInvalid() || w2.deleted)
                throw new RuntimeException("At least one of the Wires is deleted or invalid or any combo of those");
            this.c = w1.getCircuit();
            this.w1p = w1p;
            this.w2p = w2p;
            this.w1 = w1;
            this.w2 = w2;
            this.common = common;
        }

        public void doMerge() {
            w2.delete(); // Checks done in delete
            w1.set(common, w2.getOppositeEdgePoint(common)); // Checks done in set
            c.refreshTransmissions();
            c.getEditorPanel().repaint(c);
        }

        public Wire getWireSimilarToResult() {
            return new Wire(w1p, w2p, false);
        }

        public Wire getW1() {
            return w1;
        }

        public Wire getW2() {
            return w2;
        }

        public CircuitPoint getSharedPoint() {
            return common;
        }

        public CircuitPoint getW1OtherPoint() {
            return w1p;
        }

        public CircuitPoint getW2OtherPoint() {
            return w2p;
        }
    }

    public WireMerge getMerge(Wire other) {
        if (isInvalid() || deleted || other.isInvalid() || other.deleted)
            return null;
        for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation}) {
            for (CircuitPoint otherEdgePoint : new CircuitPoint[]{other.startLocation, other.endLocation}) {
                if (edgePoint.equals(otherEdgePoint)
                        && other.getDirection() == getDirection()
                        && !isSimilar(other)
                        && !(other.deleted || deleted)
                        && (other.getNumOtherEdgePointsAt(edgePoint) < 2)) { // TODO getNumOtherEdgePoints has n efficiency, not so great. Maybe later when disconnectAll() is calle, i can cache a list of the old ConnectionNodes (ONLY clone the node themselves, not their connectedto or any other data). And use those to check the required minumum # connections
                    return new WireMerge(this, other, this.getOppositeEdgePoint(edgePoint), edgePoint, other.getOppositeEdgePoint(edgePoint));
                }
            }
        }
        return null;
    }

    public void mergeCheck() {
        if (deleted || isInvalid())
            return;
        ArrayList<Wire> allWires = c.getAllEntitiesOfType(Wire.class, false);
        for (Wire w : allWires)
            if (!w.deleted && !w.equals(this))
                mergeCheck(w);
    }

    public void mergeCheck(Wire other) {
        if (isInvalid() || deleted || other.isInvalid() || other.deleted)
            return;
        for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation}) {
            for (CircuitPoint otherEdgePoint : new CircuitPoint[]{other.startLocation, other.endLocation}) {
                if (edgePoint.equals(otherEdgePoint)
                        && other.getDirection() == getDirection()
                        && !isSimilar(other)
                        && !(other.deleted || deleted)
                        && (other.getNumOtherEdgePointsAt(edgePoint) < 2)) {
                    WireMerge merge = new WireMerge(this, other,
                            this.getOppositeEdgePoint(edgePoint), edgePoint, other.getOppositeEdgePoint(edgePoint));
                    merge.doMerge();
                }
            }
        }
    }

    /**
     * Extremely inefficient method. If this were to be called on every Entity in the Circuit, the efficiency will
     * be N^2. The only reason this method is necessary is because sometimes we need to check lower bounds of
     * connected entities. For example, in order for a wire to merge with another wire, the number of connections
     * at the shared point for each of the wires must be 1. If there are 2 connections there, then it is bisected.
     * Normally I would just use getNumEntitiesConnectedAt(), but this isn't possibl with lower bound checking right now
     * because when connect/merge/bisectCheck is called, ALL entities are disconnected, so almost 100% of the time
     * the lower bound condition willl return true. That is why for lower bounds I use getNumEdgePoints at. For upper
     * bound checks it doesnt matter because you are checking a maximum so you can use getNumentitiesConnected at.
     * Potential Solution: When disconnectAll() is called, save a list of the connection locations and use that.
     * @param edgeOfThisWire
     * @return
     */
    public int getNumOtherEdgePointsAt(CircuitPoint edgeOfThisWire) {
        int num = 0;
        ArrayList<Wire> otherWires = c.getAllEntitiesOfType(Wire.class).except(this);
        for (Wire w : otherWires)
            if (w.startLocation.equals(edgeOfThisWire) || w.endLocation.equals(edgeOfThisWire))
                num++;
        return num;
    }

    @Override
    public boolean canPullConnectionFrom(CircuitPoint locationOnThisEntity) {
        for (Entity e : getCircuit().getAllEntities())
            if (e instanceof Wire
                    && !e.equals(this)
                    && e.intercepts(locationOnThisEntity)
                    && ((Wire) e).getPointsExcludingEdgePoints().intersection(getPointsExcludingEdgePoints())
                        .contains(locationOnThisEntity))
                return false;
        return getNumEntitiesConnectedAt(locationOnThisEntity) < 3;
    }

    @Override
    public void disconnect(ConnectibleEntity e) {
        connections.remove(getConnectionTo(e));
    }


    public CircuitPoint getStartLocation() {
        return startLocation;
    }

    public CircuitPoint getEndLocation() {
        return endLocation;
    }

    public LinkedList<CircuitPoint> getEdgePoints() {
        LinkedList<CircuitPoint> edgePoints = new LinkedList<>();
        edgePoints.add(startLocation);
        edgePoints.add(endLocation);
        return edgePoints;
    }

    public String whichEdgePoint(CircuitPoint p) {
        return p.equals(startLocation) ? "start" : (p.equals(endLocation) ? "end" : "none");
    }

    public boolean isEdgePoint(CircuitPoint location) {
        return !whichEdgePoint(location).equals("none");
    }

    public CircuitPoint getOppositeEdgePoint(CircuitPoint p) {
        if (!isEdgePoint(p))
            throw new RuntimeException();
        if (whichEdgePoint(p).equals("start"))
            return endLocation;
        else
            return startLocation;

    }

    public void set(CircuitPoint edgePoint, CircuitPoint to) {
        set(edgePoint, to, true);
    }

    public void set(CircuitPoint edgePoint, CircuitPoint to, boolean checkAfter) {
        if (!isEdgePoint(edgePoint))
            throw new RuntimeException("Set must be called on the edgePoint of a wire");
        if (to.equals(getOppositeEdgePoint(edgePoint))) {
            delete();
        } else if (whichEdgePoint(edgePoint).equals("start")) {
            startLocation = to;
        } else if (whichEdgePoint(edgePoint).equals("end"))
            endLocation = to;
        updateInterceptPoints();
        if (checkAfter)
            checkEntities(edgePoint, startLocation, endLocation, to);
        c.refreshTransmissions();
    }

    @Override
    public void onDelete() {
        disconnectAll();
        deleted = true;
        checkEntities(startLocation, endLocation);
        c.refreshTransmissions();
    }

    public Color getColor() {
        try {
            return getState().getColor();
        } catch (Exception e) {e.printStackTrace(); System.exit(0);}
        return null;
    }

    public void draw(GraphicsContext g, boolean drawJunctions) {
        g.setStroke(getColor());
        g.setImageSmoothing(false);
        g.setLineWidth(getLineWidth());
        PanelDrawPoint p1 = startLocation.toPanelDrawPoint();
        PanelDrawPoint p2 = endLocation.toPanelDrawPoint();
        g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        if (drawJunctions)
            for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation})
                if (getNumEntitiesConnectedAt(edgePoint) > 1)
                    drawJunction(g, edgePoint);
    }

    public void drawJunction(GraphicsContext g, CircuitPoint loc) {
        g.setStroke(getColor());
        g.setFill(getColor());
        int circleSize = (int) (getLineWidth() * 2.3);
        if (circleSize % 2 != 0) circleSize++;
        PanelDrawPoint dp = loc.toPanelDrawPoint();
        g.fillOval(dp.x - circleSize / 2.00, dp.y - circleSize / 2.00, circleSize, circleSize);
    }

    @Override
    public void draw(GraphicsContext g) {
        draw(g, true);
    }

    @Override
    public int getLineWidth() {
        return (int) (c.getLineWidth() * 1.8);
    }


    /**
     * More efficient than doing getPointsExcludingEdgePoints.contains(circuitPoint) because
     * it doesn't have to clone
     * @param circuitPoint
     * @return
     */
    public boolean interceptsExcludingEdgePoints(CircuitPoint circuitPoint) {
        for (int i = 1; i < interceptPoints.size() - 1; i++)
            if (interceptPoints.get(i).equals(circuitPoint))
                return true;
        return false;
    }


    @Override
    public String toString() {
        return "Wire{" +
                "start=" + startLocation +
                ", end=" + endLocation +
                '}';
    }

    @Override
    public String toParsableString() {
        return "[Wire]" + startLocation.toParsableString() + "," + endLocation.toParsableString();
    }

    public static CircuitPoint getPointInLineWith(CircuitPoint start, CircuitPoint end, Direction prefDir) {
        if (start.equals(end))
            throw new RuntimeException(start + " is equal to and therefore already in line with " + end);
        if (start.isInLineWith(end))
            throw new RuntimeException(start + " is already in line with " + end);
        CircuitPoint inLine;
        inLine = prefDir == Direction.HORIZONTAL // Will be equal to 'start' if it is already in line but the prefDir
                ? new CircuitPoint(end.x, start.y, start.getCircuit()) // is perpendicular. If they are in line and
                : new CircuitPoint(start.x, end.y, start.getCircuit()); // the dir is parallel, it will be 'end'
       return inLine;
    }

    public static Vector directionVecFromInLinePoints(CircuitPoint start, CircuitPoint end) {
        if (!start.isInLineWith(end))
            throw new RuntimeException("Points must be in straight line with each other to generate a direction vector");
        if (start.equals(end))
            throw new RuntimeException("These points are equal");
        if (start.x == end.x)
            return start.y > end.y ? Vector.UP : Vector.DOWN;
        else
            return start.x > end.x ? Vector.LEFT : Vector.RIGHT;
    }


    // If u dont want a wire to intercept a wire in a way where it will connect to it, use this method
    @Override
    public boolean doesGenWireInvalidlyInterceptThis(TheoreticalWire theo, PermitList exceptions, boolean strictWithWires) {
        exceptions = new PermitList(exceptions); // Clone exceptions
   //     System.out.println("Does theo " + theo + " invalidlyIntercept " + this + " ? STRICT WITH WIRES: " + strictWithWires);
        if (!strictWithWires)
            if (getDirection() != theo.getDirection())
                for (CircuitPoint p : theo.getPointsExcludingEdgePoints())
                    exceptions.add(new InterceptPermit(this, p));
 //       LogicGates.debug("Exceptions:",  exceptions);
        if (theo.invalidlyIntercepts(this)) {
 //           System.out.println("invalidly intercepts");
            return true;
        } else {
    //        LogicGates.debug("InterceptPoints", theo.getInterceptPoints(this));
            for (CircuitPoint p : theo.getInterceptPoints(this)) {
                InterceptPermit requiredExceptin = new InterceptPermit(this, p);
                if (!exceptions.contains(requiredExceptin)) {
       //             System.out.println("exceptions didn't contain an exception similar to " + requiredExceptin);
                    return true;
                }
            }
        }
        return false;
    }




    @Override
    public boolean canMove() {
        return false;
    }

    @Override
    public String getPropertyTableHeader() {
        return null;
    }

    @Override
    public PropertyList getPropertyList() {
        return null;
    }

    @Override
    public void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1) {

    }

    @Override
    public boolean hasProperty(String propertyName) {
        return false;
    }


    public static class WireGenerator {

        /** For multi-threading. This is so we can cancel the generation when it's obsolete, and not use too many resources */
        private boolean cancelled;

        /** 1 means the algorithm is efficient and weak, 2 means it is less efficient but much smarter */
        private int genOrder;

        private static final int SPLIT_DIST = 3;
        private static final int OVERSHOOT_DIST = 5;
        private static final int MAX_SPLITS = 2;

        public WireGenerator(int genOrder) {
            if (genOrder != 1 && genOrder != 2)
                throw new RuntimeException("Invalid Order Of Generation");
            this.genOrder = genOrder;
        }

        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }

        public void setCancelled(boolean cancelled, String msg) {
            this.cancelled = cancelled;
            System.out.println(msg);
        }

        public ArrayList<TheoreticalWire> genWirePathStrict(CircuitPoint start,
                                                            CircuitPoint end,
                                                            Direction initialDir,
                                                            int maxLength) {
            return genWirePath(start, end, initialDir, maxLength, genOrder, true);

        }

        public ArrayList<TheoreticalWire> genWirePathLenient(CircuitPoint start,
                                                             CircuitPoint end,
                                                             Direction initialDir,
                                                             int maxLength) {
            return genWirePath(start, end, initialDir, maxLength, genOrder, false);
        }

        public ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                             CircuitPoint end,
                                                             Direction initialDir,
                                                             int maxLength,
                                                             int order,
                                                             boolean strictWithWires) {
            PermitList permitList = new PermitList();
            int expansion = (SPLIT_DIST + OVERSHOOT_DIST) * order;
            BoundingBox scopeBox = new BoundingBox(start, end, null);
            scopeBox = scopeBox.getExpandedBy(expansion);
            EntityList<Entity> scope = scopeBox.getInterceptingEntities();
            return genWirePath(start, end, new ArrayList<>(), scope, initialDir, maxLength, 0, order, permitList, strictWithWires);
        }

        private ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                              CircuitPoint end,
                                                              ArrayList<TheoreticalWire> currPath,
                                                              EntityList<? extends Entity> scope,
                                                              Direction currDirection,
                                                              int maxLength,
                                                              int splitNum,
                                                              int tryAllTillLength,
                                                              PermitList permits,
                                                              boolean strictWithWires) {
            // Termination case
            if (currPath.size() > maxLength || cancelled)
                return null;

            // Add intercept permits
            if (currPath.size() == 0) // Add permit to start loc so the wires can intercept, if first path
                for (Entity e : start.getCircuit().getAllEntitiesThatIntercept(start).ofType(ConnectibleEntity.class))
                    permits.add(new InterceptPermit(e, start));
            for (ConnectibleEntity ce : start.getCircuit().getAllEntitiesThatIntercept(end).ofType(ConnectibleEntity.class))
                permits.add(new InterceptPermit(ce, end)); // Always add permit to end loc

            // No supplied direction: try 2 directions in this case, use shortest of both
            if (currDirection == null) {
                ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                paths.add(genWirePath(start, end, currPath, scope, Direction.HORIZONTAL, maxLength, splitNum,
                        tryAllTillLength, permits, strictWithWires));
                paths.add(genWirePath(start, end, currPath, scope, Direction.VERTICAL, maxLength, splitNum,
                        tryAllTillLength, permits, strictWithWires));
                return getShortestPath(paths);
            }

            // If start is in line with end
            if (start.isInLineWith(end)) {
                currDirection = directionVecFromInLinePoints(start, end).getGeneralDirection(); // Fix dir potentially
                TheoreticalWire potentialWire = getLongestSubWire(start, end, currPath, scope, permits, strictWithWires);
                if (potentialWire != null && potentialWire.getEndLocation().equals(end)) {
                    currPath.add(potentialWire);
                    return currPath;
                } else if (splitNum++ < MAX_SPLITS) {
                    Vector[] orthogonals = currDirection == Direction.HORIZONTAL ?
                            new Vector[] { Vector.UP, Vector.DOWN } : new Vector[] { Vector.LEFT, Vector.RIGHT };
                    CircuitPoint orthogonalStart = start;
                    if (potentialWire != null) {
                        currPath.add(potentialWire);
                        orthogonalStart = potentialWire.getEndLocation();
                        permits.add(new InterceptPermit(potentialWire, orthogonalStart));
                    }

                    ArrayList<TheoreticalWire> potentials = new ArrayList<>();
                    for (Vector v : orthogonals) {
                        CircuitPoint orthogonalEnd = v.getMultiplied(SPLIT_DIST).addedTo(orthogonalStart);
                        ArrayList<TheoreticalWire> subWires = getAllSubWires(orthogonalStart, orthogonalEnd,
                                currPath, scope, permits, strictWithWires);
                        potentials.addAll(subWires);
                    }
                    ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                    for (TheoreticalWire theo : potentials) {
                        ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                        currPathClone.add(theo);
                        PermitList exceptionsClone = new PermitList(permits);
                        exceptionsClone.add(new InterceptPermit(theo, theo.getEndLocation()));
                        ArrayList<TheoreticalWire> generated = genWirePath(theo.getEndLocation(), end,
                                currPathClone, scope, currDirection, maxLength, splitNum, tryAllTillLength,
                                exceptionsClone, strictWithWires); // Don't switch dir because we already did perpendicular
                        if (generated != null)
                            paths.add(generated);
                    }
                    return getShortestPath(paths);
                } else
                    return null;
            }

            // If start is not in line with end
            else {
                CircuitPoint trying = getPointInLineWith(start, end, currDirection);
                if (currPath.size() < tryAllTillLength) { // SMART ALGORITHM, TRIES ALL SUB PATHS, BUT LESS EFFICIENT
                    // Calculate potentials from start to end
                    EntityList<TheoreticalWire> pots = getAllSubWires(start, trying, currPath,
                            scope, permits, strictWithWires);

                    // Pots is ordered Longest(at index 0) -> Shortest(at index len - 1). The longer the wire is,
                    // the closer it is to the user's mouse so we want to prioriize the longer ones if trying regular
                    // if trying overUnder, we want to try shorter wires first

                    Direction nextDir = currDirection.getPerpendicular();
                    boolean tryingOverUnder;
                    boolean triedRegular = false;
                    do {
                        tryingOverUnder = triedRegular;
                        int maxToTry = 4;
                        ArrayList<TheoreticalWire> generated;
                        ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                        for (int i = 0; i < pots.size() && i < maxToTry; i++) {
                            TheoreticalWire theo = pots.get(i);
                            ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                            currPathClone.add(theo);
                            PermitList permitsClone = new PermitList(permits);
                            permitsClone.add(new InterceptPermit(theo, theo.getEndLocation()));
                            generated = genWirePath(theo.getEndLocation(), end,
                                    currPathClone, scope, nextDir, maxLength, splitNum, tryAllTillLength,
                                    permitsClone, strictWithWires);
                            if (generated != null)
                                paths.add(generated);
                        }
                        if (!paths.isEmpty())
                            return getShortestPath(paths);
                        triedRegular = true;
                        // If we didn't return by here, then we should try overshoot and undershoot.
                        pots.clear();
                        // Calculate potentials from start to trying + overshoot. Exclude potentials already done
                        Vector overShootVec = directionVecFromInLinePoints(start, trying).getMultiplied(OVERSHOOT_DIST);
                        CircuitPoint tryingPlusOne = overShootVec.getDivided(OVERSHOOT_DIST).addedTo(trying);
                        pots.addAll(getAllSubWires(start, overShootVec.addedTo(trying), tryingPlusOne, currPath, scope,
                                permits, strictWithWires));
                        // Calculate potentials from start to start + undershoot
                        Vector underShootVec = overShootVec.getMultiplied(-1);
                        CircuitPoint underShootPoint = underShootVec.addedTo(start);
                        pots.addAll(getAllSubWires(start, underShootPoint, currPath,
                                scope, permits, strictWithWires));
                        // Sort it so the shorter wires are first. For over/under shoot, we prioritize shorter wires
                        pots.sort(Comparator.comparingInt(eachWire -> eachWire.getInterceptPoints(false).size()));
                    } while (!tryingOverUnder);

                    return null;

                } else { // LESS INTELLIGENT ALGORITHM, TRIES THE CLOSEST SUB WIRE TO END, MUCH MORE EFFICIENT
                    TheoreticalWire potential = getLongestSubWire(start, trying, currPath, scope, permits, strictWithWires);
                    if (potential != null) {
                        currPath.add(potential);
                        Direction nextDirection = currDirection.getPerpendicular();
                        permits.add(new InterceptPermit(potential, potential.getEndLocation()));
                        return genWirePath(potential.getEndLocation(), end, currPath, scope, nextDirection, maxLength,
                                splitNum, tryAllTillLength, permits, strictWithWires);
                    }
                    return null;
                }
            }
        }


        public static TheoreticalWire getLongestSubWire(CircuitPoint start, CircuitPoint end,
                                                        List<TheoreticalWire> alsoCantIntercept,
                                                        EntityList<? extends Entity> scope,
                                                        PermitList exceptions,
                                                        boolean strictWithWires) {
            return getLongestSubWire(start, end, start, alsoCantIntercept, scope, exceptions, strictWithWires);
        }

        public static TheoreticalWire getLongestSubWire(CircuitPoint start, CircuitPoint end, CircuitPoint stop,
                                                        List<TheoreticalWire> alsoCantIntercept,
                                                        EntityList<? extends Entity> scope,
                                                        PermitList exceptions,
                                                        boolean strictWithWires) {
            ArrayList<TheoreticalWire> subWires = getSubWires(start, end, stop, alsoCantIntercept, scope,
                    exceptions, strictWithWires, true);
            return subWires.size() == 0 ? null : subWires.get(0);
        }

        public static EntityList<TheoreticalWire> getAllSubWires(CircuitPoint start,
                                                                 CircuitPoint end,
                                                                 List<TheoreticalWire> alsoCantIntercept,
                                                                 EntityList<? extends Entity> scope,
                                                                 PermitList exceptions,
                                                                 boolean strictWithWires) {
            return getAllSubWires(start, end, start, alsoCantIntercept, scope, exceptions, strictWithWires);
        }

        public static EntityList<TheoreticalWire> getAllSubWires(CircuitPoint start,
                                                                 CircuitPoint end,
                                                                 CircuitPoint stop,
                                                                 List<TheoreticalWire> alsoCantIntercept,
                                                                 EntityList<? extends Entity> scope,
                                                                 PermitList exceptions,
                                                                 boolean strictWithWires) {
            return getSubWires(start, end, stop, alsoCantIntercept, scope, exceptions, strictWithWires, false);
        }


        public static EntityList<TheoreticalWire> getSubWires(CircuitPoint start,
                                                              CircuitPoint end,
                                                              CircuitPoint stop,
                                                              List<TheoreticalWire> cantIntercept,
                                                              EntityList<? extends Entity> scope,
                                                              PermitList exceptions,
                                                              boolean strictWithWires,
                                                              boolean stopAtFirst) {
            if (!start.isInLineWith(end) || start.equals(end))
                throw new RuntimeException("Invalid sub-wire Points " + start + " and " + end);
            Vector dir = Vector.directionVectorFrom(end, start, start.x == end.x ? Direction.VERTICAL : Direction.HORIZONTAL);
            EntityList<TheoreticalWire> potentials = new EntityList<>();
            end = end.clone();
            while (!end.equals(stop)) {
                TheoreticalWire potentialWire = new TheoreticalWire(start, end);
                boolean canPlace = canPlaceWithoutInterceptingAnything(potentialWire, cantIntercept, scope,
                        exceptions, strictWithWires);
                if (canPlace) {
                    potentials.add(potentialWire);
                    if (stopAtFirst)
                        return potentials;
                }
                assert dir != null;
                end = dir.addedTo(end);
            }
            return potentials;
        }


        public static boolean canPlaceWithoutInterceptingAnything(TheoreticalWire wire,
                                                                  List<? extends Entity> alsoCantIntercept,
                                                                  EntityList<? extends Entity> scope,
                                                                  PermitList exceptions,
                                                                  boolean strictWithWires) {
            for (Entity e : scope)
                if (e.doesGenWireInvalidlyInterceptThis(wire, exceptions, strictWithWires))
                    return false;
            for (Entity e : alsoCantIntercept)
                if (e.doesGenWireInvalidlyInterceptThis(wire, exceptions, strictWithWires))
                    return false;
            return true;
        }

        public static int getNumUnitsCovered(ArrayList<TheoreticalWire> path) {
            int units = 0;
            for (TheoreticalWire w : path)
                units += w.getInterceptPoints(false).size();
            return units;
        }

        public static ArrayList<TheoreticalWire> getShortestPath(ArrayList<ArrayList<TheoreticalWire>> pathArrayList) {
            int shortestIndex = -1;
            int shortestLength = Integer.MAX_VALUE;
            int shortestUnits = Integer.MAX_VALUE;
            for (int i = 0; i < pathArrayList.size(); i++) {
                ArrayList<TheoreticalWire> path = pathArrayList.get(i);
                if (path != null) {
                    int units = getNumUnitsCovered(path);
                    if (path.size() < shortestLength || (path.size() == shortestLength && units < shortestUnits)) {
                        shortestLength = path.size();
                        shortestIndex = i;
                        shortestUnits = units;
                    }
                }
            }
            //    System.out.println("SHORTEST INDEX = " + shortestIndex);
            return shortestIndex == -1 ? null : pathArrayList.get(shortestIndex);
        }
    }


    public static class TheoreticalWire extends Wire {
        public TheoreticalWire(CircuitPoint start, CircuitPoint end) {
            super(start.getCircuit());
            if (end == null || (start.x != end.x && start.y != end.y) || start.equals(end))
                throw new RuntimeException("Invalid Theoretical Wire " + start + " to " + end);
            this.startLocation = start;
            this.endLocation = end;
            updateInterceptPoints();
        }

        private Color color = Color.ORANGE;

        @Override
        public Color getColor() {
            return color;
        }

        public void setColor(Color col) {
            color = col;
        }

        @Override
        public String toString() {
            return "Theoretical" + super.toString();
        }

        public static boolean doesAnyWireIntercept(CircuitPoint p) {
            for (Wire w : p.getCircuit().getAllEntitiesOfType(Wire.class))
                if (w.intercepts(p))
                    return true;
            return false;
        }

    }
}