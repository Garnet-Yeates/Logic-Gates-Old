package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.LogicGates;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.Property;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyList;
import edu.wit.yeatesg.logicgates.circuit.entity.Rotatable;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class PullResistor extends ConnectibleEntity implements Rotatable {

    private CircuitPoint origin;
    private int rotation;
    private int pullDirection;

    /**
     * ConnectibleEntity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * set {@link #connections} to a new ConnectionListnu
     * call construct()
     *
     * @param origin the Circuit
     */
    public PullResistor(CircuitPoint origin, int rotation, int pullDirection) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        this.pullDirection = pullDirection;
        construct();
    }

    public PullResistor(CircuitPoint origin, int rotation) {
        this(origin, rotation, 0);
    }

    public PullResistor(CircuitPoint origin) {
        this(origin, 0);
    }

    @Override
    public boolean isSimilar(Entity other) {
        if (!(other instanceof PullResistor))
            return false;
        PullResistor o = (PullResistor) other;
        return o.origin.isSimilar(origin)
                && o.rotation == rotation
                && o.pullDirection == pullDirection;
    }

    @Override
    public PullResistor getCloned(Circuit onto) {
        return new PullResistor(origin.clone(onto), rotation, pullDirection);
    }

    public static PullResistor parse(String s, Circuit c) {
        String[] fields = s.split(",");
        return new PullResistor(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]));
    }

    @Override
    public String toParsableString() {
        // [class name] <origin.x>,<origin.y>,<rotation>
        return "[" + getClass().getSimpleName() + "]" +
                origin.x + ","
                + origin.y + ","
                + rotation + ","
                + pullDirection;
    }

    public int getPullDirection() {
        return pullDirection;
    }

    private OutputNode out;

    @Override
    public void construct() {
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        connections = new ConnectionList(this);
        establishOutputNode(origin);
        out = (OutputNode) getNodeAt(origin);
        out.setOutputType(OutputType.ANY);
        assignOutputsToInputs();

        blockWiresHere = new Map<>();
        blockWiresHere.put(new CircuitPoint(0, 0, c).getRotated(origin, rotation), rotation == 0 || rotation == 180 ? Direction.HORIZONTAL : Direction.VERTICAL);

        boundingBox = new BoundingBox(new CircuitPoint(-1, -3, c).getRotated(origin, rotation),
                new CircuitPoint(1, 0, c).getRotated(origin, rotation) , this);

        interceptPoints = new CircuitPointList();
        for (int y = 0; y > -3; y--) {
            interceptPoints.add(new CircuitPoint(0, y, c).getRotated(origin, rotation));
        }
    }

    @Override
    protected void assignOutputsToInputs() { }

    private int numIterations = 6;

    @Override
    public RelativePointSet getRelativePointSet() {
        double upshift = 0.4;
        double springUp = 0.35;
        RelativePointSet ps = getPullResistorRelativePointSet(c, numIterations, springUp, upshift);
        ps.add(0, -2.5, c); // index at numiterations + 2 is top thing center
        ps.add(0.5, -2.5, c); // index at numiterations + 3 is top thing right
        ps.add(-0.5, -2.5, c); // index at numiterations + 4 is top thing left
        ps.add(0, -2.4, c); // index at numiterations + 5 is top thing center (for neck)
        return ps;
    }

    public static RelativePointSet getPullResistorRelativePointSet(Circuit c, int numIterations, double springUp, double upshift) {
        CircuitPoint origin = new CircuitPoint(0, 0, c);
        if (numIterations < 3)
            throw new RuntimeException();
        RelativePointSet ps = new RelativePointSet();
        ps.add(origin);
        ps.add(new CircuitPoint(origin.x, origin.y - upshift, c));
        Vector movementVec = new Vector(1, -springUp);
        for (int i = 0; i < numIterations; i++) {
            Vector vec = i == 0 || i == numIterations - 1 ? movementVec.getMultiplied(0.5) : movementVec;
            ps.add(ps.get(ps.size() - 1).getIfModifiedBy(vec));
            movementVec = movementVec.getXMultiplied(-1);
        }
        return ps;
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        g.setStroke(col == null ? Color.BLACK : col);
        g.setLineWidth(getLineWidth());
        CircuitPointList dps = drawPoints;
        double[] xx = new double[numIterations + 3];
        double[] yy = new double[xx.length];
        for (int i = 0; i <= numIterations + 1; i++) {
            PanelDrawPoint pp = dps.get(i).toPanelDrawPoint();
            xx[i] = pp.x;
            yy[i] = pp.y;
        }
        PanelDrawPoint centerThing = drawPoints.get(numIterations + 5).toPanelDrawPoint();
        xx[numIterations + 2] = centerThing.x; yy[numIterations + 2] = centerThing.y;

        g.strokePolyline(xx, yy, xx.length);

        LogicGates.strokePolyLine(g, new CircuitPointList(drawPoints.get(numIterations + 3), drawPoints.get(numIterations + 4)));
        LogicGates.strokePolyLine(g, new CircuitPointList(drawPoints.get(numIterations + 3), drawPoints.get(numIterations + 4)));

        out.draw(g, col, opacity);
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
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return hasNodeAt(gridSnap);
    }

    @Override
    public boolean canCreateWireFrom(CircuitPoint locationOnThisEntity) {
        return hasNodeAt(locationOnThisEntity) && getNodeAt(locationOnThisEntity).getConnectedTo() == null;
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
        for (CircuitPoint nodeLoc : connections.getEmptyConnectionLocations())
            if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc))
                connect(e, nodeLoc);
    }

    @Override
    public PowerValue getLocalPowerStateOf(OutputNode root) {
        return PowerValue.FLOATING;
    }

    private BoundingBox boundingBox;

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox.clone();
    }

    @Override
    public CircuitPointList getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }

    @Override
    public String getDisplayName() {
        return "Pull " + (pullDirection == 0 ? "Down" : "Up") + " Resistor";
    }

    @Override
    public void move(Vector v) {
        origin = origin.getIfModifiedBy(v);
        reconstruct();
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
        return "Properties for: " + getDisplayName();
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this, c);
        propList.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        propList.add(new Property("Pull Direction", pullDirection + "", "1", "0"));
        return propList;
    }

    @Override
    public void onPropertyChangeViaTable(String propName, String old, String newVal) {
        if (isItemEntity()) {
            onPropertyChange(propName, old, newVal);
            treeItem.onClick();
        }
        else
            c.new PropertyChangeOperation(this, propName, newVal, true).operate();

    }

    @Override
    public void onPropertyChange(String propertyName, String old, String newVal) {
        if (propertyName.equalsIgnoreCase("facing")) {
            rotation = Direction.rotationFromCardinal(newVal);
            reconstruct();
        }
        if (propertyName.equalsIgnoreCase("pull direction")) {
            pullDirection = Integer.parseInt(newVal);
            reconstruct();
        }
    }

    @Override
    public String getPropertyValue(String propertyName) {
        if (propertyName.equalsIgnoreCase("facing"))
            return Direction.cardinalFromRotation(rotation);
        if (propertyName.equalsIgnoreCase("pull direction"))
            return pullDirection + "";
        return null;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyName.equalsIgnoreCase("facing")
                || propertyName.equalsIgnoreCase("pull direction");
    }
}