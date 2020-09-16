package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.LogicGates;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.Property;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.*;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class PowerEmitter extends ConnectibleEntity {

    private CircuitPoint origin;
    private int rotation;

    /**
     * ConnectibleEntity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * set {@link #connections} to a new ConnectionList
     * call construct()
     *
     * @param origin the Circuit
     */
    public PowerEmitter(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    public PowerEmitter(CircuitPoint origin) {
        this(origin, 0);
    }

    @Override
    public String toParsableString() {
        // [class name] <origin.x>,<origin.y>,<rotation>
        return "[" + getClass().getSimpleName() + "]" +
                origin.x + ","
                + origin.y + ","
                + rotation;
    }

    public static PowerEmitter parse(String s, Circuit c) {
        String[] fields = s.split(",");
        return new PowerEmitter(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]));
    }

    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof PowerEmitter
                && ((PowerEmitter) other).origin.isSimilar(origin)
                && ((PowerEmitter) other).rotation == rotation;
    }

    @Override
    public PowerEmitter getCloned(Circuit onto) {
        return new PowerEmitter(origin.clone(onto), rotation);
    }

    private OutputNode out;

    private ArrayList<CircuitPoint> trianglePoints;
    private ArrayList<CircuitPoint> neckPoints;

    @Override
    public void construct() {
        trianglePoints = new ArrayList<>();
        trianglePoints.add(new CircuitPoint(0, -0.5, c).getRotated(origin, rotation));
        trianglePoints.add(new CircuitPoint(0.75, -0.5, c).getRotated(origin, rotation));
        trianglePoints.add(new CircuitPoint(0, -1.5, c).getRotated(origin, rotation));
        trianglePoints.add(new CircuitPoint(-0.75, -0.5, c).getRotated(origin, rotation));
        trianglePoints.add(new CircuitPoint(0, -0.5, c).getRotated(origin, rotation));

        neckPoints = new ArrayList<>();
        neckPoints.add(new CircuitPoint(0, -0.4, c).getRotated(origin, rotation));
        neckPoints.add(new CircuitPoint(0, 0, c).getRotated(origin, rotation));

        connections = new ConnectionList(this);
        establishOutputNode(origin);
        out = (OutputNode) getNodeAt(origin);
        out.setOutputType(OutputType.ZERO_AS_FLOATING);
        assignOutputsToInputs();

        boundingBox = new BoundingBox(new CircuitPoint(-1, -1.5, c).getRotated(origin, rotation),
                new CircuitPoint(1, 0, c).getRotated(origin, rotation) , this);

        interceptPoints = boundingBox.getInterceptPoints();

    }

    @Override
    protected void assignOutputsToInputs() { }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        g.setStroke(col == null ? out.getPowerValue().getColor() : col);

        g.setLineWidth(getLineWidth());
        LogicGates.strokePolyLine(g, trianglePoints);
        LogicGates.strokePolyLine(g, neckPoints);
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
        return PowerValue.ON;
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
        return "Power";
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
        return "Properties for Power";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this, c);
        propList.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
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
    }

    @Override
    public String getPropertyValue(String propertyName) {
        if (propertyName.equalsIgnoreCase("facing"))
            return Direction.cardinalFromRotation(rotation);
        return null;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyName.equalsIgnoreCase("facing");
    }
}