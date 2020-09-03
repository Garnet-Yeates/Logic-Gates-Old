package edu.wit.yeatesg.logicgates.datatypes;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public class BezierCurve {

    public CircuitPoint[] points;

    private CurvePolygon shape;
    private double iterationMultiplier;
    private Circuit c;

    public BezierCurve(CurvePolygon shape, double iterationMultiplier) {
        this.shape = shape;
        this.iterationMultiplier = iterationMultiplier;
        c = shape.points[0].getCircuit();
        construct();
    }

    public double lastScale;

    public void construct() {
        double scale = c.getScale();
        lastScale = scale;

        int numIterations = 12;
        if (scale == 12)
            numIterations = 14;
    //    if (scale == 10)
    //        numIterations = 12;
        if (scale < 10)
            numIterations = 8;

        numIterations = (int) Math.max(iterationMultiplier*numIterations, 6);

        points = new CircuitPoint[numIterations];
        double weightInc = 1.0 / (numIterations - 1);
        double currWeight = 0;
        for (int i = 0; i < numIterations; i++) {
            points[i] = getBezierPoint(currWeight);
            currWeight += weightInc;
        }
    }

    public BezierCurve(Circuit c, double iterationMultiplier, double... circuitPoints) {
        this(new CurvePolygon(c, circuitPoints), iterationMultiplier);
    }

    public BezierCurve(double iterationMultiplier, CircuitPoint... points) {
        this(new CurvePolygon(points), iterationMultiplier);
    }

    public void draw(GraphicsContext g, Color col, double lineWidth) {
        g.setStroke(col == null ? Color.BLACK : col);
        g.setLineWidth(lineWidth);
        double scale = c.getScale();
        if (scale != lastScale)
            construct();
        g.beginPath();
        double[] xs = new double[points.length];
        double[] ys = new double[points.length];
        for (int i = 0; i < points.length; i++) {
            PanelDrawPoint pp = points[i].toPanelDrawPoint();
            xs[i] = pp.x;
            ys[i] = pp.y;

        }
        g.strokePolyline(xs, ys, xs.length);
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
