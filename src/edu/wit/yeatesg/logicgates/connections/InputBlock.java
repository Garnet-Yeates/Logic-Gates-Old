package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.LogicGates;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;

public class InputBlock extends ConnectibleEntity implements Pokable {

    private boolean initDone;

    public InputBlock(CircuitPoint location) {
        super(location);
        initDone = true;
        c.addEntity(this);
        reconstruct();
        c.refreshTransmissions();
    }

    @Override
    public int getStrokeSize() {
        return c.getStrokeSize();
    }

    public void reconstruct() {
        if (initDone) {
            disconnectAll();
            establishConnectionNode(pointSetForDrawing.get(0));
            System.out.println("ESTABLISHED");
        }
    }

    @Override
    public void onPowerReceive() {
        if (!receivedPowerThisUpdate) {
            super.onPowerReceive();
            ConnectionNode connectNode = getNodeAt(pointSetForDrawing.get(0));
            if (connectNode.hasConnectedEntity() && !connectNode.getConnectedTo().receivedPowerThisUpdate)
                connectNode.getConnectedTo().onPowerReceive();
        }
    }

    @Override
    public boolean canRotate() {
        return true;
    }

    @Override
    public void onRotate() {
        super.onRotate();
        reconstruct();
    }

    @Override
    public PointSet getRelativePointSet() {
        PointSet set = new PointSet();
        set.add(new CircuitPoint(0, 0, c)); // Origin (bottom middle) is 0
        set.add(new CircuitPoint(-1, 0, c)); // bottom left is 1
        set.add(new CircuitPoint(-1, -2, c)); // top left is 2
        set.add(new CircuitPoint(1, -2, c)); // top right is 3
        set.add(new CircuitPoint(1, 0, c)); // bottom right is 4
        set.add(new CircuitPoint(0, -1, c)); // center point is 5
        return set;
    }

    @Override
    public void draw(Graphics2D g) {
        PanelDrawPoint drawPoint;
        PointSet pts = getPointSetForDrawing();
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
        LogicGates.debug("HasNodeAt?", hasNodeAt(at));
        return e instanceof Wire && hasNodeAt(at) && getNumEntitiesConnectedAt(at) == 0;
    }

    @Override
    public void connectCheck(ConnectibleEntity e) {

    }

    @Override
    public boolean canMoveBy(Vector vector) {
        return false;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return new BoundingBox(pointSetForDrawing.get(2), pointSetForDrawing.get(4), this);
    }

    @Override
    public void onDelete() {

    }

    @Override
    public boolean intercepts(CircuitPoint p) {
        return getBoundingBox().intercepts(p);
    }

    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire w, CircuitPoint... exceptions) {
        return false;
    }

   /* @Override
    public boolean equals(Object other) {
        return (other instanceof InputBlock && ((InputBlock) other).location.equals(location));
    }*/


}
