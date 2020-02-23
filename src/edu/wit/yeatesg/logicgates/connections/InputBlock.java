package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Entity;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;

public class InputBlock extends ConnectibleEntity implements Pokable, Rotatable {

    private CircuitPoint origin;

    public InputBlock(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        c.addEntity(this);
        this.origin = origin;
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        establishConnectionNode(drawPoints.get(0));
        c.getEditorPanel().repaint();
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

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(drawPoints.get(2), drawPoints.get(4), this);
    }

    @Override
    public void draw(Graphics2D g) {
        PanelDrawPoint drawPoint;
        PointSet pts = drawPoints;
        g.setStroke(getStroke());

        // Draw Border
        g.setColor(Color.black);
        PanelDrawPoint bL = pts.get(1).toPanelDrawPoint();
        PanelDrawPoint tL = pts.get(2).toPanelDrawPoint();
        PanelDrawPoint tR = pts.get(3).toPanelDrawPoint();
        PanelDrawPoint bR = pts.get(4).toPanelDrawPoint();
        g.drawLine(bL.x, bL.y, tL.x, tL.y);
        g.drawLine(tL.x, tL.y, tR.x, tR.y);
        g.drawLine(tR.x, tR.y, bR.x, bR.y);
        g.drawLine(bR.x, bR.y, bL.x, bL.y);

        // Draw Connection Thingy

        ConnectionNode connectNode = getNodeAt(pts.get(0));
        g.setColor(getColor());
        int circleSize = (int) (c.getScale() * 0.3);
        if (circleSize % 2 != 0) circleSize++;
        drawPoint = connectNode.getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2, drawPoint.y - circleSize/2, circleSize, circleSize);

        // Draw Circle Inside
        CircuitPoint centerPoint = pts.get(5);
        g.setColor(getColor());
        circleSize = (int) (c.getScale() * 1.35);
        if (circleSize % 2 != 0) circleSize++;
        drawPoint = centerPoint.toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2, drawPoint.y - circleSize/2, circleSize, circleSize);
    }

    @Override
    public int getStrokeSize() {
        return c.getStrokeSize();
    }

    @Override
    public PointSet getInterceptPoints() {
        return getBoundingBox().getGridPointsWithin();
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this); // Wire covers this case for us (obv casting not necessary but i did)
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }

    @Override
    public void onPowerReceive() {
        if (!receivedPowerThisUpdate) {
            super.onPowerReceive();
            ConnectionNode connectNode = getNodeAt(drawPoints.get(0));
            if (connectNode.hasConnectedEntity() && !connectNode.getConnectedTo().receivedPowerThisUpdate)
                connectNode.getConnectedTo().onPowerReceive();
        }
    }

    @Override
    public void onPoke() {
        powered = !powered;
    }

    @Override
    public boolean isPowerSource() {
        return powered;
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!hasNodeAt(atLocation))
            throw new RuntimeException("Can't connect to InputBlock here, no ConnectionNode at this CircuitPoint");
        getNodeAt(atLocation).connectedTo = e;
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
        return e instanceof Wire && hasNodeAt(at) && getNumEntitiesConnectedAt(at) == 0 && !e.isDeleted();
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {
        CircuitPoint nodeLoc = drawPoints.get(0);
        if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc) && !deleted && !e.isDeleted())
                connect(e, nodeLoc);
    }

    @Override
    public boolean canMoveBy(Vector vector) {
        return false;
    }


    @Override
    public void onDelete() {

    }

    @Override
    public boolean intercepts(CircuitPoint p) {
        return getBoundingBox().intercepts(p);
    }


   @Override
    public boolean equals(Object other) {
        return (other instanceof InputBlock && ((InputBlock) other).origin.equals(origin));
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
}