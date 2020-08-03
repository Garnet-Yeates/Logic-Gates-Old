package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.datatypes.BezierCurve;
import edu.wit.yeatesg.logicgates.datatypes.BoundingBox;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPointList;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyList;
import edu.wit.yeatesg.logicgates.circuit.entity.Rotatable;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import static edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Powerable.*;

import java.util.LinkedList;

public class SimpleGateAND extends ConnectibleEntity implements Rotatable {

    private CircuitPoint origin;
    private OutputNode out;

    private int rotation;

    public SimpleGateAND(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    @Override
    public void construct() {
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        getCircuit().pushIntoMapRange(drawPoints);
        connections = new ConnectionList(this);
        interceptPoints = getBoundingBox().getInterceptPoints();
        establishOutputNode(drawPoints.get(0));
        out = (OutputNode) getNodeAt(drawPoints.get(0));
        establishInputNode(drawPoints.get(7));
        establishInputNode(drawPoints.get(8));
        assignOutputsToInputs();
        update();
    }

    @Override
    public SimpleGateAND getCloned(Circuit onto) {
        return new SimpleGateAND(origin.clone(onto), rotation);
    }



    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof SimpleGateAND && ((SimpleGateAND) other).origin.equals(origin);
    }

    @Override
    public String toParsableString() {
        return "[SimpleGateAND]" + origin.x + "," + origin.y + "," + rotation;
    }

    @Override
    public void determinePowerStateOf(OutputNode outputNode) {
        if (outputNode.getPowerStatus() == PowerStatus.UNDETERMINED) {
            LinkedList<InputNode> relevants = getRelevantInputNodesFor(outputNode);
            if (relevants.size() == 0)
                outputNode.setPowerStatus(PowerStatus.PARTIALLY_DEPENDENT);
            if (outputNode.getPowerStatus() == PowerStatus.UNDETERMINED) {
                outputNode.setPowerStatus(PowerStatus.ON);
                for (InputNode n : getRelevantInputNodesFor(outputNode)) {
                    if (n.getPowerStatus() == PowerStatus.OFF) {
                        outputNode.setPowerStatus(PowerStatus.OFF);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean canPullPointGoHere(CircuitPoint gridSnap) {
        return hasNodeAt(gridSnap);
    }

    @Override
    protected void assignOutputsToInputs() {
        out.dependingOn().add((InputNode) getNodeAt(drawPoints.get(7)));
        out.dependingOn().add((InputNode) getNodeAt(drawPoints.get(8)));
    }

    @Override
    public double getLineWidth() {
        return getCircuit().getLineWidth();
    }

    @Override
    public CircuitPointList getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet relatives = new RelativePointSet();
        Circuit c = getCircuit();
        // Origin (middle bot of curve (u shaped curve))
        relatives.add(0, 0, c);

        // Top left of curve
        relatives.add(-2.49, -2.49, c);
        // Bottom left of curve
        relatives.add(-2.49, 0.8, c);
        // Bottom right of curve
        relatives.add(2.49, 0.8, c);
        // Top right of curve
        relatives.add(2.49, -2.49, c);

        // Top right
        relatives.add(2.49, -5, c);
        // Top left
        relatives.add(-2.49, -5, c);

        relatives.add(-1, -5, c); // Input1
        relatives.add(1, -5, c); // Input2

        // bottom left boundingbox corner
        relatives.add(new CircuitPoint(relatives.get(6).x, relatives.get(0).y, c)); // X OF TOP LEFT, Y OF ORIGIN
        return relatives;
    }


    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(drawPoints.get(5), drawPoints.get(9), this);
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        Color strokeCol = col == null ? Color.BLACK : col;
        strokeCol = Color.rgb((int) (255*strokeCol.getRed()), (int) (255*strokeCol.getGreen()), (int) (255*strokeCol.getBlue()), opacity);
        g.setStroke(strokeCol);
        g.setLineWidth(getLineWidth());
        CircuitPointList ps = drawPoints;

        PanelDrawPoint p5 = ps.get(5).toPanelDrawPoint();
        PanelDrawPoint p6 = ps.get(6).toPanelDrawPoint();
        PanelDrawPoint p1 = ps.get(1).toPanelDrawPoint();
        PanelDrawPoint p4 = ps.get(4).toPanelDrawPoint();

        // Curve 7, 1, 2, 0, 3, 4, 8
        BezierCurve curve = new BezierCurve(ps.get(1), ps.get(2), ps.get(3), ps.get(4));
        curve.draw(g, col, getLineWidth());

        // Line 8 to 5
        g.strokeLine(p4.x, p4.y, p5.x, p5.y);
        // Line 5 to 6
        g.strokeLine(p5.x, p5.y, p6.x, p6.y);
        // Line 6 to 1
        g.strokeLine(p6.x, p6.y, p1.x, p1.y);

        for (ConnectionNode node : connections)
            node.draw(g, col, opacity);
    }


    @Override
    public String getDisplayName() {
        return "Simple Gate AND";
    }

    @Override
    public void move(Vector v) {
        origin = origin.getIfModifiedBy(v);
        reconstruct();
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
            throw new RuntimeException("Can't connect to GateAND here, no ConnectionNode at this CircuitPoint");
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
    public String toString() {
        return "SimpleGateAND{" + origin + " , "  + id + "}";
    }
}