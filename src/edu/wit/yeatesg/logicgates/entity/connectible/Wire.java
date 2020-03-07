package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.entity.PropertyList;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class Wire extends ConnectibleEntity implements Dependent {

    protected CircuitPoint startLocation;
    protected CircuitPoint endLocation;

    public Wire(CircuitPoint startLocation, CircuitPoint endLocation, boolean isPreview) {
        super(startLocation.getCircuit(), isPreview);
        if ((startLocation.x != endLocation.x && startLocation.y != endLocation.y) || startLocation.equals(endLocation))
            throw new RuntimeException("Invalid Wire");
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        updateInterceptPoints();
        for (Wire w : c.getAllEntitiesOfType(Wire.class))
            if (isSimilar(w))
                throw new RuntimeException("Duplicate/Similar Wire " + startLocation + " " + endLocation );
        postInit();
        checkEntities(startLocation, endLocation);
    }

    public Wire(CircuitPoint startLocation, CircuitPoint endLocation) {
        this(startLocation, endLocation, false);
    }

    protected Wire(Circuit c) {
        super(c, false);
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
    public PointSet getInterceptPoints() {
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
            if (interceptPoints.size() > 1)
                return interceptPoints; // If it hits the same entity twice, ALL points are invalid
            else if (interceptPoints.size() == 1)
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
                return other.getInterceptPoints().intersection(getInterceptPoints());
            else {
                PointSet interceptPoints = new PointSet();
                for (CircuitPoint edgePoint : getEdgePoints())
                    for (CircuitPoint otherPoint : other.getInterceptPoints())
                        if (edgePoint.equals(otherPoint) && !interceptPoints.contains(otherPoint))
                            interceptPoints.add(edgePoint);
                for (CircuitPoint otherEdgePoint : ((Wire) other).getEdgePoints())
                    for (CircuitPoint thisPoint : getInterceptPoints())
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
        PointSet pts = getInterceptPoints().clone();
        pts.remove(0);
        pts.remove(pts.size() - 1);
        return pts;
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {
        System.out.println("ConnectCheck between " + this + " and " + e);
        if (isPreview || e.isPreview() || deleted || e.isDeleted()) {
            System.exit(0);
            return;
        }
        for (CircuitPoint edgePoint : getEdgePoints()) {
            if (canConnectTo(e, edgePoint) && e.canConnectTo(this, edgePoint) && !deleted && !e.isDeleted()) {
                System.out.println("  LES GOO");
                connect(e, edgePoint);
            } else {
                System.out.println("  Cant nigga!");
            }
        }
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        System.out.print("\nCan " + this + " connect to " + e + " at " + at + " ?");
        System.out.print(isEdgePoint(at) + " ");
        System.out.print(!hasConnectionTo(e) + " ");
        System.out.print((getNumOtherEdgePointsAt(at) < 4) + " ");
        System.out.print(!e.isDeleted() + " ");
        if (isEdgePoint(at)
                && !hasConnectionTo(e)
                && getNumOtherEdgePointsAt(at) < 4
                && !e.isDeleted()) {
            if (e instanceof Wire) {
                Wire other = (Wire) e;
                System.out.print((getDirection() != other.getDirection() || other.getNumOtherEdgePointsAt(at) > 1) + " \n");
                return getDirection() != other.getDirection() || other.getNumOtherEdgePointsAt(at) > 1;
            } else
                System.out.println();
                return true;
        }
        return false;
    }

    @Override
    public void connectCheck() {
        if (isPreview || deleted || isInvalid())
            return;
        bisectCheck();
        mergeCheck();
        super.connectCheck();
    }


    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (isInvalid() || deleted || isPreview || e.isInvalid() || e.isDeleted() || e.isPreview())
            throw new RuntimeException("Cannot bisect. At least one of these wires is deleted/invalid/preview");
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
            System.out.println("CONNECTING WIRE TO AND GAT");
            SimpleGateAND gate = (SimpleGateAND) e;
            connections.add(new ConnectionNode(atLocation, this, e));
            gate.getNodeAt(atLocation).connectedTo = this;
        }
        c.refreshTransmissions();
    }

    public void bisectCheck() {
        if (!deleted && !isPreview && !isInvalid()) {
            ArrayList<Wire> allWires = c.getAllEntitiesOfType(Wire.class, false);
            for (Wire w : allWires) {
                if (!w.deleted && !w.equals(this)) {
                    bisectCheck(w);
                    w.bisectCheck(this);
                }
            }
        }
    }

    public void bisectCheck(Wire other) {
        if (isInvalid() || deleted || isPreview || other.isInvalid() || other.deleted || other.isPreview)
            return;
        for (CircuitPoint thisWiresEndpoint : new CircuitPoint[]{startLocation, endLocation}) {
            if (other.getPointsExcludingEdgePoints().contains(thisWiresEndpoint)
                    && other.getDirection() != getDirection()) // This means it is bisecting the wire
                bisect(other, thisWiresEndpoint);
        }
    }

    public void bisect(Wire other, CircuitPoint endpointOfThisWire) {
        if (isInvalid() || deleted || isPreview || other.isInvalid() || other.deleted || other.isPreview)
            throw new RuntimeException("Cannot bisect. At least one of these wires is deleted/invalid/preview");
        System.out.println("\n" + this + "\nBISECT\n" + other + "\nAT\n" + endpointOfThisWire + "\n");
        if (!other.getPointsExcludingEdgePoints().contains(endpointOfThisWire))
            throw new RuntimeException("Invalid Wire Bisect. Can't bisect a wire at its edge points");
        if (!(endpointOfThisWire.equals(startLocation) || endpointOfThisWire.equals(endLocation)))
            throw new RuntimeException("Invalid Wire Bisect. This wire's end point must be between the other wire's" +
                    " endpoints (but cant touch the endpoints) to bisect it");
        CircuitPoint oldStartLoc = other.startLocation;
        other.set(other.startLocation, endpointOfThisWire);
        new Wire(endpointOfThisWire, oldStartLoc);
        c.refreshTransmissions();
        c.getEditorPanel().repaint();
    }

    public boolean isSimilar(Wire w) {
        return (w.startLocation.equals(startLocation) && w.endLocation.equals(endLocation))
                || (w.startLocation.equals(endLocation) && w.endLocation.equals(startLocation));
    }

    public void mergeCheck() {
        if (!deleted && !isPreview && !isInvalid()) {
            ArrayList<Wire> allWires = c.getAllEntitiesOfType(Wire.class, false);
            for (Wire w : allWires)
                if (!w.deleted && !w.equals(this))
                    mergeCheck(w);
        }
    }

    public void mergeCheck(Wire other) {
        if (isInvalid() || deleted || isPreview || other.isInvalid() || other.deleted || other.isPreview)
            return;
        for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation}) {
            for (CircuitPoint otherEdgePoint : new CircuitPoint[]{other.startLocation, other.endLocation}) {
                if (edgePoint.equals(otherEdgePoint)
                        && (other.getNumOtherEdgePointsAt(edgePoint) < 2)
                        && other.getDirection() == getDirection()
                        && !isSimilar(other)
                        && !(other.deleted || deleted)) {
                    merge(other, edgePoint);
                }
            }
        }
    }

    public void merge(Wire other, CircuitPoint commonEdgePoint) {
        if (isInvalid() || deleted || isPreview || other.isInvalid() || other.deleted || other.isPreview)
            throw new RuntimeException("Cannot merge. At least one of these wires is deleted/invalid/preview");
        other.delete(); // Checks done in delete
        set(commonEdgePoint, other.getOppositeEdgePoint(commonEdgePoint)); // Checks done in set
        c.refreshTransmissions();
        c.getEditorPanel().repaint();
    }


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

    public CircuitPoint[] getEdgePoints() {
        return new CircuitPoint[]{startLocation, endLocation};
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
        if (!isEdgePoint(edgePoint))
            throw new RuntimeException("Set must be called on the edgePoint of a wire");
        if (to.equals(getOppositeEdgePoint(edgePoint))) {
            delete();
        } else if (whichEdgePoint(edgePoint).equals("start")) {
            startLocation = to;
        } else if (whichEdgePoint(edgePoint).equals("end"))
            endLocation = to;
        updateInterceptPoints();
        if (!deleted)
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
        return getState().getColor();
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
    public boolean equals(Object other) {
        return other instanceof Wire && ((Wire) other).startLocation.equals(startLocation) &&
                ((Wire) other).endLocation.equals(endLocation);
    }

    @Override
    public String toString() {
        return "Wire{" +
                "start=" + startLocation +
                ", end=" + endLocation +
                '}';
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




    private static int genOrder = 1;


    public static ArrayList<TheoreticalWire> genWirePathStrict(CircuitPoint start,
                                                               CircuitPoint end,
                                                               Direction initialDir,
                                                               int maxLength) {
        return genWirePath(start, end, initialDir, maxLength, genOrder, true);

    }

    public static ArrayList<TheoreticalWire> genWirePathLenient(CircuitPoint start,
                                                                CircuitPoint end,
                                                                Direction initialDir,
                                                                int maxLength) {
        return genWirePath(start, end, initialDir, maxLength, genOrder, false);
    }

    public static ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                         CircuitPoint end,
                                                         Direction initialDir,
                                                         int maxLength,
                                                         int order,
                                                         boolean strictWithWires) {
   //     System.out.println("GEN FROM " + start + " to " + end + ", dir = " + initialDir);
        PermitList permitList = new PermitList();
        int expansion = (SPLIT_DIST + OVERSHOOT_DIST) * order;
        BoundingBox scopeBox = new BoundingBox(start, end, null);
        scopeBox = scopeBox.getExpandedBy(expansion);
        EntityList<Entity> scope = scopeBox.getInterceptingEntities();
        return genWirePath(start, end, new ArrayList<>(), scope, initialDir, maxLength, 0, order, permitList, strictWithWires);
    }

    private static final int SPLIT_DIST = 3;
    private static final int OVERSHOOT_DIST = 5;
    private static final int MAX_SPLITS = 2;

    private static ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                          CircuitPoint end,
                                                          ArrayList<TheoreticalWire> currPath,
                                                          EntityList<? extends Entity> scope,
                                                          Direction currDirection,
                                                          int maxLength,
                                                          int splitNum,
                                                          int tryAllTillLength,
                                                          PermitList permits,
                                                          boolean strictWithWires) {
      //  System.out.println("gen from " + start + " to " + end);
        if (currPath.size() == 0)
            for (Entity e : start.getCircuit().getAllEntitiesThatIntercept(start).ofType(ConnectibleEntity.class))
                permits.add(new InterceptPermit(e, start));
            for (ConnectibleEntity ce : start.getCircuit().getAllEntitiesThatIntercept(end).ofType(ConnectibleEntity.class))
                permits.add(new InterceptPermit(ce, end));
        if (currDirection == null) {
            // try shortest between vert and horiz
            ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
            paths.add(genWirePath(start, end, currPath, scope, Direction.HORIZONTAL, maxLength, splitNum,
                    tryAllTillLength, permits, strictWithWires));
            paths.add(genWirePath(start, end, currPath, scope, Direction.VERTICAL, maxLength, splitNum,
                    tryAllTillLength, permits, strictWithWires));
            return getShortestPath(paths);
        }
        if (currPath.size() > maxLength)
            return null;
        if (start.isInLineWith(end)) {
            currDirection = directionVecFromInLinePoints(start, end).getGeneralDirection(); // Fix dir potentially
            TheoreticalWire potentialWire = getClosestSubWire(start, end, currPath, scope, permits, strictWithWires);
            if (potentialWire != null && potentialWire.getEndLocation().equals(end)) {
                currPath.add(potentialWire);
                return currPath;
            } else if (splitNum++ < MAX_SPLITS) {
                Vector[] orthogonals = currDirection == Direction.HORIZONTAL ?
                        new Vector[]{Vector.UP, Vector.DOWN} : new Vector[]{Vector.LEFT, Vector.RIGHT};
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
                            exceptionsClone, strictWithWires); // Dont switch dir because we already did perpendicular
                    if (generated != null)
                        paths.add(generated);
                }
                return getShortestPath(paths);
            } else
                return null;
        } else {
            CircuitPoint trying = getPointInLineWith(start, end, currDirection);
            if (currPath.size() < tryAllTillLength) { // SMART ALGORITHM, TRIES ALL SUB PATHS, BUT LESS EFFICIENT
                Vector overShootVec = directionVecFromInLinePoints(start, trying).getMultiplied(OVERSHOOT_DIST);
                EntityList<TheoreticalWire> potentials = getAllSubWires(start, overShootVec.addedTo(trying), currPath,
                        scope, permits, strictWithWires);
                Vector underShootVec = overShootVec.getMultiplied(-1);
                CircuitPoint underShootPoint = underShootVec.addedTo(start);
                ArrayList<TheoreticalWire> underShootPotentials = getAllSubWires(start, underShootPoint, currPath,
                        scope, permits, strictWithWires);
                potentials.addAll(underShootPotentials); // TODO later, maybe make it try normal, then over, then undershoot so it has to do like 10 less operations per path
                final ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                final Direction currDir = currDirection;
                final int splitNumFin = splitNum;
                int maxToTry = 10;
                while (potentials.size() > maxToTry + 2*OVERSHOOT_DIST)
                    potentials.remove(potentials.get(potentials.size() - 2*OVERSHOOT_DIST - 1));
                potentials.multiThreadForEach(theo -> {
                    ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                    currPathClone.add(theo);
                    PermitList exceptionsClone = new PermitList(permits);
                    exceptionsClone.add(new InterceptPermit(theo, theo.getEndLocation()));
                    Direction nextDir = currDir.getPerpendicular();
                    ArrayList<TheoreticalWire> generated = genWirePath(theo.getEndLocation(), end,
                            currPathClone, scope, nextDir, maxLength, splitNumFin, tryAllTillLength,
                            exceptionsClone, strictWithWires);
                    if (generated != null)
                        synchronized (paths) {
                            paths.add(generated);
                        }
                }, Math.max(potentials.size() / 5, 1));
                return getShortestPath(paths);
            } else { // LESS INTELLIGENT ALGORITHM, TRIES THE CLOSEST SUB WIRE TO END, MUCH MORE EFFICIENT
                TheoreticalWire potential = getClosestSubWire(start, trying, currPath, scope, permits, strictWithWires);
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


    public static TheoreticalWire getClosestSubWire(CircuitPoint start, CircuitPoint end,
                                                    List<TheoreticalWire> alsoCantIntercept,
                                                    EntityList<? extends Entity> scope,
                                                    PermitList exceptions,
                                                    boolean strictWithWires) {
        return getClosestSubWire(start, end, start, alsoCantIntercept, scope, exceptions, strictWithWires);
    }

    public static TheoreticalWire getClosestSubWire(CircuitPoint start, CircuitPoint end, CircuitPoint stop,
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
        while (!end.equals(stop)) {
            TheoreticalWire potentialWire = new TheoreticalWire(start, end);
            boolean canPlace = Wire.canPlaceWireWithoutInterceptingAnything(potentialWire, cantIntercept, scope,
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


    public static boolean canPlaceWireWithoutInterceptingAnything(TheoreticalWire wire,
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

    public static int getNumUnitsCovered(ArrayList<TheoreticalWire> path) {
        int units = 0;
        for (TheoreticalWire w : path)
            units += w.getInterceptPoints().size();
        return units;
    }

    public static boolean invalidDirectionForPath(Vector dir, CircuitPoint start, CircuitPoint end) {
        return (dir != null) && (dir.equals(Vector.LEFT) && start.x <= end.x
                || dir.equals(Vector.RIGHT) && start.x >= end.x
                || dir.equals(Vector.DOWN) && start.y >= end.y
                || dir.equals(Vector.UP) && start.y <= end.y);
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

        @Override
        public boolean equals(Object other) {
            return other instanceof Wire
                    && ((Wire) other).startLocation.equals(getStartLocation())
                    && ((Wire) other).endLocation.equals(getEndLocation());
        }
    }
}