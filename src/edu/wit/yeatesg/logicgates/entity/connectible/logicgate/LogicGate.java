package edu.wit.yeatesg.logicgates.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;

public abstract class LogicGate extends ConnectibleEntity implements Rotatable, PropertyMutable {

    /**
     * Fuck off
     * @param origin
     * @param rotation
     * @param size
     * @param numInputs
     * @param nots
     */
    public LogicGate(CircuitPoint origin, int rotation, Size size, int numInputs, ArrayList<Integer> nots) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        this.size = size;
        this.numInputs = numInputs;
        if (nots == null)
            nots = new ArrayList<>();
        this.nots = new ArrayList<>(nots);
        if (nots.size() > numInputs)
            throw new RuntimeException("Dumb fuck 1");
        if (numInputs < 2)
            throw new RuntimeException("Dumb fuck 2");
        construct();
    }

    public LogicGate(CircuitPoint origin, int rotation, Size size, int numInputs) {
        this(origin, rotation, size, numInputs, null);
    }

    public LogicGate(CircuitPoint origin, int rotation, Size size) {
        this(origin, rotation, size, getNumBaseInputs(size));
    }

    public LogicGate(CircuitPoint origin, int rotation) {
        this(origin, rotation, Size.MEDIUM);
    }

    public LogicGate(CircuitPoint origin) {
        this(origin, 270);
    }

    @Override
    public String toParsableString() {
        // [class name] <origin.x>,<origin.y>,<rotation>,<size>,<numInputs>,<nots>
        String notString = "";
        for (int i = 0; i < nots.size(); i++)
            notString += nots.get(i) + i == nots.size() - 1 ? "" : ";";
        return "[" + getClass().getSimpleName() + "]" +
                origin.x + ","
                + origin.y + ","
                + rotation + ","
                + size + ","
                + numInputs + ","
                + notString;
    }

    protected Size size;
    protected CircuitPoint origin;
    protected int rotation;
    protected BoundingBox boundingBox;


    protected OutputNode out;

    @Override
    public void construct() {
        mainWing = getMainInputWing();
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        interceptPoints = new PointSet();
        this.nots.sort(Comparator.comparingInt(Integer::intValue));
        connections = new ConnectionList(this);
        constructNodesIntPointsAndBoundingBox();
        assignOutputsToInputs();
        // TODO not inputs after
    }

    private InputWing leftWing;
    private InputWing rightWing;
    private InputWing mainWing;

    /** Differnet logic gates have different types of input wings*/
    public abstract InputWing getInputWing(CircuitPoint start, CircuitPoint control, CircuitPoint end);

    public CircuitPoint getTailLocation(InputNode inputNode) {
        CircuitPoint loc = inputNode.getLocation();
        if (leftWing != null && leftWing.contains(inputNode.getLocation()))
            return leftWing.getBezierPointAlong(loc);
        if (rightWing != null && rightWing.contains(inputNode.getLocation()))
            return rightWing.getBezierPointAlong(loc);
        return mainWing.getBezierPointAlong(loc);
    }

    // return null if its a line
    public abstract InputWing getRelativeMainInputWing();

    public InputWing getMainInputWing() {
        InputWing relative = getRelativeMainInputWing();
        return getInputWing(relative.p1, relative.control, relative.p2);
    }

    protected abstract class InputWing {

        protected CircuitPoint p1;
        protected CircuitPoint p2;

        protected CircuitPoint control;

        protected CircuitPoint p30;

        public InputWing(CircuitPoint start, CircuitPoint control, CircuitPoint end) {
            p1 = start.getSimilar();
            p2 = end.getSimilar();
            CircuitPoint lefter = p1.y == p2.y ? (p1.x < p2.x ? p1 : p2) : (p1.y < p2.y ? p1 : p2);
            CircuitPoint righter = lefter == p1 ? p2 : p1;
            p1 = lefter;
            p2 = righter;
            this.control = control;
            Vector startToEnd = new Vector(p1, p2);
            double dist = startToEnd.getLength();
            p30 = p1.getIfModifiedBy(startToEnd.getUnitVector().getMultiplied(dist / 2));
        }

        public boolean contains(CircuitPoint cp) {
            ArrayList<CircuitPoint> interceptPoints = new ArrayList<>();
            LogicGates.lefterToRighterIterator(p1, p2).forEachRemaining(interceptPoints::add);
            return interceptPoints.contains(cp);
        }

        public CircuitPoint getMiddle() {
            return p30.getSimilar();
        }

        public abstract CircuitPoint getBezierPointAlong(CircuitPoint cp);

        public abstract void draw(GraphicsContext g, Color col);
    }

    protected class LineInputWing extends InputWing {

        public LineInputWing(CircuitPoint start, CircuitPoint end) {
            super(start, null, end);
        }

        @Override
        public CircuitPoint getBezierPointAlong(CircuitPoint cp) {
            return cp.clone(); // no implementation
        }

        public void draw(GraphicsContext g, Color col) {
            Color strokeCol = col == null ? Color.BLACK : col;
            g.setStroke(strokeCol);
            g.setLineWidth(getLineWidth());

            PanelDrawPoint p1 = this.p1.toPanelDrawPoint();
            PanelDrawPoint p2 = this.p2.toPanelDrawPoint();

            g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    protected class CurveInputWing extends InputWing {

        private BezierCurve curve;

        public CurveInputWing(CircuitPoint start, CircuitPoint controlPoint, CircuitPoint end) {
            super(start, controlPoint, end);
            curve = new BezierCurve(start, controlPoint, end);
        }

        @Override
        public CircuitPoint getBezierPointAlong(CircuitPoint cp) {
            if (rotation == 180 || rotation == 270)
                return curve.getBezierPoint(1 - BezierCurve.getWeightOfPointAlong(p1, p2, cp));
            return curve.getBezierPoint(BezierCurve.getWeightOfPointAlong(p1, p2, cp));
        }

        @Override
        public void draw(GraphicsContext g, Color col) {
            curve.draw(g, col, getLineWidth());
        }
    }

    protected void drawWings(GraphicsContext g, Color col) {
        mainWing.draw(g, col);
        if (leftWing != null && rightWing != null) {
            leftWing.draw(g, col);
            rightWing.draw(g, col);
        }
    }

    /**
     *
     * @return
     */
    public abstract RelativePointSet getRelativePointSet();

    protected int numInputs;
    protected ArrayList<Integer> nots; // the input indicies that are notted

    protected boolean hasSimilarNots(LogicGate other) {
        if (nots.size() != other.nots.size())
            return false;
        for (int i = 0; i < nots.size(); i++)
            if ((!other.nots.get(i).equals(nots.get(i))))
                return false;
        return true;
    }

    protected int numExtraInputs;

    protected static int getNumBaseInputs(Size size) {
        switch (size) {
            case SMALL:
                return 3;
            case MEDIUM:
                return 5;
            case LARGE:
                return 7;
        }
        return 0;
    }

    protected final int getNumBaseInputs() {
        return LogicGate.getNumBaseInputs(size);
    }

    protected abstract double getOuterWingXOffset();

    protected abstract double getOuterWingYOffset();


    private ArrayList<CircuitPoint> blockWiresHere = new ArrayList<>();

    public ArrayList<CircuitPoint> getWireBlockLocations() {
        return blockWiresHere;
    }

    public Direction getWireBlockDirection() {
        if (rotation == 0 || rotation == 180)
            return Direction.HORIZONTAL;
        return Direction.VERTICAL;
    }

    public void constructNodesIntPointsAndBoundingBox() {
        CircuitPoint inputOrigin = new CircuitPoint(0, 0, c).getIfModifiedBy(getOriginToInputOrigin());
        System.out.println("IN ORIGIN " + inputOrigin.toParsableString());
        int originOffset = 0;
        int dir = -1;
        if (numInputs % 2 == 0)
            originOffset++;
        int numIterations =  (numInputs % 2 == 0 ? numInputs : numInputs - 1) / 2 + 1;
        while (originOffset < numIterations) {
            CircuitPoint addingInputAt = inputOrigin.getIfModifiedBy(new Vector(originOffset, 0).getMultiplied(dir)).getRotated(origin, rotation);
            if (originOffset == numIterations - 1)
                blockWiresHere.add(addingInputAt);
            if (!addingInputAt.isSimilar(addingInputAt.getGridSnapped()))
                throw new RuntimeException("tu fucked");
            establishInputNode(addingInputAt);
            interceptPoints.add(addingInputAt);
            if (dir == 1)
                originOffset++;
            else if (originOffset == 0) {
                originOffset++;
                continue; // Skip changing the dir below
            }
            dir *= -1;
        }

        int dist = (getNumBaseInputs() - 1) / 2;

        CircuitPoint corner1 = inputOrigin.getIfModifiedBy(new Vector(-dist, 0)); // top left
        CircuitPoint corner2 = getRelativePointSet().get(0).getIfModifiedBy(new Vector(dist, 0)); // bot right
        for (CircuitPoint cp : new BoundingBox(corner1.getRotated(origin, rotation), corner2.getRotated(origin, rotation), this).getInterceptPoints())
            if (!interceptPoints.contains(cp))
                interceptPoints.add(cp);

        // Create wings, using relativePointSet. The wing class has a rotate() method
        numExtraInputs = numInputs - getNumBaseInputs();
        if (numExtraInputs > 0) {

            if (numExtraInputs % 2 != 0)
                numExtraInputs++; // If numInputs is even, then we need to add another extra input because we skip
                                  // adding the input at origin offset 0 if numInputs is even to preserve symmetry
            double xOff = getOuterWingXOffset();
            double yOff = getOuterWingYOffset();
            CircuitPoint leftWingRight = inputOrigin.getIfModifiedBy(new Vector(-1*((getNumBaseInputs() - 1.0) / 2 + xOff), 0));
            leftWingRight.y -= yOff;
            CircuitPoint leftWingLeft = leftWingRight.getIfModifiedBy(new Vector(-1*(numExtraInputs / 2.0 + 0.5) + xOff, 0));
            corner1 = leftWingLeft;
            leftWing = getInputWing(leftWingLeft, null, leftWingRight);

            CircuitPoint rightWingLeft = inputOrigin.getIfModifiedBy(new Vector((getNumBaseInputs() - 1.0) / 2 + xOff, 0));
            rightWingLeft.y -= yOff;
            CircuitPoint rightWingRight = rightWingLeft.getIfModifiedBy(new Vector(numExtraInputs / 2.0 + 0.5 - xOff, 0));

            corner2 = new CircuitPoint(rightWingRight.x, corner2.y, c);
            rightWing = getInputWing(rightWingRight, null, rightWingLeft);
        }

        boundingBox = new BoundingBox(corner1.getIfModifiedBy(new Vector(-0.49, 0)).getRotated(origin, rotation),
                corner2.getIfModifiedBy(new Vector(0.49, 0)).getRotated(origin, rotation), this);

        establishOutputNode(origin);
        out = (OutputNode) getNodeAt(origin);

        for (InputNode in : getInputNodes())
            in.setTailLocation(getTailLocation(in));

        connections.sort(rotation == 0 || rotation == 180 ? ConnectionNode.getHorizontalComparator() : ConnectionNode.getVerticalComparator());
    }

    @Override
    protected void assignOutputsToInputs() {
        getInputNodes().forEach(inputNode -> out.dependingOn().add(inputNode));
    }

    @Override
    public boolean canPullPointGoHere(CircuitPoint gridSnap) {
        return hasNodeAt(gridSnap);
    }

    @Override
    public void move(Vector v) {
        origin = origin.getIfModifiedBy(v);
        reconstruct();
    }

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox.clone();
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }

    public boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo, PermitList permits, boolean strictWithWires) {
        permits = new PermitList(permits);
        if (theo.invalidlyIntercepts(this))
            return true;
        else
            for (CircuitPoint p : theo.getInterceptPoints(this))
                if (!permits.contains(new InterceptPermit(this, p)))
                    return true;
        return false;
    }


    @Override
    public String getPropertyTableHeader() {
        return "null";
    }

    @Override
    public PropertyList getPropertyList() {
        return new PropertyList();
    }

    @Override
    public void onPropertyChange(String propertyName, String old, String newVal) {
        if (propertyName.equalsIgnoreCase("rotation")) {
            rotation = Integer.parseInt(newVal);
            reconstruct();
        }
    }

    @Override
    public String getPropertyValue(String propertyName) {
        if (propertyName.equalsIgnoreCase("rotation"))
            return rotation + "";
        return null;
    }


    @Override
    public boolean hasProperty(String propertyName) {
        return propertyName.equalsIgnoreCase("rotation");
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!canConnectTo(e, atLocation))
            throw new RuntimeException("Cannot connect these 2 entities");
        if (!hasNodeAt(atLocation))
            throw new RuntimeException("Can't connect to LogicGate here, no ConnectionNode at this CircuitPoint");
        getNodeAt(atLocation).setConnectedTo(e);
        e.getConnections().add(new ConnectionNode(atLocation, e, this));
    }

    @Override
    public boolean canCreateWireFrom(CircuitPoint locationOnThisEntity) {
        return connections.hasNodeAt(locationOnThisEntity) && getNodeAt(locationOnThisEntity).getConnectedTo() == null;
    }

    @Override
    public void disconnect(ConnectibleEntity e) {
        getConnectionTo(e).setConnectedTo(null);
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        return e instanceof Wire
                && canConnectToGeneral(e)
                && hasNodeAt(at)
                && (getNumEntitiesConnectedAt(at) == 0);
    }

    @Override
    protected void connectCheck(ConnectibleEntity e) {
        for (CircuitPoint nodeLoc : connections.getEmptyConnectionLocations()) {
            if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc)) {
                connect(e, nodeLoc);
            }
        }

    }

    @Override
    public double getLineWidth() {
        return c.getLineWidth();
    }


    public void drawInputTails(GraphicsContext g, Color col) {
        for (InputNode in : getInputNodes())
            in.drawTail(g, col);
    }


    // Vector from origin to center of inputs
    public abstract Vector getOriginToInputOrigin();

}
