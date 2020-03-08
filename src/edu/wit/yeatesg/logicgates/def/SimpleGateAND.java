package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.entity.PropertyList;
import edu.wit.yeatesg.logicgates.entity.Rotatable;
import edu.wit.yeatesg.logicgates.entity.connectible.*;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import static edu.wit.yeatesg.logicgates.entity.connectible.Dependent.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class SimpleGateAND extends ConnectibleEntity implements Rotatable {

    private CircuitPoint origin;
    private OutputNode out;

    public SimpleGateAND(CircuitPoint origin, int rotation, boolean isPreview) {
        super(origin.getCircuit(), isPreview);
        this.origin = origin;
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        establishOutputNode(drawPoints.get(0));
        out = (OutputNode) getNodeAt(drawPoints.get(0));
        establishInputNode(drawPoints.get(7));
        establishInputNode(drawPoints.get(8));
        //    establishConnectionNode(drawPoints.get(0));
        postInit();
    }

    public SimpleGateAND(CircuitPoint origin, int rotation) {
        this(origin, rotation, false);
    }

    @Override
    public void determinePowerStateOf(OutputNode outputNode) {
        if (outputNode.getState() != State.ILLOGICAL) {
            LinkedList<InputNode> relevants = getRelevantInputNodesFor(outputNode);
            if (relevants.size() == 0 && outputNode.hasConnectedEntity())
                outputNode.setState(State.PARTIALLY_DEPENDENT);
            else if (relevants.size() == 0)
                outputNode.setState(State.NO_DEPENDENT);
            if (outputNode.getState() == State.OFF) {
                outputNode.setState(State.ON);
                for (InputNode n : getRelevantInputNodesFor(outputNode)) {
                    if (n.getState() == State.OFF) {
                        outputNode.setState(State.OFF);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return hasNodeAt(gridSnap);
    }

    @Override
    protected void determineOutputDependencies() {
        out.getDependencyList().add((InputNode) getNodeAt(drawPoints.get(7)));
        out.getDependencyList().add((InputNode) getNodeAt(drawPoints.get(8)));
    }

    @Override
    public int getLineWidth() {
        return c.getLineWidth();
      //  return (int) (c.getLineWidth() * 0.8);
    }

    @Override
    public PointSet getInterceptPoints() {
        return getBoundingBox().getGridPointsWithin();
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }


    @Override
    public Entity getRotated(int rotation) {
        return null;
    }

    @Override
    public int getRotation() {
        return 0;
    }


    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet relatives = new RelativePointSet();
        // Origin (middle bot of curve (u shaped curve))
        relatives.add(0, 0, c);

        // Top left of curve
        relatives.add(-2.5, -2.5, c);
        // Bottom left of curve
        relatives.add(-2.5, 0.8, c);
        // Bottom right of curve
        relatives.add(2.5, 0.8, c);
        // Top right of curve
        relatives.add(2.5, -2.5, c);

        // Top right
        relatives.add(2.5, -5, c);
        // Top left
        relatives.add(-2.5, -5, c);

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
    public void draw(GraphicsContext g) {
        g.setStroke(Color.BLACK);
        g.setLineWidth(getLineWidth());
        PointSet ps = drawPoints;

        PanelDrawPoint p5 = ps.get(5).toPanelDrawPoint();
        PanelDrawPoint p6 = ps.get(6).toPanelDrawPoint();
        PanelDrawPoint p1 = ps.get(1).toPanelDrawPoint();
        PanelDrawPoint p4 = ps.get(4).toPanelDrawPoint();

        // Curve 7, 1, 2, 0, 3, 4, 8
        BezierCurve curve = new BezierCurve(ps.get(1), ps.get(2), ps.get(3), ps.get(4));
        curve.draw(g, getLineWidth());

        // Line 8 to 5
        g.strokeLine(p4.x, p4.y, p5.x, p5.y);
        // Line 5 to 6
        g.strokeLine(p5.x, p5.y, p6.x, p6.y);
        // Line 6 to 1
        g.strokeLine(p6.x, p6.y, p1.x, p1.y);

        for (ConnectionNode node : connections)
            node.draw(g);
    }

    @Override
    public String getDisplayName() {
        return "Simple Gate AND";
    }

    @Override
    public void onDelete() { }

    @Override
    public boolean equals(Object other) {
        return other instanceof SimpleGateAND && ((SimpleGateAND) other).origin.equals(origin);
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
    public boolean canMove() {
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
    public void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1) { }

    @Override
    public boolean hasProperty(String propertyName) {
        return false;
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
    public boolean canPullConnectionFrom(CircuitPoint locationOnThisEntity) {
        return connections.hasNodeAt(locationOnThisEntity) && getNodeAt(locationOnThisEntity).getConnectedTo() == null;
    }

    @Override
    public void disconnect(ConnectibleEntity e) {
        getConnectionTo(e).setConnectedTo(null);
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        LogicGates.debug("CanConnectTo in AND class", "", "hasNodeAt?", hasNodeAt(at), "connectedAt", getEntitiesConnectedAt(at),
            "e is deleted?", e.isDeleted());
        return e instanceof Wire
                && hasNodeAt(at)
                && (getNumEntitiesConnectedAt(at) == 0)
                && !e.isDeleted();
    }

    @Override
    protected void connectCheck(ConnectibleEntity e) {
        if (isPreview || e.isPreview() || deleted || e.isDeleted() || isInvalid() || e.isInvalid())
            return;
        for (CircuitPoint nodeLoc : connections.getEmptyConnectionLocations()) {
            if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc) && !deleted && !e.isDeleted()) {
                connect(e, nodeLoc);
            }
        }

    }

    @Override
    public String toString() {
        return "SimpleGateAND{" + origin + "}";
    }
}