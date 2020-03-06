package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class InputBlock extends ConnectibleEntity implements Pokable, Rotatable {

    private CircuitPoint origin;

    public InputBlock(CircuitPoint origin, int rotation, boolean preview) {
        super(origin.getCircuit(), preview);
        this.origin = origin;
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        establishOutputNode(drawPoints.get(0));
        updateInvalidInterceptPoints();
        if (!preview) {
            c.addEntity(this);
            connectCheck();
            c.getEditorPanel().repaint();
        }
        c.refreshTransmissions();
    }

    public InputBlock(CircuitPoint origin, int rotation) {
        this(origin, rotation, false);
    }

    // Rotatable Interface Methods

    private int rotation;

    @Override
    public Entity getRotated(int rotation) {
        if (!validRotation(rotation))
            throw new RuntimeException("Invalid Rotation");
        return null;
    }

    @Override
    public int getRotation() {
        return rotation;
    }

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet drawPointRelative = new RelativePointSet();
        drawPointRelative.add(0, 0, c); // Origin (bottom middle) is 0
        drawPointRelative.add(-1, 0, c); // bottom left is 1
        drawPointRelative.add(-1, -2, c); // top left is 2
        drawPointRelative.add(1, -2, c); // top right is 3
        drawPointRelative.add(1, 0, c); // bottom right is 4
        drawPointRelative.add(0, -1, c); // center point is 5
        return drawPointRelative;
    }

    // Other stuff

    public Color getColor() {
        return state.getColor();
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(drawPoints.get(2), drawPoints.get(4), this);
    }

    @Override
    public void draw(GraphicsContext g) {
        PanelDrawPoint drawPoint;
        PointSet pts = drawPoints;
        g.setLineWidth(getLineWidth());

        // Draw Border
        g.setStroke(Color.BLACK);
        PanelDrawPoint bL = pts.get(1).toPanelDrawPoint();
        PanelDrawPoint tL = pts.get(2).toPanelDrawPoint();
        PanelDrawPoint tR = pts.get(3).toPanelDrawPoint();
        PanelDrawPoint bR = pts.get(4).toPanelDrawPoint();
        g.strokeLine(bL.x, bL.y, tL.x, tL.y);
        g.strokeLine(tL.x, tL.y, tR.x, tR.y);
        g.strokeLine(tR.x, tR.y, bR.x, bR.y);
        g.strokeLine(bR.x, bR.y, bL.x, bL.y);

        // Draw Connection Thingy

        ConnectionNode connectNode = getNodeAt(pts.get(0));
        connectNode.draw(g);
        // Draw Circle Inside
        CircuitPoint centerPoint = pts.get(5);
        g.setFill(getColor());
        int circleSize = (int) (c.getScale() * 1.35);
        if (circleSize % 2 != 0) circleSize++;
        drawPoint = centerPoint.toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }

    @Override
    public int getLineWidth() {
        return c.getLineWidth();

     //   return (int) (c.getLineWidth() * 0.8);
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


    private boolean powerStatus;

    @Override
    public void onPoke() {
        powerStatus = !powerStatus;
    }

    @Override
    public void determinePowerState() {
        super.determinePowerState();
        if (state != State.ILLOGICAL)
            state = powerStatus ? State.ON : State.OFF;
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!canConnectTo(e, atLocation))
            throw new RuntimeException("Cannot connect these 2 entities");
        if (!hasNodeAt(atLocation))
            throw new RuntimeException("Can't connect to InputBlock here, no ConnectionNode at this CircuitPoint");
        getNodeAt(atLocation).connectedTo = e;
        e.connections.add(new ConnectionNode(atLocation, e, this));
    }

    @Override
    public boolean canPullConnectionFrom(CircuitPoint locationOnThisEntity) {
        return hasNodeAt(locationOnThisEntity) && !getNodeAt(locationOnThisEntity).hasConnectedEntity();
    }

    @Override
    public void disconnect(ConnectibleEntity e) {
        getConnectionTo(e).setConnectedTo(null);
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        return e instanceof Wire
                && hasNodeAt(at)
                && (getNumEntitiesConnectedAt(at) == 0)
                && !e.isDeleted();
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {
        if (isPreview || e.isPreview() || deleted || e.isDeleted() || isInvalid() || e.isInvalid())
            return;
        CircuitPoint nodeLoc = drawPoints.get(0);
        if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc) && !deleted && !e.isDeleted())
                connect(e, nodeLoc);
    }

    @Override
    public String getDisplayName() {
        return "Input Block";
    }

    @Override
    public void onDelete() { }

    // TODO might have to fix intercepts for inputblock, might have to add a method 'fuzzyIntercepts' which checks
    // if the bounding box intercepts


   @Override
    public boolean equals(Object other) {
        return (other instanceof InputBlock && ((InputBlock) other).origin.equals(origin));
    }

    @Override
    public String getPropertyTableHeader() {
        return "Properties For: " + getDisplayName();
    }

    private static final String[] properties = new String[] { "Facing", "Label" };

    @Override
    public boolean hasProperty(String propertyName) {
        return Arrays.asList(properties).contains(propertyName);
    }

    // TODO implement
    @Override
    public PropertyList getPropertyList() {
        PropertyList list = new PropertyList();
        list.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        list.add(new Property("Label", "", ""));
        return list;
    }

    // TODO implement
    @Override
    public void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1) {

    }

    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo, PermitList permit, boolean strictWithWires) {
        permit = new PermitList(permit);
        if (theo.invalidlyIntercepts(this))
            return true;
        else
            for (CircuitPoint p : theo.getInterceptPoints(this))
                if (!permit.contains(new InterceptPermit(this, p)))
                    return true;
        return false;
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public Entity onDragMove(CircuitPoint newLocation) {
        super.onDragMove(newLocation);
        if (!newLocation.equals(origin)) {
            InputBlock preview = new InputBlock(newLocation, rotation, true);
            return preview;
        }
        return null;
    }

    @Override
    public void onDragMoveRelease(CircuitPoint newLocation) {
        super.onDragMoveRelease(newLocation);
    }

    @Override
    public String toString() {
        return "InputBlock{" +
                "origin=" + origin +
                '}';
    }
}