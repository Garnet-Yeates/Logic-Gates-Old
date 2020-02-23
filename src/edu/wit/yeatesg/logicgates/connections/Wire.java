package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;
import java.util.*;
import java.util.List;

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
                throw new RuntimeException("Duplicate/Similar Wire " + startLocation + " " + endLocation );
        c.addEntity(this);
        connectCheck();
        c.getEditorPanel().repaint();
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

    @Override
    public void connectCheck() {
        bisectCheck();
        mergeCheck();
        super.connectCheck();
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
        for (CircuitPoint thisWiresEndpoint : new CircuitPoint[]{startLocation, endLocation})
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

        if (!deleted)
            ConnectibleEntity.checkEntities(edgePoint, startLocation, endLocation);
    }

    @Override
    public void onDelete() {
        disconnectAll();
        deleted = true;
        ConnectibleEntity.checkEntities(startLocation, endLocation);
        c.refreshTransmissions();
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
            for (CircuitPoint edgePoint : new CircuitPoint[]{startLocation, endLocation})
                if (getNumEntitiesConnectedAt(edgePoint) > 1)
                    drawJunction(g, edgePoint);
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

    public static ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                          CircuitPoint end,
                                                          Direction initialDir,
                                                          int maxLength,
                                                          boolean strictWithWires) {
        System.out.println("GEN FROM " + start + " to " + end + ", dir = " + initialDir);
        PermitList permitList = new PermitList();
        for (ConnectibleEntity ce : start.getCircuit().getAllEntitiesThatIntercept(end).ofType(ConnectibleEntity.class))
            permitList.add(new InterceptPermit(ce, end));
        return genWirePath(start, end, new ArrayList<>(), initialDir, maxLength, 0, end, permitList, strictWithWires);
    }

    private static ArrayList<TheoreticalWire> genWirePath(CircuitPoint start,
                                                           CircuitPoint end,
                                                           ArrayList<TheoreticalWire> currPath,
                                                           Direction currDirection,
                                                           int maxLength,
                                                           int splitNum,
                                                           CircuitPoint absoluteEnd,
                                                           PermitList permits,
                                                           boolean strictWithWires) {
        System.out.println("gen from " + start + " to " + end);
        if (currPath.size() == 0)
            for (Entity e : start.getCircuit().getAllEntitiesThatIntercept(start))
                permits.add(new InterceptPermit(e, start));
        if (currDirection == null) {
            // try shortest between vert and horiz
            ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
            paths.add(genWirePath(start, end, currPath, Direction.HORIZONTAL, maxLength, splitNum, absoluteEnd,
                    permits, strictWithWires));
            paths.add(genWirePath(start, end, currPath, Direction.VERTICAL, maxLength, splitNum, absoluteEnd,
                    permits, strictWithWires));
            return getShortestPath(paths);
        }
        if (currPath.size() > maxLength)
            return null;
        if (start.isInLineWith(end)) {
            //      System.out.println("en lin");
            currDirection = directionVecFromInLinePoints(start, end).getGeneralDirection(); // Fix dir potentially
            TheoreticalWire potentialWire = getClosestSubWire(start, end, currPath, permits, strictWithWires);
            if (potentialWire != null && potentialWire.getEndLocation().equals(end)) {
                currPath.add(potentialWire);
                return currPath;
            } else if (splitNum++ < 2) {
                Vector[] orthogonals = currDirection == Direction.HORIZONTAL ?
                        new Vector[]{Vector.UP, Vector.DOWN} : new Vector[]{Vector.LEFT, Vector.RIGHT};
                int splitDist = 3;
                CircuitPoint orthogonalStart = start;
                if (potentialWire != null) {
                    currPath.add(potentialWire);
                    orthogonalStart = potentialWire.getEndLocation();
                    permits.add(new InterceptPermit(potentialWire, orthogonalStart));
                }

                ArrayList<TheoreticalWire> potentials = new ArrayList<>();
                for (Vector v : orthogonals) {
                    CircuitPoint orthogonalEnd = v.getMultiplied(splitDist).addedTo(orthogonalStart);
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
                            currPathClone, currDirection, maxLength, splitNum, absoluteEnd,
                            exceptionsClone, strictWithWires); // Dont switch dir because we already did perpendicular
                    if (generated != null)
                        paths.add(generated);

                }
                return getShortestPath(paths);
            } else
                return null;
        } else {
            CircuitPoint trying = getPointInLineWith(start, end, currDirection);
            if (currPath.size() == 0) {
                final int overShoot = 4;
                Vector overShootVec = directionVecFromInLinePoints(start, trying).getMultiplied(overShoot);
                ArrayList<TheoreticalWire> potentials = getAllSubWires(start, overShootVec.addedTo(trying), currPath,
                        permits, strictWithWires);
                final ArrayList<ArrayList<TheoreticalWire>> paths = new ArrayList<>();
                final Direction currDir = currDirection;
                final int splitNumFin = splitNum;
                int lastN = 20;
                ArrayList<Thread> threads = new ArrayList<>();
                for (TheoreticalWire theo : potentials) {
                    if (lastN-- > 0) {
                        threads.add(new Thread(() -> {
                            ArrayList<TheoreticalWire> currPathClone = new ArrayList<>(currPath);
                            currPathClone.add(theo);
                            PermitList exceptionsClone = new PermitList(permits);
                            exceptionsClone.add(new InterceptPermit(theo, theo.getEndLocation()));
                            Direction nextDir = currDir.getPerpendicular();
                            ArrayList<TheoreticalWire> generated = genWirePath(theo.getEndLocation(), end, currPathClone, nextDir,
                                    maxLength, splitNumFin, absoluteEnd, exceptionsClone, strictWithWires);
                            if (generated != null)
                                paths.add(generated);
                        }));
                    }
                }
                for (Thread t : threads)
                    t.start();
                for (Thread t : threads)
                    try { t.join(); } catch (InterruptedException ignored) { }
                return getShortestPath(paths);
            } else {
                TheoreticalWire potential = getClosestSubWire(start, trying, currPath, permits, strictWithWires);
                if (potential != null) {
                    currPath.add(potential);
                    Direction nextDirection = currDirection.getPerpendicular();
                    permits.add(new InterceptPermit(potential, potential.getEndLocation()));
                    return genWirePath(potential.getEndLocation(), end, currPath, nextDirection, maxLength, splitNum, absoluteEnd,
                            permits, strictWithWires);
                }
                return null;
            }
        }
    }


    public static TheoreticalWire getClosestSubWire(CircuitPoint start,
                                                               CircuitPoint end,
                                                               List<TheoreticalWire> alsoCantIntercept,
                                                               PermitList exceptions,
                                                               boolean strictWithWires) {
        return getClosestSubWire(start, end, start, alsoCantIntercept, exceptions, strictWithWires);
    }

    public static TheoreticalWire getClosestSubWire(CircuitPoint start,
                                                               CircuitPoint end,
                                                               CircuitPoint stop,
                                                               List<TheoreticalWire> alsoCantIntercept,
                                                               PermitList exceptions,
                                                               boolean strictWithWires) {
        ArrayList<TheoreticalWire> subWires = getSubWires(start, end, stop, alsoCantIntercept,
                exceptions, strictWithWires, true);
        return subWires.size() == 0 ? null : subWires.get(0);
    }

    public static ArrayList<TheoreticalWire> getAllSubWires(CircuitPoint start,
                                                         CircuitPoint end,
                                                         List<TheoreticalWire> alsoCantIntercept,
                                                         PermitList exceptions,
                                                         boolean strictWithWires) {
        return getAllSubWires(start, end, start, alsoCantIntercept, exceptions, strictWithWires);
    }

    public static ArrayList<TheoreticalWire> getAllSubWires(CircuitPoint start,
                                                            CircuitPoint end,
                                                            CircuitPoint stop,
                                                            List<TheoreticalWire> alsoCantIntercept,
                                                            PermitList exceptions,
                                                            boolean strictWithWires) {
        return getSubWires(start, end, stop, alsoCantIntercept, exceptions, strictWithWires, false);
    }

    public static ArrayList<TheoreticalWire> getSubWires(CircuitPoint start,
                                                          CircuitPoint end,
                                                          CircuitPoint stop,
                                                          List<TheoreticalWire> cantIntercept,
                                                          PermitList exceptions,
                                                          boolean strictWithWires,
                                                          boolean stopAtFirst) {
        if (!start.isInLineWith(end) || start.equals(end))
            throw new RuntimeException("Invalid sub-wire Points " + start + " and " + end);
        Vector dir = Vector.directionVectorFrom(end, start, start.x == end.x ? Direction.VERTICAL : Direction.HORIZONTAL);
        ArrayList<TheoreticalWire> potentials = new ArrayList<>();
        while (!end.equals(stop)) {
            TheoreticalWire potentialWire = new TheoreticalWire(start, end);
            boolean canPlace = Wire.canPlaceWireWithoutInterceptingAnything(potentialWire, cantIntercept, exceptions, strictWithWires);
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
                                                                  PermitList exceptions,
                                                                  boolean strictWithWires) {
        for (Entity e : wire.getCircuit().getAllEntities(false))
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
                for (CircuitPoint p : theo.getPointsExcludingEndpoints())
                    exceptions.add(new InterceptPermit(this, p));
 //       LogicGates.debug("Exceptions:",  exceptions);
        if (theo.invalidlyIntercepts(this)) {
 //           System.out.println("invalidly intercepts");
            return true;
        }
        else {
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


    public static class TheoreticalWire extends Wire {
        public TheoreticalWire(CircuitPoint start, CircuitPoint end) {
            super(start.getCircuit());
            if (end == null || (start.x != end.x && start.y != end.y) || start.equals(end))
                throw new RuntimeException("Invalid Theoretical Wire " + start + " to " + end);
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