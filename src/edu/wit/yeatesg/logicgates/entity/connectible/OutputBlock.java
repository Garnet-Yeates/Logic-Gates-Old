package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;

import static edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent.*;

public class OutputBlock extends ConnectibleEntity implements Rotatable {

    private CircuitPoint origin;
    private InputNode in;

    public OutputBlock(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    @Override
    public void construct() {
        connections = new ConnectionList(this);
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        getCircuit().pushIntoMapRange(drawPoints);
        interceptPoints = getBoundingBox().getInterceptPoints();
        update();
        establishInputNode(drawPoints.get(0));
        in = (InputNode) getNodeAt(drawPoints.get(0));
        postInit();
    }

    @Override
    public OutputBlock clone(Circuit c) {
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
    public void determinePowerStateOf(OutputNode outputNode) { }

    @Override
    public boolean canPullPointGoHere(CircuitPoint gridSnap) {
        return in.getLocation().equals(gridSnap);
    }


    // Rotatable Interface Methods

    private int rotation;

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet drawPointRelative = new RelativePointSet();
        Circuit c = getCircuit();
        drawPointRelative.add(0, 0, c); // Origin (bottom middle) is 0
        drawPointRelative.add(-1, 0, c); // top left is 1
        drawPointRelative.add(-1, 2, c); // bot left is 2
        drawPointRelative.add(1, 2, c); // bot right is 3
        drawPointRelative.add(1, 0, c); // top right is 4
        drawPointRelative.add(0, 1, c); // center point is 5
        return drawPointRelative;
    }

    // Other stuff

    public Color getColor() {
        return in.getPowerStatus().getColor();
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(drawPoints.get(2), drawPoints.get(4), this);
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        PanelDrawPoint drawPoint;
        PointSet pts = drawPoints;
        Circuit c = getCircuit();
        g.setLineWidth(getLineWidth());

        // Draw Border
        Color strokeCol = col == null ? Color.BLACK : col;
        strokeCol = Color.rgb((int) (255*strokeCol.getRed()), (int) (255*strokeCol.getGreen()), (int) (255*strokeCol.getBlue()), opacity);
        g.setStroke(strokeCol);

        PanelDrawPoint tL = pts.get(1).toPanelDrawPoint();
        PanelDrawPoint bL = pts.get(2).toPanelDrawPoint();
        PanelDrawPoint bR = pts.get(3).toPanelDrawPoint();
        PanelDrawPoint tR = pts.get(4).toPanelDrawPoint();
        double dist = new Vector(tL, tR).getLength();
        BoundingBox forOval = new BoundingBox(tL, bR, null);
        g.strokeOval(forOval.p1.toPanelDrawPoint().x, forOval.p1.toPanelDrawPoint().y, dist, dist);

        // Draw Connection Thingy

        ConnectionNode connectNode = getNodeAt(pts.get(0));
        connectNode.draw(g, col, opacity);
        // Draw Circle Inside
        CircuitPoint centerPoint = pts.get(5);
        g.setFill(col == null ? in.getPowerStatus().getColor() : col);
        double circleSize = (c.getScale() * 1.3);
        drawPoint = centerPoint.toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);

        PowerStatus inStatus = in.getPowerStatus();
        String text = inStatus == PowerStatus.OFF ? "0" : (inStatus == PowerStatus.ON ? "1" : "");
        double widthOfThisHereInputBlock = c.getScale()*2; // TODO change for diff size
        double maxWidth = widthOfThisHereInputBlock*0.75;
        if (col == null && inStatus == PowerStatus.OFF)
            strokeCol = Color.rgb(40, 40, 40);
        InputBlock.drawText(text, getLineWidth()*0.5, c, g, strokeCol, centerPoint.getSimilar(), maxWidth);
    }


    public double getLineWidth() {
        return getCircuit().getLineWidth();
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
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
        connectingWire.connections.add(new ConnectionNode(atLocation, e, this));
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


    // TODO implement
    @Override
    public PropertyList getPropertyList() {
        PropertyList list = new PropertyList();
        list.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        list.add(new Property("Label", "", ""));
        return list;
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