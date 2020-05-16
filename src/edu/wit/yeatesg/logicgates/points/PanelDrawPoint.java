package edu.wit.yeatesg.logicgates.points;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;

public class PanelDrawPoint  {

    protected Circuit c;

    public int x;
    public int y;

    public PanelDrawPoint(int x, int y, Circuit c) {
        this.x = x;
        this.y = y;
        this.c = c;
    }

    public PanelDrawPoint(CircuitPoint circuitPoint) {
        this((int) ((circuitPoint.x * circuitPoint.getCircuit().getScale()) + circuitPoint.getCircuit().getXOffset()),
                (int) ((circuitPoint.y * circuitPoint.getCircuit().getScale()) + circuitPoint.getCircuit().getYOffset()),
                circuitPoint.getCircuit());
    }




    public CircuitPoint toCircuitPoint() {
        return new CircuitPoint(this);
    }

    public Circuit getCircuit() {
        return c;
    }

    public PanelDrawPoint getIfModifiedBy(Vector vector) {
        return new PanelDrawPoint((int) (x + vector.x), (int) (y + vector.y), c);
    }

    @Override
    public String toString() {
        return "( " + x + " , " + y + " )";
    }

    @Override
    public PanelDrawPoint clone() {
        return new PanelDrawPoint(x, y, c);
    }
}
