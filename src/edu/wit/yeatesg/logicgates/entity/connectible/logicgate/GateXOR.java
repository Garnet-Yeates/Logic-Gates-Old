package edu.wit.yeatesg.logicgates.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.def.BezierCurve;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.Rotatable;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import static edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class GateXOR extends LogicGate {

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
    public GateXOR(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate, ArrayList<Integer> nots) {
        super(origin, rotation, size, numInputs, negate, nots);
    }

    public GateXOR(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate) {
        super(origin, rotation, size, numInputs, negate);
    }

    public GateXOR(CircuitPoint origin, int rotation, Size size, int numInputs) {
        super(origin, rotation, size, numInputs);
    }

    public GateXOR(CircuitPoint origin, int rotation, Size size) {
        super(origin, rotation, size);
    }

    public GateXOR(CircuitPoint origin, int rotation) {
        super(origin, rotation);
    }

    public GateXOR(CircuitPoint origin) {
        super(origin);
    }
    
    public static GateXOR parse(String s, Circuit c) {
        String[] fields = s.split(",");
        CircuitPoint origin = new CircuitPoint(fields[0], fields[1], c);
        int rotation = Integer.parseInt(fields[2]);
        Size size = Size.fromString(fields[3]);
        int numInputs = Integer.parseInt(fields[4]);
        boolean negate = Boolean.parseBoolean(fields[5]);
        ArrayList<Integer> nots = new ArrayList<>();
        String[] notsString = fields[6].split(";");
        for (String str : notsString)
            nots.add(Integer.parseInt(str));
        return new GateXOR(origin, rotation, size, numInputs, negate, nots);
    }

    @Override
    public InputWing getInputWing(CircuitPoint relativeStart, CircuitPoint relativeControl, CircuitPoint relativeEnd) {
        CircuitPoint p1 = relativeStart.getSimilar();
        CircuitPoint p2 = relativeEnd.getSimilar();
        CircuitPoint lefter = p1.y == p2.y ? (p1.x < p2.x ? p1 : p2) : (p1.y < p2.y ? p1 : p2);
        CircuitPoint righter = lefter == p1 ? p2 : p1;
        Vector startToEnd = new Vector(lefter, righter);
        double distDown = 0.15;
        distDown += 0.85 * (startToEnd.getLength() / 6);
        distDown = Math.min(1.35, distDown);
        double dist = startToEnd.getLength();
        CircuitPoint middle = lefter.getIfModifiedBy(startToEnd.getUnitVector().getMultiplied(dist / 2));
        return new CurveInputWing(lefter.getRotated(origin, rotation),
                relativeControl == null ? middle.getIfModifiedBy(new Vector(0, distDown)).getRotated(origin, rotation) : relativeControl.getRotated(origin, rotation),
                righter.getRotated(origin, rotation));
    }

    @Override
    public InputWing getRelativeMainInputWing() {
        RelativePointSet rps = getRelativePointSet();
        return new CurveInputWing(rps.get(10), rps.get(11), rps.get(12));
    }

    private double backCurveUpShift = 0.1;

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

        double backCurveYPercent = 0.3; // percent dist from inner side of back curve down to origin, for control point
        ps.add(new CircuitPoint(ps.get(0).x,
                ps.get(2).y + backCurveYPercent*-1*yDist, c)); // Back curve controller 7

        ps.add(new CircuitPoint(ps.get(1).x, ps.get(1).y - backCurveUpShift, c)); // 8
        ps.add(new CircuitPoint(ps.get(2).x, ps.get(2).y - backCurveUpShift, c)); // 9

        // Curve from 8 -> 7 -> 9 back curve with inputs

        ps.add(ps.get(8).x, ps.get(8).y - 1, c); // 10
        ps.add(ps.get(7).x, ps.get(7).y - 1, c); // 11
        ps.add(ps.get(9).x, ps.get(9).y - 1, c); // 12

        // Curve from 10 -> 11 -> 12 (XOR Gate)

        // 10 is the input origin
        ps.add(new CircuitPoint(0, -5, c));
        return ps;
    }

    @Override
    protected double getOuterWingXOffset() {
        return 0.5;
    }

    @Override
    protected double getOuterWingYOffset() {
        return backCurveUpShift;
    }

    @Override
    public Vector getOriginToInputOrigin() {
        return new Vector(0, -6);
    }


    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        drawInputTails(g, col);
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
    public void determinePowerStateOf(OutputNode outNode) {
        if (outNode.getPowerStatus() == Dependent.PowerStatus.UNDETERMINED) {
            LinkedList<InputNode> relevants = getRelevantInputNodesFor(outNode);
            if (relevants.size() == 0)
                outNode.setPowerStatus(Dependent.PowerStatus.PARTIALLY_DEPENDENT);
            if (outNode.getPowerStatus() == PowerStatus.UNDETERMINED) {
                int numOn = 0;
                for (InputNode in : relevants) {
                    if (in.getTruePowerValue() == PowerStatus.ON) {
                        numOn++;
                    }
                }

                PowerStatus out = PowerStatus.OFF;
                if (numOn == 1)
                    out = PowerStatus.ON;
                // TODO if it's notted flip 'out' power status
                outNode.setPowerStatus(out);
            }

        }
    }

    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof GateXOR
                && ((GateXOR) other).origin.isSimilar(origin)
                && ((GateXOR) other).rotation == rotation
                && ((GateXOR) other).size == size
                && hasSimilarNots((GateXOR) other);
    }

    @Override
    public GateXOR clone(Circuit onto) {
        return new GateXOR(origin.clone(onto), rotation, size, numInputs, out.isNegated(), new ArrayList<>(inNots));
    }

    @Override
    public String getDisplayName() {
        return "XOR Gate";
    }
}
