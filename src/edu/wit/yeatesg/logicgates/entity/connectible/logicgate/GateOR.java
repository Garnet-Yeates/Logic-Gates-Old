package edu.wit.yeatesg.logicgates.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.def.BezierCurve;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.entity.Rotatable;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;

public class GateOR extends LogicGate {

    /**
     * ConnectibleEntity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * set {@link #connections} to a new ConnectionList
     * call construct()
     *
     * @param origin
     * @param rotation
     * @param size
     * @param numInputs
     */
    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs, ArrayList<Integer> nots) {
        super(origin, rotation, size, numInputs, nots);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs) {
        super(origin, rotation, size, numInputs, null);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size) {
        super(origin, rotation, size, getNumBaseInputs(size));
    }

    public GateOR(CircuitPoint origin, int rotation) {
        super(origin, rotation);
    }

    public GateOR(CircuitPoint origin) {
        super(origin);
    }

    public static GateOR parse(String s, Circuit c) {
        String[] fields = s.split(",");
        CircuitPoint origin = new CircuitPoint(fields[0], fields[1], c);
        int rotation = Integer.parseInt(fields[2]);
        Size size = Size.fromString(fields[3]);
        int numInputs = Integer.parseInt(fields[4]);
        ArrayList<Integer> nots = new ArrayList<>();
        String[] notsString = fields[5].split(";");
        for (String str : notsString)
            nots.add(Integer.parseInt(str));
        return new GateOR(origin, rotation, size, numInputs, nots);
    }
    
    @Override
    public RelativePointSet getRelativePointSet() {
        Rotatable.RelativePointSet ps = new Rotatable.RelativePointSet();
        ps.add(new CircuitPoint(0, 0, c)); // Out Node / Origin, 0
        ps.add(new CircuitPoint(2.5, -5, c)); // Top right          1
        ps.add(new CircuitPoint(-2.5, -5, c)); // Top left          2
        ps.add(new CircuitPoint(-2.5, 0, c)); // Bot left          3
        ps.add(new CircuitPoint(2.5, 0, c)); // Bot right         4

        double yDist = ps.get(2).y - ps.get(3).y;
        double xDist = ps.get(0).x - ps.get(3).x;
        double xPercent = 0.0;
        double yPercent = 0.15;

        ps.add(new CircuitPoint(ps.get(0).x - xDist + xDist*xPercent
                , ps.get(3).y + yDist*yPercent, c));      // BL control point     5
        ps.add(new CircuitPoint(ps.get(0).x + xDist - xDist*xPercent,
                ps.get(4).y + yDist*yPercent, c));       // BR control point      6

        // Curve from 2 -> 5 -> 0 (Top left -> BL Control -> Origin)
        // Curve from 1 -> 6 -> 0 (Top right -> BR Control -> Origin

        double backCurveYPercent = 0.20; // percent dist from inner side of back curve down to origin, for control point
        ps.add(new CircuitPoint(ps.get(0).x,
                ps.get(2).y + backCurveYPercent*-1*yDist, c)); // Back curve controller 7

        double backCurveUpShift = 0.1;

        ps.add(new CircuitPoint(ps.get(1).x, ps.get(1).y - backCurveUpShift, c)); // 8
        ps.add(new CircuitPoint(ps.get(2).x, ps.get(2).y - backCurveUpShift, c)); // 9

        // Curve from 8 -> 7 -> 9 (Back Curve) with inputs

        // 10 is the input origin
        ps.add(new CircuitPoint(0, -5, c));
        return ps;
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        g.setStroke(col == null ? Color.BLACK : col);
        g.setLineWidth(c.getLineWidth());

        BezierCurve curve = new BezierCurve(drawPoints.get(2), drawPoints.get(5), drawPoints.get(0));
        BezierCurve curve2 = new BezierCurve(drawPoints.get(1), drawPoints.get(6), drawPoints.get(0));

        BezierCurve backCurve = new BezierCurve(drawPoints.get(8), drawPoints.get(7), drawPoints.get(9));

        curve.draw(g, Color.BLACK, c.getLineWidth());
        curve2.draw(g, Color.BLACK, c.getLineWidth());
        backCurve.draw(g, Color.BLACK, c.getLineWidth());

        drawWings(g, col);

        for (ConnectionNode node : connections)
            node.draw(g, col, opacity);

    }

    @Override
    protected int getInputOriginIndex() {
        return 10;
    }

    @Override
    public void determinePowerStateOf(OutputNode outputNode) {
        if (outputNode.getPowerStatus() == Dependent.PowerStatus.UNDETERMINED) {
            LinkedList<InputNode> relevants = getRelevantInputNodesFor(outputNode);
            if (relevants.size() == 0)
                outputNode.setPowerStatus(Dependent.PowerStatus.PARTIALLY_DEPENDENT);
            if (outputNode.getPowerStatus() == Dependent.PowerStatus.UNDETERMINED) {
                outputNode.setPowerStatus(Dependent.PowerStatus.OFF);
                for (InputNode n : getRelevantInputNodesFor(outputNode)) {
                    if (n.getPowerStatus() == Dependent.PowerStatus.ON) {
                        outputNode.setPowerStatus(Dependent.PowerStatus.ON);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof GateOR
                && ((GateOR) other).origin.isSimilar(origin)
                && ((GateOR) other).rotation == rotation
                && ((GateOR) other).size == size
                && hasSimilarNots((GateOR) other);
    }

    @Override
    public GateOR clone(Circuit onto) {
        return new GateOR(origin.clone(onto), rotation, size, numInputs, nots);
    }

    @Override
    public String getDisplayName() {
        return "XOR Gate";
    }
}
