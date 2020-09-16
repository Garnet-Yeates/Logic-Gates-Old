package edu.wit.yeatesg.logicgates;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.datatypes.*;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Collection;
import java.util.Iterator;

public class LogicGates {

    private long startTime;


    public static String doTimeTest(Runnable r) {
        long start = System.currentTimeMillis();
        r.run();
        return ((System.currentTimeMillis() - start)) + "ms";
    }

    public static Iterator<CircuitPoint> lefterToRighterIterator(CircuitPoint p1, CircuitPoint p2) {
        p1 = p1.getGridSnapped();
        p2 = p2.getGridSnapped();
        CircuitPoint lefter = p1.y == p2.y ? (
                p1.x < p2.x ? p1 : p2)
                : (p1.y < p2.y ? p1 : p2);
        CircuitPoint righter = lefter == p1 ? p2 : p1;
        lefter = lefter.getSimilar();
        righter = righter.getSimilar();
        int size = (int) (lefter.y == righter.y ? righter.x - lefter.x : righter.y - lefter.y);
        final CircuitPoint left = lefter, right = righter;
        return new Iterator<>() {
            int cursor = 0;
            Vector dir = new Vector(left, right).getUnitVector();
            CircuitPoint curr = left.getSimilar();

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public CircuitPoint next() {
                CircuitPoint returning = curr.getSimilar();
                cursor++;
                curr = dir.addedTo(curr);
                return returning;
            }
        };
    }




    @SuppressWarnings("unchecked")
    public static void debug(Object... args) {
        if (args.length % 2 != 0)
            throw new RuntimeException("Invalid Debug");
        for (int i = 0; i < args.length; i++) {
            if (i % 2 == 0)
                System.out.println(args[i]);
            else {
                if (args[i] == null)
                    System.out.println("null");
                else if (args[i].getClass().isArray()) {
                    Object[] arr = (Object[]) args[i];
                    for (Object o : arr)
                        System.out.print("  " + o);
                    System.out.println();
                } else if (args[i] instanceof Iterable<?>) {
                    ((Iterable<?>) args[i]).forEach(System.out::println);
                    System.out.println();
                } else
                    System.out.println("  " + args[i]);
            }
        }
    }

    public static void strokePolyLine(GraphicsContext g, Collection<? extends CircuitPoint> points) {
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        int i = 0;
        for (CircuitPoint point : points) {
            PanelDrawPoint pp = point.toPanelDrawPoint();
            x[i] = pp.x;
            y[i++] = pp.y;
        }
        g.strokePolyline(x, y, points.size());
    }

    public static void drawText(String text, BoundingBox b, Circuit c, GraphicsContext g, Color col) {
        Color strokeCol = col == null ? Color.BLACK : col;
        g.setStroke(strokeCol);

        double w = b.p4.x - b.p1.x;
        PanelDrawPoint center = b.p1.getIfModifiedBy(new Vector(b.p1, b.p4).getMultiplied(0.5)).toPanelDrawPoint();

        double heightInCircuitPoints = b.p3.y - b.p1.y;
        double wPP = w * c.getScale();

        Font font =  new Font("Consolas", heightInCircuitPoints*c.getScale()*1.55);

        double yShiftInCircuitPoints = 0.15;

      //  b.drawBorder(g, Color.ORANGE, 1);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.BLACK);
        g.setFont(font);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(text, center.x, center.y + yShiftInCircuitPoints * c.getScale(), wPP);

    }

}
