package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class BoundingBox {

    public CircuitPoint p1, p2,
                        p3, p4;

    private Entity owner;

    public BoundingBox(PointSet points, Entity owner) {
        this.owner = owner;
        Circuit c = points.get(0).getCircuit();
        double leftMost = Double.MAX_VALUE, rightMost = Double.MAX_VALUE*-1, topMost = Double.MAX_VALUE,
                bottomMost = Double.MAX_VALUE*-1;
        for (CircuitPoint cp : points) {
            if (cp.x < leftMost)
                leftMost = cp.x;
            if (cp.x > rightMost)
                rightMost = cp.x;
            if (cp.y < topMost)
                topMost = cp.y;
            if (cp.y > bottomMost)
                bottomMost = cp.y;
        }
        System.out.println();
        System.out.println("LEFT MOST " + leftMost);
        System.out.println("RIGHT MOST " + rightMost);
        System.out.println("TOP MOST " + topMost);
        System.out.println("BOT MOST " + bottomMost);
        System.out.println();


        initFromTwoCorners(new CircuitPoint(leftMost, topMost, c), new CircuitPoint(rightMost, bottomMost, c));
        
    }
    
    public BoundingBox(CircuitPoint corner1, CircuitPoint corner2, Entity owner) {
        this.owner = owner;
        initFromTwoCorners(corner1, corner2);
    }

    public void initFromTwoCorners(CircuitPoint corner1, CircuitPoint corner2) {
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

    public double getRight() {
        return p4.x;
    }

    public double getLeft() {
        return p1.x;
    }

    public int getDrawWidth() {
        return (int) (getWidth() * p1.getCircuit().getScale());
    }

    public int getDrawHeight() {
        return (int) (getHeight() * p1.getCircuit().getScale());
    }

    public double getWidth() {
        return p4.x - p1.x;
    }

    public double getHeight() {
        return p4.y - p1.y;

    }

    public BoundingBox(PanelDrawPoint corner1, PanelDrawPoint corner2, Entity owner) {
        this(corner1.toCircuitPoint(), corner2.toCircuitPoint(), owner);
    }

    public BoundingBox getExpandedBy(double amount) {
        return new BoundingBox(new CircuitPoint(p1.x - amount, p1.y - amount, p1.getCircuit()),
                new CircuitPoint(p4.x + amount, p4.y + amount, p1.getCircuit()), owner);
    }

    public PointSet getInterceptPoints() {
        PointSet gridPointsWithin = new PointSet();
        CircuitPoint topLeft = p1.getGridSnapped();
        CircuitPoint bottomRight = p4.getGridSnapped();
        for (int y = (int) topLeft.y; y <= bottomRight.y; y++)
            for (int x = (int) topLeft.x; x <= bottomRight.x; x++)
                gridPointsWithin.add(new CircuitPoint(x, y, topLeft.getCircuit()));
        return gridPointsWithin;
    }

    public boolean intercepts(PanelDrawPoint p) {
        return intercepts(p.toCircuitPoint());
    }

    public boolean intercepts(CircuitPoint p) {
        if ((p1.x == p4.x || p1.y == p4.y))
            return getExpandedBy(0.3).intercepts(p);
        return p.x >= p1.x && p.x <= p4.x && p.y >= p1.y && p.y <= p4.y;
    }

    public boolean intercepts(Entity e) {
        for (CircuitPoint intPoint : e.getInterceptPoints())
            if (intercepts(intPoint))
                return true;
        return false;
    }

    public EntityList<Entity> getInterceptingEntities() {
        EntityList<Entity> interceptors = new EntityList<>();
        for (Entity e : p1.getCircuit().getAllEntities())
            if (intercepts(e))
                interceptors.add(e);
        return interceptors;
    }

    public BoundingBox clone() {
        return new BoundingBox(p1.clone(), p4.clone(), owner);
    }

    public static final Color BOX_COL = Color.rgb(255, 249, 230, 1);
    public static final Color OUTLINE_COL = Color.BLACK;

    public void draw(GraphicsContext g) {
        Circuit c = p1.getCircuit();

        double borderLen = c.getScale()*0.65;
        double borderThicc = borderLen*0.15;
        double off = borderLen / 2;

        for (CircuitPoint p : new CircuitPoint[]{p1, p2, p3, p4}) {
            for (int i = 0; i < 1; i++) {
                PanelDrawPoint pp = p.toPanelDrawPoint();
                g.setFill(BOX_COL);
                g.fillRect(pp.x - off, pp.y - off, borderLen, borderLen);
            }
        }

        for (CircuitPoint p : new CircuitPoint[]{p1, p2, p3, p4}) {
            for (int i = 0; i < 3; i++) {
                PanelDrawPoint pp = p.toPanelDrawPoint();
                g.setStroke(OUTLINE_COL);
                g.setLineWidth(borderThicc);
                g.strokeRect(pp.x - off, pp.y - off, borderLen, borderLen);
            }
        }


    }

    public Color HIGHLIGHT_COL = Color.ORANGE;

    public void drawBorder(GraphicsContext g) {
        g.setStroke(HIGHLIGHT_COL);
        double ownerStroke = owner == null ? 1 : owner.getLineWidth();
        double stroke = (ownerStroke * 0.55);
        stroke = Math.min(ownerStroke - 2, stroke);
        stroke = Math.max(1, stroke);
        g.setLineWidth(stroke);
        PanelDrawPoint p1 = this.p1.toPanelDrawPoint();
        PanelDrawPoint p2 = this.p2.toPanelDrawPoint();
        PanelDrawPoint p3 = this.p3.toPanelDrawPoint();
        PanelDrawPoint p4 = this.p4.toPanelDrawPoint();
        g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        g.strokeLine(p2.x, p2.y, p4.x, p4.y);
        g.strokeLine(p4.x, p4.y, p3.x, p3.y);
        g.strokeLine(p3.x, p3.y, p1.x, p1.y);
    }
}
