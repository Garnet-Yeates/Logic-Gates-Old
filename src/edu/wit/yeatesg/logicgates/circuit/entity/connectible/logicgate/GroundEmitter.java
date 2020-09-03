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
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.awt.*;
import java.util.ArrayList;

public class GroundEmitter extends ConnectibleEntity {

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
    public GroundEmitter(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    public GroundEmitter(CircuitPoint origin) {
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

    public static GroundEmitter parse(String s, Circuit c) {
        String[] fields = s.split(",");
        return new GroundEmitter(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]));
    }

    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof GroundEmitter
                && ((GroundEmitter) other).origin.isSimilar(origin)
                && ((GroundEmitter) other).rotation == rotation;
    }

    @Override
    public GroundEmitter getCloned(Circuit onto) {
        return new GroundEmitter(origin, rotation);
    }


    private OutputNode out;

    private CircuitPointList[] polyLines;

    @Override
    public void construct() {
        double totalHeight = 1.5;
        double neckLength = 0.5;
        double workingLength = totalHeight - neckLength;
        double startOut = 0.9;
        double endOut = 0.3;

        double distOut = startOut;
        polyLines = new CircuitPointList[4];
        double y = -neckLength;
        for (int i = 0; i < 3; i++, distOut -= (startOut - endOut) / 2, y -= workingLength / 2) {
            polyLines[i] = new CircuitPointList((new CircuitPoint(distOut, y, c).getRotated(origin, rotation)),
                    new CircuitPoint(-distOut, y, c).getRotated(origin, rotation));
        }
        polyLines[3] = new CircuitPointList(new CircuitPoint(0, 0, c).getRotated(origin, rotation),
                new CircuitPoint(0, -neckLength*0.8, c).getRotated(origin, rotation));

        connections = new ConnectionList(this);
        establishOutputNode(origin);
        out = (OutputNode) getNodeAt(origin);
        out.setOutputType(OutputType.ONE_AS_FLOATING);
        assignOutputsToInputs();

        boundingBox = new BoundingBox(new CircuitPoint(-1, -1.5, c).getRotated(origin, rotation),
                new CircuitPoint(1, 0, c).getRotated(origin, rotation) , this);

        interceptPoints = boundingBox.getInterceptPoints();
    }

    @Override
    protected void assignOutputsToInputs() { }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        g.setStroke(col == null ? Color.BLACK : col);
        g.setStroke(col == null ? out.getPowerValue().getColor() : col);
        g.setLineWidth(getLineWidth());

        double barStrokeSize = getLineWidth() * 1;
        double neckStrokeSize = getLineWidth();

        g.setLineWidth(barStrokeSize);
        for (int i = 0; i < 3; i++)
            LogicGates.strokePolyLine(g, polyLines[i]);

        g.setLineWidth(neckStrokeSize);
        LogicGates.strokePolyLine(g, polyLines[3]);

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
        return PowerValue.OFF;
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
        return "Ground";
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
        return "Properties for: Ground";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this, c);
        propList.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        return propList;
    }

    @Override
    public void onPropertyChangeViaTable(String propName, String old, String newVal) {
        if (isTemplateEntity())
            onPropertyChange(propName, old, newVal);
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