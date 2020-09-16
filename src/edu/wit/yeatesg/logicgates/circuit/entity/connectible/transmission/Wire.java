package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.EntityList;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.datatypes.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

public class Wire extends ConnectibleEntity implements Powerable {

    protected CircuitPoint startLocation;
    protected CircuitPoint endLocation;

    /**
     * Constructs a Wire whose startLocation and endLocation are similar to the ones supplied in the parameters
     * @param startLocation the startLocation for this Wire, which will be cloned
     * @param endLocation the endLocation for this Wire, which will be cloned
     */
    public Wire(CircuitPoint startLocation, CircuitPoint endLocation) {
        super(startLocation.getCircuit());
   //     System.out.println("construct wire of eid " + getEntityID());
        if (!startLocation.getCircuit().equals(endLocation.getCircuit()))
            throw new RuntimeException("Points are on different Circuits");
        if ((startLocation.x != endLocation.x && startLocation.y != endLocation.y) || startLocation.equals(endLocation))
            throw new RuntimeException("Invalid Wire");
        this.startLocation = startLocation.getSimilar();
        this.endLocation = endLocation.getSimilar();
        construct();
    }

    @Override
    public void construct() {
        interceptPoints = new CircuitPointList();
        connections = new ConnectionList(this);
        boundingBox = new BoundingBox(startLocation, endLocation, this);
        edgeToEdgeIterator().forEachRemaining(intPoint -> interceptPoints.add(intPoint));
        postInit();
    }

    @Override
    public Wire getCloned(Circuit onto) {
        return new Wire(startLocation.clone(onto), endLocation.clone(onto));
    }

    @Override
    public Wire getSimilarEntity() {
        return (Wire) super.getSimilarEntity();
    }

    public void set(CircuitPoint edgePoint, CircuitPoint to) {
        if (!isEdgePoint(edgePoint))
            throw new RuntimeException("Set must be called on the edgePoint of a wire");
        if (to.equals(getOppositeEdgePoint(edgePoint))) {
            remove();
            return;
        } else if (whichEdgePoint(edgePoint).equals("start")) {
            startLocation = to.getSimilar();
        } else if (whichEdgePoint(edgePoint).equals("end"))
            endLocation = to.getSimilar();
        else
            throw new RuntimeException("tu fucked up");
        reconstruct();
    }


    public boolean isSimilar(Entity e) {
        if (!(e instanceof Wire))
            return false;
        Wire w = (Wire) e;
        return (w.startLocation.equals(startLocation) && w.endLocation.equals(endLocation))
                || (w.startLocation.equals(endLocation) && w.endLocation.equals(startLocation));
    }

    @Override
    public String toParsableString() {
        CircuitPoint start = interceptPoints.get(0);
        CircuitPoint end = interceptPoints.get(interceptPoints.size() - 1);
        CircuitPoint lefter = isHorizontal() ? (start.x < end.x ? start : end) : (start.y < end.y ? start : end);
        CircuitPoint righter = lefter.isSimilar(start) ? end : start;
        return "[Wire]" + lefter.toParsableString() + "," + righter.toParsableString();
     //   return "[Wire]" + getLefterEdgePoint().toParsableString() + "," + getRighterEdgePoint().toParsableString();
    }

    /**
     * Returns the lefter edge point of this wire if horizontal, returns the higher edge point of this wire if
     * vertical
     * @return
     */
    public CircuitPoint getLefterEdgePoint() {
        return isHorizontal() ? (
                startLocation.x < endLocation.x ? startLocation : endLocation)
                : (startLocation.y < endLocation.y ? startLocation : endLocation).getSimilar();
    }

    public CircuitPoint getRighterEdgePoint() {
        return getOppositeEdgePoint(getLefterEdgePoint());
    }

    /**
     * Iterator that goes from the lefter/higher edge point of this Wire to the righter/lower edge point of this Wire
     * @return the edge to edge iterator of this Wire
     */
    public Iterator<CircuitPoint> edgeToEdgeIterator() {
        CircuitPoint firstEdge = getLefterEdgePoint();
        CircuitPoint secondEdge = getRighterEdgePoint();
        return new Iterator<>() {
            int size = getLength();
            int cursor = 0;
            Vector dir = new Vector(firstEdge, secondEdge).getUnitVector();
            CircuitPoint curr = firstEdge.getSimilar();

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public CircuitPoint next() {
                CircuitPoint returning = curr.getSimilar();
                cursor++;
                curr = dir.addedTo(curr);
                return returning;
            }
        };
    }

    /**
     * Obtains the length of this Wire. The length of this Wire is how many CircuitPoints it touches. A Wire where
     * start=[0,0] end=[0,1] has a length of 2. The shortest possible length for a Wire is 2, anything less means
     * there is an error. This method does not look at interceptPoints.size() because this method is used to
     * update the interceptPoints themselves.
     * @return the number of CircuitPoints that this Wire touches
     */
    public int getLength() {
        CircuitPoint first = getLefterEdgePoint(); // If horizontal, first is the left point. If vertical, first if the higher point
        CircuitPoint second = getRighterEdgePoint();
        return 1 + (isHorizontal() ? (int) (second.x - first.x) : (int) (second.y - first.y));
    }

    public boolean eats(Wire other) {
        return intercepts(other.getStartLocation()) && intercepts(other.getEndLocation()) && getLength() > other.getLength();
    }

    @Override
    public PowerValue getLocalPowerStateOf(OutputNode root) {
        return null; // Has volatile nodes
    }

    @Override
    protected void assignOutputsToInputs() { /* Not Implemented For Wire, Has Volatile Nodes */ }

    @Override
    public String getDisplayName() {
        return "Wire";
    }

    @Override
    public void move(Vector v) {
        startLocation = startLocation.getIfModifiedBy(v);
        endLocation = endLocation.getIfModifiedBy(v);
        reconstruct();
    }

    @Override
    public CircuitPointList getInvalidInterceptPoints(Entity e) {
        CircuitPointList interceptPoints = getInterceptPoints(e);
        CircuitPointList invalidPoints = new CircuitPointList();
        if (interceptPoints.size() > 1)
            return interceptPoints; // If it hits the same entity twice, ALL points are invalid
        else if (interceptPoints.size() == 1) {
            CircuitPoint intPoint = interceptPoints.get(0);
            if (e instanceof Wire) {
                Wire other = (Wire) e;
                if (other.getDirection() == getDirection())
                    for (CircuitPoint interceptPoint : interceptPoints)
                        if (!(other.isEdgePoint(interceptPoint) && isEdgePoint(interceptPoint)))
                            invalidPoints.add(interceptPoint);
                return invalidPoints;
            } else if (e instanceof ConnectibleEntity) {
                if (!isEdgePoint(intPoint))
                    return interceptPoints; // If this Wire intercepts a non-wire at a non edge point location, it's invalid
                ConnectibleEntity ce = (ConnectibleEntity) e;
                CircuitPoint point = interceptPoints.get(0);
                if (e.getWireBlockLocations() != null)
                    for (Map.MapEntry<CircuitPoint, Direction> entry : e.getWireBlockLocations().getEntries())
                        if (entry.getKey().isSimilar(point) && getDirection() == entry.getValue())
                            return interceptPoints;

                if (!hasConnectionTo(ce) && !(ce.hasNodeAt(point) && ce.getNumEntitiesConnectedAt(point) == 0))
                    return interceptPoints; // If it hits the entity once, but cant connect, it's invalid
             }
        }
        return invalidPoints; // InvalidPoints is empty here
    }

    private BoundingBox boundingBox;

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox.clone();
    }

    public Direction getDirection() {
        return startLocation.x == endLocation.x ? Direction.VERTICAL : Direction.HORIZONTAL;
    }

    public boolean isVertical() {
        return getDirection() == Direction.VERTICAL;
    }

    public boolean isHorizontal() {
        return getDirection() == Direction.HORIZONTAL;
    }

    public CircuitPointList getPointsExcludingEdgePoints() {
        CircuitPointList pts = getInterceptPoints();
        pts.remove(0);
        pts.remove(pts.size() - 1);
        return pts;
    }

    // Connect checking and connecting

    @Override
    public void connectCheck() {
        mergeCheck();
        bisectCheck();
        super.connectCheck();
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {
        for (CircuitPoint edgePoint : getEdgePoints()) {
            if (canConnectTo(e, edgePoint) && e.canConnectTo(this, edgePoint))
                connect(e, edgePoint);
        }
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        return isEdgePoint(at) && getNumOtherEdgePointsAt(at) < 4 && canConnectToGeneral(e);
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!canConnectTo(e, atLocation) || !e.canConnectTo(this, atLocation))
            throw new RuntimeException("canConnectTo returned false");
        if (e instanceof Wire) {
            connections.add(new ConnectionNode(atLocation, this, e));
            ((Wire) e).connections.add(new ConnectionNode(atLocation, e, this));
        } else {
            connections.add(new ConnectionNode(atLocation, this, e));
            e.getNodeAt(atLocation).setConnectedTo(this);
        }

    }

    @Override
    public void disconnect(ConnectibleEntity e) {
        connections.remove(getConnectionTo(e));
    }

    private boolean blockTransform = false;

    public boolean isTransformBlocked() {
        return blockTransform;
    }

    public void disableTransformable() {
        blockTransform = true;
    }

    public void enableTransformable(boolean update) {
        blockTransform = false;
        if (update)
            update();
    }

    public void enableTransformable() {
        enableTransformable(true);
    }

    // Transform checks for wires

    public boolean canTransform() {
        return !c.isTransformDisabled() && !blockTransform && !blockUpdate && existsInCircuit();
    }

    private boolean blockBisect = false;

    public void disableBisect() {
        blockBisect = true;
    }

    public void enableBisect() {
        blockBisect = false;
    }

    public boolean canBisect() {
        return canTransform() && !blockBisect && !isInvalid();
    }

    private void bisectCheck() {
        if (canBisect()) {
            ArrayList<Wire> interceptingWires = getInterceptingEntities().thatExtend(Wire.class);
            for (Wire w : interceptingWires) {
                bisectCheck(w);
                w.bisectCheck(this);
            }
        }
    }

    private void bisectCheck(Wire other) {
        if (!canBisect() || !other.canBisect() || isSimilar(other))
            return;
        for (CircuitPoint edgePoint : new CircuitPoint[]{ startLocation, endLocation }) {
            if (other.getPointsExcludingEdgePoints().contains(edgePoint)
                    && other.getDirection() != getDirection()
                    && !c.isWireBridgeAt(edgePoint)) {// This means it is bisecting the wire
                CircuitPoint bisectPoint = edgePoint.getSimilar();
                CircuitPoint otherOldStartLoc = other.getStartLocation();
                blockBisect = true;
                blockMerge = true;
                other.disconnectAll();
                c.disableUpdate();
                other.set(other.startLocation, bisectPoint);
                Wire added = new Wire(bisectPoint.getSimilar(), otherOldStartLoc.getSimilar());
                if (other.isSelected())
                    added.select();
                added.add();
                c.enableUpdate();
                c.updateEntities(other, added, this);
                blockBisect = false;
                blockMerge = false;
            }
        }
    }

    /**
     * When using this method, make sure you have transform disabled for either this wire, or for the whole Circuit.
     * If you don't then this Wire will immediately re-merge
     * @param splitPoint the location on this Wire that it is being split at
     * @return
     */
    public void split(CircuitPoint splitPoint) {
        if (!intercepts(splitPoint) || isEdgePoint(splitPoint))
            throw new RuntimeException("Invalid Split " + splitPoint.toParsableString() + " on " + this.toParsableString());
        if (blockTransform)
            disableTransformable();
        splitPoint = splitPoint.getSimilar();
        CircuitPoint oldStart = startLocation.getSimilar();
        set(startLocation, splitPoint);
        Wire created = new Wire(splitPoint, oldStart);
        if (isSelected())
            created.select();
        created.add();
    }

    // Merge checks for Wires

    private void mergeCheck() {
        if (canTransform())
            for (Wire w : getInterceptingEntities().thatExtend(Wire.class))
                mergeCheck(w);
    }

    private boolean blockMerge;

    private void mergeCheck(Wire other) {
   //     System.out.println("  MERGE CHECK " + this.toParsableString() + " ("
    //            + id + ")" + " with " + other.toParsableString() + " (" + other.id + ")");
        if (other.getInterceptPoints().intersection(getInterceptPoints()).size() > 1)
            return;
        if (blockMerge || other.blockMerge || other.isSimilar(this) || !canTransform() || !other.canTransform()
                || (isSelected() != other.isSelected()) ) {
            return;
        }
        for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation}) {
            for (CircuitPoint otherEdgePoint : new CircuitPoint[]{other.startLocation, other.endLocation}) {
                if (edgePoint.equals(otherEdgePoint)
                        && other.getDirection() == getDirection()
                        && getNumOtherEdgePointsInOppositeDirectionAt(edgePoint) == 0) {
             //       System.out.println("  MERGE " + this.toParsableString() + " ("
             //               + id + ")" + " with " + other.toParsableString() + " (" + other.id + ")");
                    c.disableUpdate();
                    blockMerge = true;
                    blockBisect = true;
                    CircuitPoint opposite = getOppositeEdgePoint(edgePoint);
                    other.remove(); // Checks done in delete
                   // System.out.println("SET " + edgePoint.toParsableString() + " to " + other.getOppositeEdgePoint(edgePoint).toParsableString());
                    set(edgePoint, other.getOppositeEdgePoint(edgePoint)); // Reconstruct done in set
                    //System.out.println("  DONE MERGE");
                    c.enableUpdate();
                    c.updateEntitiesAt(edgePoint, other.getOppositeEdgePoint(edgePoint), opposite);
                    blockMerge = false;
                    blockBisect = false;
                }
            }
        }
    }

    public int getNumOtherEdgePointsAt(CircuitPoint edgeOfThisWire) {
        int num = 0;
        ArrayList<Wire> otherWires = getInterceptingEntities().thatExtend(Wire.class);
        for (Wire w : otherWires)
            if (w.startLocation.equals(edgeOfThisWire) || w.endLocation.equals(edgeOfThisWire))
                num++;
        return num;
    }

    public int getNumOtherEdgePointsInOppositeDirectionAt(CircuitPoint edgeOfThisWire) {
        int num = 0;
        ArrayList<Wire> otherWires = getInterceptingEntities().getWiresGoingInOppositeDirection(getDirection());
        for (Wire w : otherWires)
            if (w.startLocation.equals(edgeOfThisWire) || w.endLocation.equals(edgeOfThisWire))
                num++;
        return num;
    }



    // For the PullPoint inside of the Circuit class

    @Override
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return intercepts(gridSnap);
    }

    @Override
    public boolean canCreateWireFrom(CircuitPoint locationOnThisEntity) {
        for (Entity e : getCircuit().getEntitiesThatIntercept(locationOnThisEntity)) {
            if (e instanceof Wire // Covers the case where 2 wires make a plus, but aren't connected
                    && !e.equals(this)
                    && ((Wire) e).getPointsExcludingEdgePoints().intersection(getPointsExcludingEdgePoints())
                        .contains(locationOnThisEntity))
                return false;
        }
        return getNumEntitiesConnectedAt(locationOnThisEntity) < 3; // General case
    }

    public CircuitPoint getStartLocation() {
        return startLocation.getSimilar();
    }

    public CircuitPoint getEndLocation() {
        return endLocation.getSimilar();
    }

    public LinkedList<CircuitPoint> getEdgePoints() {
        LinkedList<CircuitPoint> edgePoints = new LinkedList<>();
        edgePoints.add(startLocation.getSimilar());
        edgePoints.add(endLocation.getSimilar());
        return edgePoints;
    }

    public boolean isEdgePoint(CircuitPoint location) {
        return !whichEdgePoint(location).equals("none");
    }

    public String whichEdgePoint(CircuitPoint p) {
        return p.equals(startLocation) ? "start" : (p.equals(endLocation) ? "end" : "none");
    }

    public CircuitPoint getOppositeEdgePoint(CircuitPoint p) {
        if (!isEdgePoint(p))
            throw new RuntimeException();
        if (whichEdgePoint(p).equals("start"))
            return endLocation.getSimilar();
        else
            return startLocation.getSimilar();

    }

    public Color getColor() {
        try {
            return getPowerValue().getColor();
        } catch (Exception e) { System.out.println("No color for " + this); e.printStackTrace(); System.exit(0);}
        return null;
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        draw(g, col == null ? getColor() : col, opacity, true);
    }

    public void draw(GraphicsContext g, Color col, double opacity, boolean drawJunctions) {
        col = Color.rgb((int) (255*col.getRed()), (int) (255*col.getGreen()), (int) (255*col.getBlue()), opacity);
        g.setStroke(col);
        g.setLineWidth(getLineWidth());
        PanelDrawPoint p1 = startLocation.toPanelDrawPoint();
        PanelDrawPoint p2 = endLocation.toPanelDrawPoint();
        g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        if (drawJunctions)
            for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation})
                if (getNumEntitiesConnectedAt(edgePoint) > 1)
                    drawJunction(g, col, edgePoint);
    }

    public void drawJunction(GraphicsContext g, Color col, CircuitPoint loc) {
        g.setStroke(col);
        double circleSize = (getLineWidth() * 2.25);
        if (loc.getCircuit().getScale() == 5)
            circleSize *= 1.25;
        PanelDrawPoint dp = loc.toPanelDrawPoint();
        g.setFill(Circuit.COL_BG);
        g.fillOval(dp.x - circleSize / 2.00, dp.y - circleSize / 2.00, circleSize, circleSize);
        g.setFill(getColor());
        g.fillOval(dp.x - circleSize / 2.00, dp.y - circleSize / 2.00, circleSize, circleSize);
    }

    @Override
    public double getLineWidth() {
        return getLineWidth(c);
    }

    public static double getLineWidth(Circuit c) {
        return c.getLineWidth() * 1.2;
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
                ", eid=" + eID + "}";
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
        if (!strictWithWires)
            if (getDirection() != theo.getDirection())
                for (CircuitPoint p : theo.getPointsExcludingEdgePoints())
                    exceptions.add(new InterceptPermit(this, p));
        if (theo.invalidlyIntercepts(this)) {
            return true;
        } else {
            for (CircuitPoint p : theo.getInterceptPoints(this)) {
                InterceptPermit requiredExceptin = new InterceptPermit(this, p);
                if (!exceptions.contains(requiredExceptin)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getPropertyTableHeader() {
        return "Properties For: Wire";
    }

    @Override
    public PropertyList getPropertyList() {
        return new PropertyList(this, c);
    }

    @Override
    public void onPropertyChangeViaTable(String propertyName, String old, String newVal) {

    }

    @Override
    public void onPropertyChange(String propertyName, String old, String newVal) {

    }

    @Override
    public String getPropertyValue(String propertyName) {
        return null;
    }


    @Override
    public boolean hasProperty(String propertyName) {
        return false;
    }


    private DependencyTree dependencyTree;

    @Override
    public DependencyTree getDependencyTree() {
        return dependencyTree;
    }

    @Override
    public void setDependencyTree(DependencyTree tree) {
        dependencyTree = tree;
    }

    private boolean marked = false;

    @Override
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    @Override
    public boolean isMarked() {
        return marked;
    }

    @Override
    public boolean isNegated() {
        return false;
    }

    private PowerValue powerValue = PowerValue.UNDETERMINED;

    @Override
    public PowerValue getPowerValue() {
        return powerValue;
    }

    @Override
    public void setPowerValue(PowerValue val) {
        powerValue = val;
    }

    @Override
    public int getOscillationIndex() {
        return 0; // Not implemented for wires
    }

    @Override
    public void setOscillationIndex(int index) {
        // Not implemented for wires
    }


    public static class WireGenerator {

        /** For multi-threading. This is so we can cancel the generation when it's obsolete, and not use too many resources */
        private boolean cancelled;

        /** 1 means the algorithm is efficient and weak, 2 means it is less efficient but much smarter */
        private int genOrder;

        private static final int SPLIT_DIST = 3;
        private static final int OVERSHOOT_DIST = 3;
        private static final int MAX_SPLITS = 2;

        public WireGenerator(int genOrder) {
            if (genOrder != 1 && genOrder != 2 && genOrder != 3)
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
            return genWirePath(start, end, new ArrayList<>(), initialDir, maxLength, 0, order, permitList, strictWithWires);
        }

        private ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                       CircuitPoint end,
                                                       ArrayList<TheoreticalWire> currPath,
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
                for (Entity e : start.getCircuit().getEntitiesThatIntercept(start).thatExtend(ConnectibleEntity.class))
                    permits.add(new InterceptPermit(e, start));
            for (ConnectibleEntity ce : start.getCircuit().getEntitiesThatIntercept(end).thatExtend(ConnectibleEntity.class))
                permits.add(new InterceptPermit(ce, end)); // Always add permit to end loc

            // No supplied direction: try 2 directions in this case, use shortest of both
            if (currDirection == null) {
                ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                paths.add(genWirePath(start, end, currPath, Direction.HORIZONTAL, maxLength, splitNum,
                        tryAllTillLength, permits, strictWithWires));
                paths.add(genWirePath(start, end, currPath, Direction.VERTICAL, maxLength, splitNum,
                        tryAllTillLength, permits, strictWithWires));
                return getShortestPath(paths);
            }

            // If start is in line with end
            if (start.isInLineWith(end)) {
                currDirection = directionVecFromInLinePoints(start, end).getGeneralDirection(); // Fix dir potentially
                TheoreticalWire potentialWire = getLongestSubWire(start, end, currPath, permits, strictWithWires);
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
                        permits.add(new InterceptPermit(potentialWire, potentialWire.getEndLocation()));
                    }
                    ArrayList<TheoreticalWire> potentials = new ArrayList<>();
                    for (Vector v : orthogonals) {
                        CircuitPoint orthogonalEnd = v.getMultiplied(SPLIT_DIST).addedTo(orthogonalStart);
                        ArrayList<TheoreticalWire> subWires = getAllSubWires(orthogonalStart, orthogonalEnd,
                                currPath, permits, strictWithWires);
                        potentials.addAll(subWires);
                    }
                    ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                    for (TheoreticalWire theo : potentials) {
                        ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                        currPathClone.add(theo);
                        PermitList exceptionsClone = new PermitList(permits);
                        exceptionsClone.add(new InterceptPermit(theo, theo.getEndLocation()));
                        ArrayList<TheoreticalWire> generated = genWirePath(theo.getEndLocation(), end,
                                currPathClone, currDirection, maxLength, splitNum, tryAllTillLength,
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
                    EntityList<TheoreticalWire> pots = getAllSubWires(start, trying, currPath, permits, strictWithWires);

                    // Pots is ordered Longest(at index 0) -> Shortest(at index len - 1). The longer the wire is,
                    // the closer it is to the user's mouse so we want to prioriize the longer ones if trying regular
                    // if trying overUnder, we want to try shorter wires first

                    ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                    Direction nextDir = currDirection.getPerpendicular();
                    boolean tryingOverUnder;
                    boolean triedRegular = false;
                    do {
                        tryingOverUnder = triedRegular;
                        int maxToTry = 5;
                        ArrayList<TheoreticalWire> generated;
                        for (int i = 0; i < pots.size() && i < maxToTry; i++) {
                            TheoreticalWire theo = pots.get(i);
                            ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                            currPathClone.add(theo);
                            PermitList permitsClone = new PermitList(permits);
                            permitsClone.add(new InterceptPermit(theo, theo.getEndLocation()));
                            generated = genWirePath(theo.getEndLocation(), end,
                                    currPathClone, nextDir, maxLength, splitNum, tryAllTillLength,
                                    permitsClone, strictWithWires);
                            if (generated != null)
                                paths.add(generated);
                        }

                        triedRegular = true;
                        // If we didn't return by here, then we should try overshoot and undershoot.
                        pots.clear();
                        // Calculate potentials from start to trying + overshoot. Exclude potentials already done
                        Vector overShootVec = directionVecFromInLinePoints(start, trying).getMultiplied(OVERSHOOT_DIST);
                        CircuitPoint tryingPlusOne = overShootVec.getDivided(OVERSHOOT_DIST).addedTo(trying);
                        pots.addAll(getAllSubWires(start, overShootVec.addedTo(trying), tryingPlusOne, currPath,
                                permits, strictWithWires));
                        // Calculate potentials from start to start + undershoot
                        Vector underShootVec = overShootVec.getMultiplied(-1);
                        CircuitPoint underShootPoint = underShootVec.addedTo(start);
                        pots.addAll(getAllSubWires(start, underShootPoint, currPath, permits, strictWithWires));
                        // Sort it so the shorter wires are first. For over/under shoot, we prioritize shorter wires
                        pots.sort(Comparator.comparingInt(Wire::getLength));
                    } while (!tryingOverUnder);

                    if (!paths.isEmpty())
                        return getShortestPath(paths);


                } else { // LESS INTELLIGENT ALGORITHM, TRIES THE CLOSEST SUB WIRE TO END, MUCH MORE EFFICIENT
                    TheoreticalWire potential = getLongestSubWire(start, trying, currPath, permits, strictWithWires);
                    if (potential != null) {
                        currPath.add(potential);
                        Direction nextDirection = currDirection.getPerpendicular();
                        permits.add(new InterceptPermit(potential, potential.getEndLocation()));
                        return genWirePath(potential.getEndLocation(), end, currPath, nextDirection, maxLength,
                                splitNum, tryAllTillLength, permits, strictWithWires);
                    }
                }
                return null;
            }
        }


        public static TheoreticalWire getLongestSubWire(CircuitPoint start, CircuitPoint end,
                                                        ArrayList<TheoreticalWire> alsoCantIntercept,
                                                        PermitList exceptions,
                                                        boolean strictWithWires) {
            return getLongestSubWire(start, end, start, alsoCantIntercept, exceptions, strictWithWires);
        }

        public static TheoreticalWire getLongestSubWire(CircuitPoint start, CircuitPoint end, CircuitPoint stop,
                                                        ArrayList<TheoreticalWire> alsoCantIntercept,
                                                        PermitList exceptions,
                                                        boolean strictWithWires) {
            ArrayList<TheoreticalWire> subWires = getSubWires(start, end, stop, alsoCantIntercept,
                    exceptions, strictWithWires, true);
            return subWires.size() == 0 ? null : subWires.get(0);
        }

        public static EntityList<TheoreticalWire> getAllSubWires(CircuitPoint start,
                                                                 CircuitPoint end,
                                                                 ArrayList<TheoreticalWire> alsoCantIntercept,
                                                                 PermitList exceptions,
                                                                 boolean strictWithWires) {
            return getAllSubWires(start, end, start, alsoCantIntercept, exceptions, strictWithWires);
        }

        public static EntityList<TheoreticalWire> getAllSubWires(CircuitPoint start,
                                                                 CircuitPoint end,
                                                                 CircuitPoint stop,
                                                                 ArrayList<TheoreticalWire> alsoCantIntercept,
                                                                 PermitList exceptions,
                                                                 boolean strictWithWires) {
            return getSubWires(start, end, stop, alsoCantIntercept, exceptions, strictWithWires, false);
        }


        public static EntityList<TheoreticalWire> getSubWires(CircuitPoint start,
                                                              CircuitPoint end,
                                                              CircuitPoint stop,
                                                              ArrayList<TheoreticalWire> cantIntercept,
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
                boolean canPlace = canPlaceWithoutInterceptingAnything(potentialWire, cantIntercept,
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
                                                                  ArrayList<? extends Entity> alsoCantIntercept,
                                                                  PermitList exceptions,
                                                                  boolean strictWithWires) {
            if (!wire.getStartLocation().isInMapRange() || !wire.getEndLocation().isInMapRange())
                return false;
            for (Entity e : wire.getInterceptingEntities())
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
                units += w.getLength();
            return units;
        }

        public static ArrayList<TheoreticalWire> getShortestPath(ArrayList<ArrayList<TheoreticalWire>> pathArrayList) {
            int shortestIndex = -1;
            int shortestUnits = Integer.MAX_VALUE;
            for (int i = 0; i < pathArrayList.size(); i++) {
                ArrayList<TheoreticalWire> path = pathArrayList.get(i);
                if (path != null) {
                    int units = getNumUnitsCovered(path);
                    if (units < shortestUnits) {
                        shortestIndex = i;
                        shortestUnits = units;
                    }
                }
            }
            //    System.out.println("SHORTEST INDEX = " + shortestIndex);
            return shortestIndex == -1 ? null : pathArrayList.get(shortestIndex);
        }
    }


    /**
     * This Object is very similar to a Wire, but its {@link #canConnectToGeneral(ConnectibleEntity)} method does not check the
     * {@link #isInConnectibleState()} method. This is because {@link #isInConnectibleState()} normally returns
     * false if its entity instance is not in the Circuit yet, but we want to check if TheoreticalWires can connect
     * regardless of whether or not they are on the Circuit.
     */
    public static class TheoreticalWire extends Wire {
        public TheoreticalWire(CircuitPoint start, CircuitPoint end) {
            super(start, end);
        }

        @Override
        public void onAddToCircuit() {
            throw new UnsupportedOperationException("TheoreticalWire instances should not be added to the Circuit");
        }

        @Override
        public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
            throw new UnsupportedOperationException("TheoreticalWire instances cannot connect to Entities. Tbh if you got this far tell Garn he is stupid");
        }

        @Override
        public boolean canConnectToGeneral(ConnectibleEntity other) {
            return !isInvalid() && !isSimilar(other);
        } // Doesn't care abt if it's in connectible state; we know it isn't, because it's not on the circuit.


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


    }
}