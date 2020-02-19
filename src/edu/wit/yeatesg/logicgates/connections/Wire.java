package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Entity;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class Wire extends ConnectibleEntity {

    protected CircuitPoint startLocation;
    protected CircuitPoint endLocation;

    public Wire(CircuitPoint startLocation, CircuitPoint endLocation) {
        super(startLocation.getCircuit());
        if ((startLocation.x != endLocation.x && startLocation.y != endLocation.y) || startLocation.equals(endLocation))
            throw new RuntimeException("Invalid Wire");
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        for (Wire w : c.getAllEntitiesOfType(Wire.class))
            if (isSimilar(w))
                throw new RuntimeException("Duplicate/Similar Wire " + startLocation + " " + endLocation);
        c.addEntity(this);
        bisectCheck();
        mergeCheck();
        connectCheck();
    }

    protected Wire(Circuit c) {
        super(c);
    }

    @Override
    public PointSet getInterceptPoints() {
        PointSet interceptPoints = new PointSet();
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
        return interceptPoints;
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        return null;
    }


    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(startLocation, endLocation, this);

    }

    public Direction getDirection() {
        return startLocation.x == endLocation.x ? Direction.VERTICAL : Direction.HORIZONTAL;
    }

    public PointSet getPointsExcludingEndpoints() {
        PointSet pts = getInterceptPoints();
        pts.remove(0);
        pts.remove(pts.size() - 1);
        return pts;
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {
        for (CircuitPoint edgePoint : getEdgePoints())
            if (canConnectTo(e, edgePoint) && e.canConnectTo(this, edgePoint) && !deleted && !e.isDeleted())
                connect(e, edgePoint);
    }

    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!atLocation.equals(startLocation) && !atLocation.equals(endLocation))
            throw new RuntimeException("Wires can only be connected at end points! (w1)");
        if (hasConnectionTo(e) || e.hasConnectionTo(this))
            throw new RuntimeException(this + " is Already Connected To " + e);
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
        }
        c.refreshTransmissions();
    }

    public void bisectCheck() {
        for (Wire w : c.getAllEntitiesOfType(Wire.class).thatAreNotDeleted().except(this)) {
            bisectCheck(w);
            w.bisectCheck(this);
        }
    }

    public void bisectCheck(Wire other) {
        for (CircuitPoint thisWiresEndpoint : new CircuitPoint[] { startLocation, endLocation })
            if (other.getPointsExcludingEndpoints().contains(thisWiresEndpoint)
                    && other.getDirection() != getDirection()
                    && !(other.deleted || deleted))  // This means it is bisecting the wire
                bisect(other, thisWiresEndpoint);
    }

    public void bisect(Wire other, CircuitPoint endpointOfThisWire) {
        System.out.println("\n" + this + "\nBISECT\n" + other + "\nAT\n" + endpointOfThisWire + "\n");
        if (!other.getPointsExcludingEndpoints().contains(endpointOfThisWire))
            throw new RuntimeException("Invalid Wire Bisect. Can't bisect a wire at its edge points");
        if (!(endpointOfThisWire.equals(startLocation) || endpointOfThisWire.equals(endLocation)))
            throw new RuntimeException("Invalid Wire Bisect. This wire's end point must be between the other wire's" +
                    " endpoints (but cant touch the endpoints) to bisect it");
        CircuitPoint oldStartLoc = other.startLocation;
        other.startLocation = endpointOfThisWire;
        new Wire(endpointOfThisWire, oldStartLoc);
        ConnectibleEntity.checkEntities(oldStartLoc, endpointOfThisWire);
        c.refreshTransmissions();
        c.getEditorPanel().repaint();
    }

    public void mergeCheck() {
        ArrayList<Wire> allWires = c.getAllEntitiesOfType(Wire.class, true).thatAreNotDeleted();
        allWires.remove(this);
        for (Wire w : allWires)
            if (!deleted)
                mergeCheck(w);
    }

    public boolean isSimilar(Wire w) {
        return (w.startLocation.equals(startLocation) && w.endLocation.equals(endLocation))
                || (w.startLocation.equals(endLocation) && w.endLocation.equals(startLocation));
    }

    public void mergeCheck(Wire other) {
        for (CircuitPoint edgePoint : new CircuitPoint[] { startLocation, endLocation }) {
            for (CircuitPoint otherEdgePoint : new CircuitPoint[] { other.startLocation, other.endLocation }) {
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
        System.out.println("OTHER START: " + other.startLocation);
        System.out.println("OTHER END: " + other.endLocation);
        System.out.println("COMMON EDGE: " + commonEdgePoint);
        other.delete(); // Checks done in delete
        set(commonEdgePoint, other.getOppositeEdgePoint(commonEdgePoint)); // Checks done in set
        c.refreshTransmissions();
        c.getEditorPanel().repaint();
    }

    @Override
    public void onPowerReceive() {
        if (!receivedPowerThisUpdate) {
            super.onPowerReceive();
            for (ConnectibleEntity ent : getConnectedEntities()) {
                if (!ent.receivedPowerThisUpdate)
                    ent.onPowerReceive();
            }
        }
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

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        if (isEdgePoint(at)
                && !hasConnectionTo(e)
                && getNumOtherEdgePointsAt(at) < 4
                && !e.isDeleted()) {
            if (e instanceof Wire) {
                Wire other = (Wire) e;
                return getDirection() != other.getDirection() || other.getNumOtherEdgePointsAt(at) > 1;
            } else
                return true;
        }
        return false;
    }

    @Override
    public boolean canMoveBy(Vector movementVector) {
        return true;
    }

    public void draw(Graphics2D g, boolean drawJunctions) {
        g.setColor(getColor());
        int wireStroke = (int) (c.getScale() * 0.25);
        if (wireStroke % 2 == 0) wireStroke++;
        g.setStroke(new BasicStroke(wireStroke));
        PanelDrawPoint p1 = startLocation.toPanelDrawPoint();
        PanelDrawPoint p2 = endLocation.toPanelDrawPoint();
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        if (drawJunctions)
            for (CircuitPoint edgePoint : new CircuitPoint[] { startLocation, endLocation })
                if (getNumEntitiesConnectedAt(edgePoint) > 1) {
                    drawJunction(g, edgePoint);
                }
    }

    public void drawJunction(Graphics2D g, CircuitPoint loc) {
        g.setColor(getColor());
        int circleSize = (int) (getStrokeSize() * 2.75);
        if (circleSize % 2 != 0) circleSize++;
        PanelDrawPoint dp = loc.toPanelDrawPoint();
        g.fillOval(dp.x - circleSize / 2, dp.y - circleSize / 2, circleSize, circleSize);
    }

    @Override
    public void draw(Graphics2D g) {
        draw(g, true);
    }

    @Override
    public int getStrokeSize() {
        int stroke = (int) (c.getScale() * 0.25);
        if (stroke % 2 == 0) stroke++;
        return stroke;
    }



    @Override
    public boolean intercepts(CircuitPoint p) {
        return getInterceptPoints().contains(p);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Wire && ((Wire) other).startLocation.equals(startLocation) &&
                ((Wire) other).endLocation.equals(endLocation);
    }

    @Override
    public String toString() {
        return "Wire{" +
                "startLocation=" + startLocation +
                ", endLocation=" + endLocation +
                '}';
    }

    public static List<TheoreticalWire> generateWirePath(CircuitPoint from,
                                                         CircuitPoint to,
                                                         int maxLength) {
        return generateWirePath(from, to, new ArrayList<>(), null, 1, maxLength, true, to);
    }

    public static List<TheoreticalWire> generateWirePath(CircuitPoint from,
                                                         CircuitPoint to,
                                                         Vector initialDir,
                                                         int maxLength) {
        System.out.println("Generate Wire Path From " + from + " to " + to + " with initial vector " + initialDir);
        return generateWirePath(from, to, new ArrayList<>(), initialDir, 1, maxLength, true, to);
    }

    private static List<TheoreticalWire> generateWirePath(CircuitPoint start,
                                                          CircuitPoint end,
                                                          List<TheoreticalWire> currPath,
                                                          Vector currDirection,
                                                          int currLength,
                                                          int maxLength,
                                                          boolean canSplit,
                                                          CircuitPoint absoluteEnd) {
        System.out.println("FROM: " + start + " TO " + end + " DIR " + currDirection);
        System.out.println("  CURR DIR: " + currDirection);
        if (invalidDirectionForPath(currDirection, start, end))
            currDirection = findValidDirectionForPath(start, end);
        System.out.println("  DIR AFTER CHECK: " + currDirection);
        Circuit c = start.getCircuit();
       /* debug("CURR ITERATION", currLength, "MAX ITERATION", maxLength, "ALL ENTITIES",
                start.getCircuit().getAllEntities(), "START", start, "END", end, "CURR PATH", currPath,
                "CURR DIR", currDirection, "GENERAL DIR", Vector.getGeneralDirection(currDirection));*/
        if (currLength > maxLength)
            return null;
        for (Entity e : c.getAllEntities())
            if (e.interceptsAll(start, end))
                return null;
        if (currDirection == null) {
            Vector vert = Vector.directionVectorFrom(start, end, Direction.VERTICAL);
            Vector horiz = Vector.directionVectorFrom(start, end, Direction.HORIZONTAL);
            List<List<TheoreticalWire>> possiblePaths = new ArrayList<>();
            if (vert != null)
                possiblePaths.add(generateWirePath(start, end, new ArrayList<>(currPath),
                        vert, currLength, maxLength, canSplit, absoluteEnd));
            if (horiz != null)
                possiblePaths.add(generateWirePath(start, end, new ArrayList<>(currPath),
                        horiz, currLength, maxLength, canSplit, absoluteEnd));
            return getShortestPath(possiblePaths);
        } else {
            if (start.isInLineWith(end)) {
                CircuitPoint trying = end.clone();
                Vector oppositeDir = currDirection.getMultiplied(-1);
                TheoreticalWire chosenWire = null;
                while (!trying.equals(start)) {
                    TheoreticalWire potentialWire = new TheoreticalWire(start, trying);
                    if (Wire.canPlaceWireWithoutInterceptingAnything(potentialWire, currPath, start, absoluteEnd)) {
                        chosenWire = potentialWire;
                        break;
                    }
                    trying = oppositeDir.addedTo(trying);
                }
                if (chosenWire == null) {
                    return null;
                } else if (chosenWire.getEndLocation().equals(end)) {
                    currPath.add(chosenWire);
                    return currPath;
                } else if (canSplit) {
                    return null;
                    // Do split shit
                } else
                    return null;
            } else {
                CircuitPoint trying;
                if (Vector.getGeneralDirection(currDirection) == Direction.HORIZONTAL)
                    trying = new CircuitPoint(end.x, start.y, start.getCircuit());
                else
                    trying = new CircuitPoint(start.x, end.y, start.getCircuit());
                Vector oppositeDir = currDirection.getMultiplied(-1);
                ArrayList<TheoreticalWire> potentials = new ArrayList<>();
                while (!trying.equals(start)) {
                    TheoreticalWire potentialWire = new TheoreticalWire(start, trying);
                    boolean canPlace = Wire.canPlaceWireWithoutInterceptingAnything(potentialWire, currPath, start, absoluteEnd);
                    if (canPlace)
                        potentials.add(potentialWire);
                    trying = oppositeDir.addedTo(trying);
                }
                if (currLength == 1) { // If first iteration, try all potential sub wires
                    List<List<TheoreticalWire>> paths = new ArrayList<>();
                    for (TheoreticalWire theo : potentials) {
                        ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                        currPathClone.add(theo);
                        paths.add(generateWirePath(theo.getEndLocation(),
                                end, currPathClone, null, ++currLength, maxLength, canSplit, absoluteEnd));
                    }
                    return getShortestPath(paths);
                } else if (!potentials.isEmpty()) {
                    System.out.println("  WE GOT OURSELVES AN ENDPOINT: " + trying);
                    currPath.add(potentials.get(0));
                    return generateWirePath(potentials.get(0).getEndLocation(),
                            end, currPath, null, ++currLength, maxLength, canSplit, absoluteEnd);
                }
                else
                    return null;
            }
        }
    }

    public boolean set(CircuitPoint edgePoint, CircuitPoint to) {
        if (!isEdgePoint(edgePoint))
            throw new RuntimeException();
        if (to.equals(getOppositeEdgePoint(edgePoint))) {
            delete();
        }
        else if (whichEdgePoint(edgePoint).equals("start")) {
            startLocation = to;
        }
        else if (whichEdgePoint(edgePoint).equals("end"))
            endLocation = to;

        if (!deleted)
            ConnectibleEntity.checkEntities(edgePoint, startLocation, endLocation);

        return deleted;
    }

    @Override
    public void onDelete() {
        disconnectAll();
        deleted = true;
        ConnectibleEntity.checkEntities(startLocation, endLocation);
        c.refreshTransmissions();
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

    public String whichEdgePoint(CircuitPoint p) {
        return p.equals(startLocation) ? "start" : (p.equals(endLocation) ? "end" : "none");
    }

    public static boolean invalidDirectionForPath(Vector dir, CircuitPoint start, CircuitPoint end) {
        return (dir != null) && (dir.equals(Vector.LEFT) && start.x <= end.x
                || dir.equals(Vector.RIGHT) && start.x >= end.x
                || dir.equals(Vector.DOWN) && start.y >= end.y
                || dir.equals(Vector.UP) && start.y <= end.y);
    }

    public static Vector findValidDirectionForPath(CircuitPoint start, CircuitPoint end) {
        for (Vector v : Vector.getDirectionVecs())
            if (!invalidDirectionForPath(v, start, end) && generateWirePath(start, end, v, 8) != null)
                return v;
        return null;
    }

    public static List<TheoreticalWire> getShortestPath(List<List<TheoreticalWire>> pathList) {
        int shortestIndex = -1;
        int shortestLength = Integer.MAX_VALUE;
        for (int i = 0; i < pathList.size(); i++) {
            List<TheoreticalWire> path = pathList.get(i);
            if (path != null && path.size() < shortestLength) {
                shortestLength = path.size();
                shortestIndex = i;
            }
        }
        return shortestIndex == -1 ? null : pathList.get(shortestIndex);
    }

    public static boolean canPlaceWireWithoutInterceptingAnything(TheoreticalWire wire,
                                                                  List<? extends Entity> alsoCantIntercept,
                                                                  CircuitPoint... exceptionsForList) {
        ArrayList<Entity> checkingForInterceptions = wire.getCircuit().getAllEntities();
        checkingForInterceptions.addAll(alsoCantIntercept);
        for (Entity e : wire.getCircuit().getAllEntities())
            if (e.doesGenWireInvalidlyInterceptThis(wire, exceptionsForList))
                return false;
        return true;
    }


    public CircuitPoint[] getEdgePoints() {
        return new CircuitPoint[] {startLocation,endLocation};
    }

    // If u dont want a wire to intercept a wire in a way where it will connect to it, use this method
    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire theo, CircuitPoint... exceptions) {
        ArrayList<CircuitPoint> interceptPoints = new ArrayList<>();
        if (theo.getDirection() == getDirection()) { // Can't intercept any point on any wire going in the same dir
            for (CircuitPoint cp : getInterceptPoints()) {
                for (CircuitPoint cp2 : theo.getInterceptPoints()) {
                    if (cp.equals(cp2))
                        interceptPoints.add(cp);
                }
            }
        } else {
            // THE GENERATED WIRE IS ALLOWED TO TOUCH THE ENDPOINT OF A NON GENERATED WIRE
            // BUT THE NON GENERATED WIRE IS NOT ALLOWED TO TOUCH THE ENDPOINT OF A GEND WIRE
            for (CircuitPoint edgePoint : theo.getEdgePoints())
                if (this.intercepts(edgePoint))
                    interceptPoints.add(edgePoint);


            // FOR THE SECOND AUTO PATHT HING IM DOING LATER WHERE WE DONT WANT IT TO CONNECT TO
            // ANNNNNNNNNNNNYTHING AT ALL BELOW:
            // Endpoints of each wire cant touch any of the points on the other wire if diff direction
           /* for (CircuitPoint cp : w.getPoints())
                if (startLocation.equals(cp) || endLocation.equals(cp))
                    interceptPoints.add(cp);
            for (CircuitPoint cp : getPoints())
                if (w.getStartLocation().equals(cp) || w.getEndLocation().equals(cp))
                    interceptPoints.add(cp);*/
        }
        if (interceptPoints.size() != 0) {
            for (CircuitPoint interceptPoint : interceptPoints) {
                boolean exceptionsContainsPoint = false;
                for (CircuitPoint p : exceptions) {
                    if (p.equals(interceptPoint)) {
                        exceptionsContainsPoint = true;
                        break;
                    }
                }
                if (!exceptionsContainsPoint)
                    return true;
            }
        }
        return false;
    }

    public CircuitPoint getStartLocation() {
        return startLocation;
    }

    public CircuitPoint getEndLocation() {
        return endLocation;
    }

    public static class TheoreticalWire extends Wire {
        public TheoreticalWire(CircuitPoint start, CircuitPoint end) {
            super(start.getCircuit());
            if (start == null || end == null || (start.x != end.x && start.y != end.y) || start.equals(end))
                throw new RuntimeException("Invalid Theoretical Wire");
            this.startLocation = start;
            this.endLocation = end;
        }

        private Color color = Color.orange;

        @Override
        public Color getColor() {
            return color;
        }

        public void setColor(Color col) {
            color = col;
        }

        @Override
        public String toString() {
            return "THEOTHEOTHEO" + super.toString();
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