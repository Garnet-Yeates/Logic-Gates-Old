package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.gui.MainGUI;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class LogicGates {

    private long startTime;

    public static String doTimeTest(Runnable r) {
        long start = System.currentTimeMillis();
        r.run();
        return ((System.currentTimeMillis() - start) / 1000.0) + "s";
    }

    public Iterator<CircuitPoint> lefterToRighterIterator(CircuitPoint p1, CircuitPoint p2) {
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
}
