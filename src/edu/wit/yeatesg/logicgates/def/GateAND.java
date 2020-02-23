package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.PointSet;
import edu.wit.yeatesg.logicgates.connections.Rotatable;
import edu.wit.yeatesg.logicgates.connections.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;

public class GateAND extends Entity implements Rotatable {

    public GateAND(CircuitPoint origin, int rotation) {
        super(origin.getCircuit());
        c.addEntity(this);
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
    //    establishConnectionNode(drawPoints.get(0));
        c.getEditorPanel().repaint();
    }

    @Override
    public int getStrokeSize() {
        return 0;
    }

    @Override
    public PointSet getInterceptPoints() {
        return null;
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        return null;
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
        relatives.add(-2.5, -0.8, c);
        // Bottom right of curve
        relatives.add(2.5, 0.8, c);
        // Top right of curve
        relatives.add(2.5, -2.5, c);

        // Top right
        relatives.add(2.5, -5, c);
        // Top left
        relatives.add(-2.5, -5, c);
        return relatives;
    }

    @Override
    public boolean canMoveBy(Vector vector) {
        return false;
    }


    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Color.black);
        g.setStroke(c.getStroke());
        PointSet ps = drawPoints;

        PanelDrawPoint p5 = ps.get(5).toPanelDrawPoint();
        PanelDrawPoint p6 = ps.get(6).toPanelDrawPoint();
        PanelDrawPoint p1 = ps.get(1).toPanelDrawPoint();
        PanelDrawPoint p4 = ps.get(4).toPanelDrawPoint();

        // Curve 7, 1, 2, 0, 3, 4, 8
        BezierCurve curve = new BezierCurve(ps.get(1), ps.get(2), ps.get(3), ps.get(4));
        curve.draw(g, c.getStroke());

        // Line 8 to 5
        g.drawLine(p4.x, p4.y, p5.x, p5.y);
        // Line 5 to 6
        g.drawLine(p5.x, p5.y, p6.x, p6.y);
        // Line 6 to 1
        g.drawLine(p6.x, p6.y, p1.x, p1.y);
    }

    @Override
    public void onDelete() {

    }

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo, PermitList exceptions, boolean strictWithWires) {
        return false;
    }

    @Override
    public boolean intercepts(CircuitPoint p) {
        return false;
    }


}