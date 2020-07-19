package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BezierCurve {

    public CircuitPoint[] points;

    private CurvePolygon shape;

    public static final double IT = 2;

    public BezierCurve(CurvePolygon shape) {
        this.shape = shape;
        Circuit c = shape.points[0].getCircuit();
        CurvePolygon.Line longestLine = null;
        for (CurvePolygon.Line l : shape.lines)
            if (longestLine == null || l.getLength() > longestLine.getLength())
                longestLine = l;
        assert longestLine != null;
        double panelDistance = longestLine.getLength()*c.getScale();

        int numIterations = (int) Math.max(panelDistance / IT, 14);
        points = new CircuitPoint[numIterations];
        double weightInc = 1.0 / (numIterations - 1);
        double currWeight = 0;
        for (int i = 0; i < numIterations; i++) {
            points[i] = getBezierPoint(currWeight);
            currWeight += weightInc;
        }
    }

    public BezierCurve(Circuit c, double... circuitPoints) {
        this(new CurvePolygon(c, circuitPoints));
    }

    public BezierCurve(CircuitPoint... points) {
        this(new CurvePolygon(points));
    }

    public void draw(GraphicsContext g, Color col, double lineWidth) {
        g.setStroke(col);
        g.setLineWidth(lineWidth);

        g.beginPath();
        for (CircuitPoint p : new CurvePolygon(points).points) {
            PanelDrawPoint pp = p.toPanelDrawPoint();
            double x = pp.x, y = pp.y;
            g.lineTo(x, y);
            g.moveTo(x, y);
        }
        g.closePath();
        g.stroke();
       /* for (CurvePolygon.Line l : new CurvePolygon(points).lines) {
            PanelDrawPoint p1 = l.startPoint.toPanelDrawPoint();
            PanelDrawPoint p2 = l.endPoint.toPanelDrawPoint();
            g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }*/
    }

    public static CircuitPoint getBezierPoint(CurvePolygon shape, double weight) {
        if (shape.lines.length == 1)
            return shape.lines[0].getPointAtWeight(weight);
        else {
            CircuitPoint[] pointsAtWeight = new CircuitPoint[shape.lines.length];
            for (int i = 0; i < shape.lines.length; i++)
                pointsAtWeight[i] = shape.lines[i].getPointAtWeight(weight);
            return getBezierPoint(new CurvePolygon(pointsAtWeight), weight);
        }
    }

    public CircuitPoint getBezierPoint(double weight) {
        return getBezierPoint(shape, weight);
    }

    public static double getWeightOfPointAlong(CircuitPoint p1, CircuitPoint p2, CircuitPoint w) {
        boolean isHorizontal = p1.y == p2.y;
        CircuitPoint lefter, righter;
        lefter = isHorizontal ? (p1.x < p2.x ? p1 : p2) : (p1.y < p2.y ? p1 : p2);
        righter = lefter == p1 ? p2 : p1;
        double dist = isHorizontal ? righter.x - lefter.x : righter.y - lefter.y;
        double startToPoint = isHorizontal ? w.x - lefter.x : w.y - lefter.y;
        return startToPoint / dist;
    }
}
