package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;

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

import java.util.ArrayList;

public class GateNOT extends ConnectibleEntity implements Rotatable {

    private OutputNode out;
    private InputNode in;

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
    public GateNOT(CircuitPoint origin, int rotation, Size size) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        this.size = size;
        construct();
    }

    public GateNOT(CircuitPoint origin, int rotation) {
        this(origin, rotation, Size.NORMAL);
    }


    public GateNOT(CircuitPoint origin) {
        this(origin, 0);
    }

    public static GateNOT parse(String s, Circuit c) {
        String[] fields = s.split(",");
        return new GateNOT(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]), Size.fromString(fields[3]));
    }

    @Override
    public void construct() {
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        boundingBox = new BoundingBox(drawPoints.get(3), drawPoints.get(5), this);
        if (size == NORMAL)
            interceptPoints = boundingBox.getInterceptPoints();
        else {
            interceptPoints = new CircuitPointList();
            interceptPoints.add(drawPoints.get(0));
            interceptPoints.add(drawPoints.get(6));
            interceptPoints.add(drawPoints.get(7));
        }
        connections = new ConnectionList(this);
        establishOutputNode(origin);
        out = (OutputNode) getNodeAt(origin);
        establishInputNode(drawPoints.get(6));
        in = (InputNode) getNodeAt(drawPoints.get(6));
        assignOutputsToInputs();
    }

    @Override
    protected void assignOutputsToInputs() {
        out.assignToInput(in);
    }

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet ps = new RelativePointSet();
        if (size == NORMAL) {
            ps.add(0, 0, c); // origin               0
            ps.add(0, -0.5, c); // Circle center     1
            ps.add(0, -1, c); // tip                 2
            ps.add(-1, -3, c); // tL of tri          3
            ps.add(1, -3, c); // tR of tri           4
            ps.add(1, 0, c); // bR of bbox           5
            ps.add(0, -3, c); // in node loc
        } else {
            ps.add(0, 0, c); // origin               0
            ps.add(0, -0.4, c); // Circle center     1
            ps.add(0, -0.8, c); // tip               2
            ps.add(-0.65, -2, c); // tL of tri          3
            ps.add(0.65, -2, c); // tR of tri           4
            ps.add(0.65, 0, c); // bR of bbox           5
            ps.add(0, -2, c); // in node loc           6
            ps.add(0, -1, c);                      //  7 (last int point of the 3)
        }

        return ps;
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        g.setStroke(col == null ? Color.BLACK : col);
        g.setLineWidth(getLineWidth());
        ConnectionNode.drawNegationCircle(g, col, drawPoints.get(1), size == NORMAL ? 1 : 0.8);
        PanelDrawPoint triBot = drawPoints.get(2).toPanelDrawPoint();
        PanelDrawPoint triTL = drawPoints.get(3).toPanelDrawPoint();
        PanelDrawPoint triTR = drawPoints.get(4).toPanelDrawPoint();
        g.strokeLine(triBot.x, triBot.y, triTL.x, triTL.y);
        g.strokeLine(triTL.x, triTL.y, triTR.x, triTR.y);
        g.strokeLine(triTR.x, triTR.y, triBot.x, triBot.y);
        out.draw(g, col, opacity);
        in.draw(g, col, opacity);
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
        ArrayList<PowerValue> relevants = out.getRelevantPowerValuesAffectingMe();
        if (relevants.isEmpty())
            return PowerValue.FLOATING_ERROR;
        return relevants.get(0).getNegated();
    }

    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof GateNOT
                && ((GateNOT) other).origin.isSimilar(origin);
    }

    @Override
    public GateNOT getCloned(Circuit onto) {
        return new GateNOT(origin.clone(onto), rotation, size);
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
        return "NOT Gate";
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
    public String toParsableString() {
        // [class name] <origin.x>,<origin.y>,<rotation>,<size>,<numInputs>,<negate>,<nots>,<outtype.simplestring>,<numbits>
        return "[" + getClass().getSimpleName() + "]" +
                origin.x + ","
                + origin.y + ","
                + rotation + ","
                + size;
    }


    @Override
    public String getPropertyTableHeader() {
        return "Properties for NOT Gate";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this, c);
        propList.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        propList.add(new Property("Size", size.toString(), Property.possibleSizes));
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
        else if (propertyName.equalsIgnoreCase("size")) {
            size = Size.fromString(newVal);
            reconstruct();
        }
    }

    @Override
    public String getPropertyValue(String propertyName) {
        if (propertyName.equalsIgnoreCase("facing"))
            return Direction.cardinalFromRotation(rotation);
        if (propertyName.equalsIgnoreCase("size"))
            return size.toString();
        return null;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyName.equalsIgnoreCase("facing")
                || propertyName.equalsIgnoreCase("size");
    }
}
