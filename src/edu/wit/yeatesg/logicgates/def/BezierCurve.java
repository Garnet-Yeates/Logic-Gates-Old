package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BezierCurve {

    public CircuitPoint[] points;

    public BezierCurve(CurvePolygon shape) {
        Circuit c = shape.points[0].getCircuit();
        int numIterations = (int) (c.getScale()*(c.getScale() < 10 ? 2.8 : 2.3));
        points = new CircuitPoint[numIterations];
        double weightInc = 1.0 / (numIterations - 1);
        double currWeight = 0;
        for (int i = 0; i < numIterations; i++) {
            points[i] = getBezierPoint(shape, currWeight);
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
        for (CurvePolygon.Line l : new CurvePolygon(points).lines) {
            PanelDrawPoint p1 = l.startPoint.toPanelDrawPoint();
            PanelDrawPoint p2 = l.endPoint.toPanelDrawPoint();
            g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private static CircuitPoint getBezierPoint(CurvePolygon shape, double weight) {
        if (shape.lines.length == 1)
            return shape.lines[0].getPointAtWeight(weight);
        else {
            CircuitPoint[] pointsAtWeight = new CircuitPoint[shape.lines.length];
            for (int i = 0; i < shape.lines.length; i++)
                pointsAtWeight[i] = shape.lines[i].getPointAtWeight(weight);
            return getBezierPoint(new CurvePolygon(pointsAtWeight), weight);
        }
    }
}
