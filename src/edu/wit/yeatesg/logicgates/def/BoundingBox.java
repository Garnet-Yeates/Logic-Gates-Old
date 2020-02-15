package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;

public class BoundingBox {

    public CircuitPoint p1, p2,
                        p3, p4;

    public BoundingBox(CircuitPoint corner1, CircuitPoint corner2) {
        Circuit c = corner2.getCircuit();
        if (corner1.x >= corner2.x) {
            if (corner1.y >= corner2.y) {
                p4 = corner1;
                p1 = corner2;
                p2 = new CircuitPoint(p4.x, p1.y, c);
                p3 = new CircuitPoint(p1.x, p4.y, c);
            } else {
                p2 = corner1;
                p3 = corner2;
                p1 = new CircuitPoint(p3.x, p2.y, c);
                p4 = new CircuitPoint(p2.x, p3.y, c);
            }
        } else {
            if (corner1.y >= corner2.y) {
                p3 = corner1;
                p2 = corner2;
                p1 = new CircuitPoint(p3.x, p2.y, c);
                p4 = new CircuitPoint(p2.x, p3.y, c);
            } else {
                p1 = corner1;
                p4 = corner2;
                p2 = new CircuitPoint(p4.x, p1.y, c);
                p3 = new CircuitPoint(p1.x, p4.y, c);
            }
        }
    }

    public BoundingBox(PanelDrawPoint corner1, PanelDrawPoint corner2) {
        this(corner1.toCircuitPoint(), corner2.toCircuitPoint());
    }

    public boolean intercepts(BoundingBox other) {
        for (CircuitPoint p : new CircuitPoint[] { p1, p2, p3, p4 })
            if (intercepts(p))
                return true;
        return false;
    }

    public boolean intercepts(CircuitPoint p) {
        return intercepts(p.toPanelDrawPoint());
    }

    public boolean intercepts(PanelDrawPoint p) {
        PanelDrawPoint p1 = this.p1.toPanelDrawPoint();
        PanelDrawPoint p4 = this.p4.toPanelDrawPoint();
        System.out.println();
        for (CircuitPoint pee : new CircuitPoint[] {this.p1, p2, p3, this.p4,}) {
            System.out.print(pee + " ");
        }
        System.out.println("\nP E NUTS" );
        int thresh = (int) (p1.getCircuit().getScale()*0.3);
        if (p1.x == p4.x || p1.y == p4.y) {
            return new BoundingBox(new PanelDrawPoint(p1.x - thresh, p1.y - thresh, p1.getCircuit()),
                    new PanelDrawPoint(p4.x + thresh, p4.y + thresh, p4.getCircuit())).intercepts(p);
        }
        return p.x >= p1.x && p.x <= p4.x && p.y >= p1.y && p.y <= p4.y;
    }

    public static final Color BOX_COL = new Color(255, 249, 230);
    public static final Color BORDER_COL = Color.black;

    public void paint(Graphics2D g) {
        Circuit c = p1.getCircuit();
        int strokeSize = (int) (c.getScale() * 0.6);
        if (strokeSize % 2 == 0) strokeSize++;
        int borderThickness = (int) Math.ceil(strokeSize / 5.00) + 1;
        int innerStrokeSize = strokeSize - borderThickness;
        if (innerStrokeSize % 2 == 0) innerStrokeSize++;
        for (CircuitPoint p : new CircuitPoint[] { p1, p2, p3, p4 }) {
            PanelDrawPoint pp = p.toPanelDrawPoint();
            g.setColor(BORDER_COL);
            g.setStroke(new BasicStroke(strokeSize));
            g.drawLine(pp.x, pp.y, pp.x, pp.y);
            g.setColor(BOX_COL);
            g.setStroke(new BasicStroke(innerStrokeSize));
            g.drawLine(pp.x, pp.y, pp.x, pp.y);
        }
    }

    public void paintSimple(Graphics2D g) {
        Stroke stroke = new BasicStroke(1);
        g.setStroke(stroke);
        g.setColor(Color.orange);
        PanelDrawPoint p1 = this.p1.toPanelDrawPoint();
        PanelDrawPoint p2 = this.p2.toPanelDrawPoint();
        PanelDrawPoint p3 = this.p3.toPanelDrawPoint();
        PanelDrawPoint p4 = this.p4.toPanelDrawPoint();
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        g.drawLine(p2.x, p2.y, p4.x, p4.y);
        g.drawLine(p4.x, p4.y, p3.x, p3.y);
        g.drawLine(p3.x, p3.y, p1.x, p1.y);
    }
}
