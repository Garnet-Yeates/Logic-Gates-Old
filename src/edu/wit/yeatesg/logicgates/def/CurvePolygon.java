package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

public class CurvePolygon {

    public final CircuitPoint[] points;
    public final Line[] lines;

    public CurvePolygon(CircuitPoint... points) {
        this.points = points;
        lines = new Line[points.length - 1];
        for (int i = 0, j = 1; j < points.length; i++, j++)
            lines[i] = new Line(points[i], points[j]);
    }

    public CurvePolygon(Circuit c, double... circuitPoints) {
        if (circuitPoints.length % 2 != 0)
            throw new RuntimeException("Invalid Bezier Curve Points!");
        this.points = new CircuitPoint[circuitPoints.length / 2];
        for (int i = 0, j = 1; j < circuitPoints.length; i += 2, j += 2)
            points[i / 2] = new CircuitPoint(circuitPoints[i], circuitPoints[j], c);
        this.lines = new Line[points.length - 1];
        for (int i = 0, j = 1; j < points.length; i++, j++)
            lines[i] = new Line(points[i], points[j]);
    }

    public CurvePolygon(PanelDrawPoint... points) {
        this.points = new CircuitPoint[points.length];
        for (int i = 0; i < points.length; i++)
            this.points[i] = points[i].toCircuitPoint();
        lines = new Line[points.length - 1];
        for (int i = 0, j = 1; j < this.points.length; i++, j++)
            lines[i] = new Line(this.points[i], this.points[j]);

    }

    public static class Line {

        public final CircuitPoint startPoint;
        public final CircuitPoint endPoint;

        public Line(CircuitPoint startPoint, CircuitPoint endPoint) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }

        public double getLength() {
            double a = startPoint.x - endPoint.x;
            double b = startPoint.y - endPoint.y;
            return Math.sqrt(a*a + b*b);
        }

        public CircuitPoint getPointAtWeight(double endWeight) {
            double startWeight = 1 - endWeight;
            double x = startWeight * startPoint.x + endWeight * endPoint.x;
            double y = startWeight * startPoint.y + endWeight * endPoint.y;
            return new CircuitPoint(x, y, startPoint.getCircuit());
        }
    }
}