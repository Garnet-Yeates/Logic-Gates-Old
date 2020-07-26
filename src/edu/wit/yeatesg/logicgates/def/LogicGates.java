package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.gui.MainGUI;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

    public static void drawText(String text, double lineWidth, Circuit c, GraphicsContext g, Color col, CircuitPoint center, double fit) {
        Color strokeCol = col == null ? Color.BLACK : col;
        g.setStroke(strokeCol);

        Text toFindSize = new Text(text);
        double width;
        double height;
        double fontSize = (fit / (c.getScale()*1.5))*c.getScale() * 2.6;
        Font f;
        int numIts = 0;
        do{
            numIts++;
            toFindSize.setFont((f = new Font("Consolas", fontSize -= Math.max(0.1, fontSize * 0.05))));
            toFindSize.setTextAlignment(TextAlignment.CENTER);
            toFindSize.applyCss();
            width = toFindSize.getLayoutBounds().getWidth();
            height = fontSize*0.66;
        } while (width > fit || height > fit);

        PanelDrawPoint pp = center.toPanelDrawPoint();



        pp.y += height / 2;
        pp.x -= width / 2;

        g.setFill(strokeCol);
        g.setLineWidth(lineWidth);
        g.setFont(f);
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText(text, pp.x + 0.45, pp.y);

        pp.x -= width / 2;
    }

}
