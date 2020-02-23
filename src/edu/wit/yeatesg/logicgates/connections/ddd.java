package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.*;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import javax.swing.*;
import java.util.LinkedList;

public class ddd {

    public static void main(String[] args) {
        int fuck = 0;
        for (int i = 0; i < 10; i++)
            if (i == 5) {
                System.out.println("hey");
            }

        if (fuck == 1) {
            JFrame frame = new JFrame();
            EditorPanel panel = new EditorPanel(frame);
            Circuit c = panel.getCurrentCircuit();
 /*       Wire w = new Wire(new CircuitPoint(0, 0, c), new CircuitPoint(10, 0, c));
        Wire w2 = new Wire(new CircuitPoint(3, 0, c), new CircuitPoint(-5, 0, c));
        LogicGates.debug("Intersect", w.getInterceptPoints().intersection(w2.getInterceptPoints()));
        LogicGates.debug("Intercept", w.getInterceptPoints(w2));
        LogicGates.debug("InvalidInterceptPoints", w.getInvalidInterceptPoints(w2));
        Wire w3 = new Wire(new CircuitPoint(5, -5, c), new CircuitPoint(5, -10, c));
        Wire.TheoreticalWire theo = new Wire.TheoreticalWire(new CircuitPoint(0, -5, c), new CircuitPoint(8, -5, c));
        LogicGates.debug("CanPlace " + theo + " on circuit ",
                Wire.canPlaceWireWithoutInterceptingAnything(theo, new LinkedList<>(), new Entity.InterceptExceptionList(
                ), false));*/


            new Wire(new CircuitPoint(-3, -1, c), new CircuitPoint(3, -1, c));
            new Wire(new CircuitPoint(-3, -1, c), new CircuitPoint(-3, -8, c));
            new Wire(new CircuitPoint(-1, -1, c), new CircuitPoint(-1, 5, c));

            //   new Wire(new CircuitPoint(3, 5, c), new CircuitPoint(3, -5, c));
            boolean canPlaceT = Wire.canPlaceWireWithoutInterceptingAnything(new Wire.TheoreticalWire(new CircuitPoint(3, 5, c),
                            new CircuitPoint(3, -5, c)), new LinkedList<>(), new Entity.PermitList(),
                    false);
            System.out.println("CanPlaceT? " + canPlaceT);

            boolean canPlaceL = Wire.canPlaceWireWithoutInterceptingAnything(new Wire.TheoreticalWire(new CircuitPoint(10, 5, c),
                            new CircuitPoint(-1, 5, c)), new LinkedList<>(), new Entity.PermitList(),
                    false);
            System.out.println("CanPlaceL? " + canPlaceL);

            //   boolean canPlaceL =

//        Wire.genWirePath(new CircuitPoint(0, 0, c), new CircuitPoint(-5, -5, c), Direction.HORIZONTAL, 20, false);


        }

    }

}
