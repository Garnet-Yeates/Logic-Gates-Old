package edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.LogicGates;
import edu.wit.yeatesg.logicgates.circuit.entity.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


public class OutputBlock extends ConnectibleEntity implements Rotatable {

    private CircuitPoint origin;
    private InputNode in;

    public OutputBlock(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    public OutputBlock(CircuitPoint origin) {
        this(origin, 0);
    }

    @Override
    public void construct() {
        connections = new ConnectionList(this);
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        boundingBox = new BoundingBox(drawPoints.get(2), drawPoints.get(4), this);
        interceptPoints = getBoundingBox().getInterceptPoints();
        establishInputNode(drawPoints.get(0));
        in = (InputNode) getNodeAt(drawPoints.get(0));
        assignOutputsToInputs();
    }

    @Override
    public OutputBlock getCloned(Circuit c) {
        return new OutputBlock(origin.clone(c), rotation);
    }

    @Override
    public boolean isSimilar(Entity other) {
        return (other instanceof OutputBlock && ((OutputBlock) other).origin.equals(origin));
    }

    @Override
    public String toParsableString() {
        return "[OutputBlock]" + origin.toParsableString() + "," + rotation;
    }

    @Override
    protected void assignOutputsToInputs() { /* Output Node Dependency List Will Be Empty For Input Nodes */}

    @Override
    public PowerValue getLocalPowerStateOf(OutputNode root) {
        return null; // Doesn't have out nodes
    }

    @Override
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return in.getLocation().equals(gridSnap);
    }


    // Rotatable Interface Methods

    private int rotation;

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet drawPointRelative = new RelativePointSet();
        Circuit c = getCircuit();
        drawPointRelative.add(0, 0, c); // Origin (bottom middle) is 0
        drawPointRelative.add(-1, -2, c); // top left is 1
        drawPointRelative.add(-1, 0, c); // bot left is 2
        drawPointRelative.add(1, 0, c); // bot right is 3
        drawPointRelative.add(1, -2, c); // top right is 4
        drawPointRelative.add(0, -1, c); // center point is 5
        return drawPointRelative;
    }

    // Other stuff

    public Color getColor() {
        return in.getPowerValue().getColor();
    }

    private BoundingBox boundingBox;

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox.clone();
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        PanelDrawPoint drawPoint;
        CircuitPointList pts = drawPoints;
        Circuit c = getCircuit();
        g.setLineWidth(getLineWidth());

        // Draw Border
        Color strokeCol = col == null ? Color.BLACK : col;
        strokeCol = Color.rgb((int) (255*strokeCol.getRed()), (int) (255*strokeCol.getGreen()), (int) (255*strokeCol.getBlue()), opacity);
        g.setStroke(strokeCol);

        CircuitPoint centerPoint = pts.get(5);


        double circleSize = (c.getScale() * 2);
        drawPoint = centerPoint.toPanelDrawPoint();
        g.strokeOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);

        // Draw Connection Thingy

        ConnectionNode connectNode = getNodeAt(pts.get(0));
        connectNode.draw(g, col, opacity);
        // Draw Circle Inside
        g.setFill(col == null ? in.getPowerValue().getColor() : col);
        circleSize = (c.getScale() * 1.5);
        double radius = circleSize / 2.0;
        drawPoint = centerPoint.toPanelDrawPoint();
        g.fillOval(drawPoint.x - radius, drawPoint.y - radius, circleSize, circleSize);

        PowerValue inStatus = in.getPowerValue();
        String text = inStatus.getAbbreviated();

        radius /= c.getScale();
        BoundingBox bb = new BoundingBox(new CircuitPoint(centerPoint.x - radius, centerPoint.y - radius, c),
                new CircuitPoint(centerPoint.x + radius, centerPoint.y + radius, c), null);
        bb = bb.getShrunkBy(0.1);
        if (text.equals("?"))
            bb = bb.getShrunkBy(0.15);
        if (text.equals("E") || text.equals("O"))
            bb = bb.getShrunkBy(0.1);

        if (col == null)
         LogicGates.drawText(text, bb, c, g, Color.BLACK);
    }


    @Override
    public CircuitPointList getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        if (e instanceof OutputBlock) {
            OutputBlock other = (OutputBlock) e;
            CircuitPointList view = new CircuitPointList(drawPoints.get(0), drawPoints.get(5));
            CircuitPointList otherView = new CircuitPointList(other.drawPoints.get(0), other.drawPoints.get(5));
            return view.intersection(other.interceptPoints).union(otherView.intersection(interceptPoints));
        }
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }



    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!(e instanceof Wire))
            throw new RuntimeException("OutputBlocks can only connect to Wires");
        if (!canConnectTo(e, atLocation))
            throw new RuntimeException("Cannot connect these 2 entities");
        if (!hasNodeAt(atLocation))
            throw new RuntimeException("Can't connect to OutputBlock here, no ConnectionNode at this CircuitPoint");
        Wire connectingWire = (Wire) e;
        getNodeAt(atLocation).setConnectedTo(connectingWire);
        connectingWire.getConnections().add(new ConnectionNode(atLocation, e, this));
    }

    @Override
    public boolean canCreateWireFrom(CircuitPoint locationOnThisEntity) {
        return hasNodeAt(locationOnThisEntity) && !getNodeAt(locationOnThisEntity).hasConnectedEntity();
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
    public void connectCheck(ConnectibleEntity e) {
        CircuitPoint nodeLoc = drawPoints.get(0);
        if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc))
            connect(e, nodeLoc);
    }


    @Override
    public String getDisplayName() {
        return "Input Block";
    }

    @Override
    public void move(Vector v) {
        origin = origin.getIfModifiedBy(v);
        reconstruct();
    }


    @Override
    public String getPropertyTableHeader() {
        return "Properties For: " + getDisplayName();
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList list = new PropertyList(this, c);
        list.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        return list;
    }

    @Override
    public void onPropertyChangeViaTable(String propertyName, String old, String newVal) {
        if (isItemEntity()) {
            onPropertyChange(propertyName, old, newVal);
            treeItem.onClick();
        } else {
            c.new PropertyChangeOperation(this, propertyName, newVal, true).operate();
        }
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


    @Override
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
    public String toString() {
        return "OutputBlock{" +
                "origin=" + origin +
                '}';
    }
}