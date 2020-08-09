package edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.LogicGates;
import edu.wit.yeatesg.logicgates.circuit.entity.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.PowerValue;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


public class InputBlock extends ConnectibleEntity implements Pokable, Rotatable {

    private CircuitPoint origin;
    private OutputNode out;
    private OutputNode out2;

    public InputBlock(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    @Override
    public void construct() {
        connections = new ConnectionList(this);
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        interceptPoints = getBoundingBox().getInterceptPoints();

        establishOutputNode(drawPoints.get(0));
        establishOutputNode(drawPoints.get(6));

        out = (OutputNode) getNodeAt(drawPoints.get(0));
        out2 = (OutputNode) getNodeAt(drawPoints.get(6));

        postInit();
    }

    @Override
    public InputBlock getCloned(Circuit c) {
        return new InputBlock(origin.clone(c), rotation);
    }


    @Override
    public boolean isSimilar(Entity other) {
        return (other instanceof InputBlock && ((InputBlock) other).origin.equals(origin));
    }

    @Override
    public String toParsableString() {
        return "[InputBlock]" + origin.toParsableString() + "," + rotation;
    }

    @Override
    protected void assignOutputsToInputs() { /* Output Node Dependency List Will Be Empty For Input Nodes */}

    @Override
    public PowerValue getLocalPowerStateOf(OutputNode root) {
        return powerStatus ? PowerValue.ON : PowerValue.OFF;
    }

    @Override
    public boolean canPullPointGoHere(CircuitPoint gridSnap) {
        return out.getLocation().equals(gridSnap) || out2.getLocation().equals(gridSnap);
    }


    // Rotatable Interface Methods

    private int rotation;

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet drawPointRelative = new RelativePointSet();
        Circuit c = getCircuit();
        drawPointRelative.add(0, 0, c); // Origin (bottom middle) is 0
        drawPointRelative.add(-1, 0, c); // bottom left is 1
        drawPointRelative.add(-1, -2, c); // top left is 2
        drawPointRelative.add(1, -2, c); // top right is 3
        drawPointRelative.add(1, 0, c); // bottom right is 4
        drawPointRelative.add(0, -1, c); // center point is 5
        drawPointRelative.add(0, -2, c); // center point is 5

        return drawPointRelative;
    }

    // Other stuff

    public Color getColor() {
        return out.getPowerValueFromTree().getColor();
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(drawPoints.get(2), drawPoints.get(4), this);
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        // Draw Border
        Color strokeCol = col == null ? Color.BLACK : col;
        strokeCol = Color.rgb((int) (255*strokeCol.getRed()), (int) (255*strokeCol.getGreen()), (int) (255*strokeCol.getBlue()), opacity);
        g.setStroke(strokeCol);

        PanelDrawPoint drawPoint;
        CircuitPointList pts = drawPoints;
        Circuit c = getCircuit();
        g.setLineWidth(getLineWidth());

        // Draw Circle Inside
        CircuitPoint centerPoint = pts.get(5);
        double circleSize = (c.getScale() * 1.3);
        circleSize *= 1.2;
        drawPoint = centerPoint.toPanelDrawPoint();

        g.setFill(col == null ? out.getPowerValueFromTree().getColor() : col);
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);

        // Draw Text 0 / 1
        PowerValue outStatus = out.getPowerValueFromTree();
        String text = outStatus == PowerValue.OFF ? "0" : (outStatus == PowerValue.ON ? "1" : "");
        double widthOfThisHereInputBlock = c.getScale()*2; // TODO change for diff size
        double maxWidth = widthOfThisHereInputBlock*0.70;
        Color numCol = strokeCol;
        if (col == null && outStatus == PowerValue.OFF)
            numCol = Color.rgb(0, 0, 0);
        LogicGates.drawText(text, getLineWidth()*0.5, c, g, numCol, centerPoint.getSimilar(), maxWidth);

        g.setLineWidth(getLineWidth());
        g.setStroke(strokeCol);

        PanelDrawPoint bL = pts.get(1).toPanelDrawPoint();
        PanelDrawPoint tL = pts.get(2).toPanelDrawPoint();
        PanelDrawPoint tR = pts.get(3).toPanelDrawPoint();
        PanelDrawPoint bR = pts.get(4).toPanelDrawPoint();
        g.strokeLine(bL.x, bL.y, tL.x, tL.y);
        g.strokeLine(tL.x, tL.y, tR.x, tR.y);
        g.strokeLine(tR.x, tR.y, bR.x, bR.y);
        g.strokeLine(bR.x, bR.y, bL.x, bL.y);

        ConnectionNode connectNode = getNodeAt(pts.get(0));
        connectNode.draw(g, col, opacity);
        connectNode = getNodeAt(pts.get(6));
        connectNode.draw(g, col, opacity);


    }


    public double getLineWidth() {
        return getCircuit().getLineWidth();
    }

    @Override
    public CircuitPointList getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }


    private boolean powerStatus;

    public void setPowerStatus(boolean powerStatus) {
        this.powerStatus = powerStatus;
    }

    @Override
    public void onPoke() {
        powerStatus = !powerStatus;
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!(e instanceof Wire))
            throw new RuntimeException("InputBlocks can only connect to Wires");
        if (!canConnectTo(e, atLocation))
            throw new RuntimeException("Cannot connect these 2 entities");
        if (!hasNodeAt(atLocation))
            throw new RuntimeException("Can't connect to InputBlock here, no ConnectionNode at this CircuitPoint");
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
        for (OutputNode out : getOutputNodes()) {
            CircuitPoint nodeLoc = out.getLocation();
            if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc))
                connect(e, nodeLoc);
        }

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
        if (isTemplateEntity()) {
            onPropertyChange(propertyName, old, newVal);
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
        return "InputBlock{" +
                "origin=" + origin +
                '}';
    }
}