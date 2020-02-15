package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;

public class GateAND extends Entity {

    public GateAND(CircuitPoint location) {
        super(location);
        c.addEntity(this);
        c.getEditorPanel().repaint();
    }

    @Override
    public PointSet getRelativePointSet() {
        CircuitPoint[] points = new CircuitPoint[7];
        // Origin (middle bot of curve (u shaped curve))
        points[0] = new CircuitPoint(0, 0, c);

        // Top left of curve
        points[1] = new CircuitPoint(-2.5, -2.5, c);
        // Bottom left of curve
        points[2] = new CircuitPoint(-2.5, 0.8, c);
        // Bottom right of curve
        points[3] = new CircuitPoint(2.5, 0.8, c);
        // Top right of curve
        points[4] = new CircuitPoint(2.5, -2.5, c);

        // Top right
        points[5] = new CircuitPoint(2.5, -5, c);
        // Top left
        points[6] = new CircuitPoint(-2.5, -5, c);

        return new PointSet(points);
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
        PointSet ps = pointSetForDrawing;

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
    public boolean intercepts(CircuitPoint p) {
        return false;
    }

    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire w, CircuitPoint... exceptions) {
        return false;
    }
}
