package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.BezierCurve;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.Rotatable;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class GateOR extends LogicGate {


    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate,
                   ArrayList<Integer> negatedInputIndices, OutputType outType, int dataBits) {
        super(origin, rotation, size, numInputs, negate, negatedInputIndices, outType, dataBits);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate,
                   ArrayList<Integer> negatedInputIndices, OutputType outType) {
        super(origin, rotation, size, numInputs, negate, negatedInputIndices, outType, 1);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate,
                   ArrayList<Integer> negatedInputIndices) {
        super(origin, rotation, size, numInputs, negate, negatedInputIndices, OutputType.ZERO_ONE);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate) {
        super(origin, rotation, size, numInputs, negate, null);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size, int numInputs) {
        super(origin, rotation, size, numInputs, false);
    }

    public GateOR(CircuitPoint origin, int rotation, Size size) {
        super(origin, rotation, size, getNumBaseInputs(size));
    }

    public GateOR(CircuitPoint origin, int rotation) {
        super(origin, rotation, Size.MEDIUM);
    }

    public GateOR(CircuitPoint origin) {
        super(origin, 270);
    }

    public static GateOR parse(String s, Circuit c) {
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
        OutputType outType = OutputType.parse(fields[7]);
        int dataBits = Integer.parseInt(fields[8]);
        return new GateOR(origin, rotation, size, numInputs, negate, nots, outType, dataBits);
    }

    @Override
    public void setDefaults() {
        backCurveUpShift = 0.08;
    }

    @Override
    public InputWing getInputWing(CircuitPoint relativeStart, CircuitPoint relativeControl, CircuitPoint relativeEnd) {
        CircuitPoint p1 = relativeStart.getSimilar();
        CircuitPoint p2 = relativeEnd.getSimilar();
        CircuitPoint lefter = p1.y == p2.y ? (p1.x < p2.x ? p1 : p2) : (p1.y < p2.y ? p1 : p2);
        CircuitPoint righter = lefter == p1 ? p2 : p1;
        Vector startToEnd = new Vector(lefter, righter);
        final double maxDistDown = 3;
        int inputProgress = numInputs % 2 == 0 ? numInputs : numInputs - 1;
        double distDown = ( (inputProgress - getNumBaseInputs()) / (32.0 - getNumBaseInputs()) ) * maxDistDown;
        double dist = startToEnd.getLength();
        CircuitPoint middle = lefter.getIfModifiedBy(startToEnd.getUnitVector().getMultiplied(dist / 2));
        return new CurveInputWing(lefter.getRotated(origin, rotation),
                relativeControl == null ? middle.getIfModifiedBy(new Vector(0, distDown)).getRotated(origin, rotation) : relativeControl.getRotated(origin, rotation),
                righter.getRotated(origin, rotation));
    }

    @Override
    public InputWing getRelativeMainInputWing() {
        RelativePointSet rps = getRelativePointSet();
        return new CurveInputWing(rps.get(8), rps.get(7), rps.get(9));
    }

    private double backCurveUpShift;

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

        // Curve from 8 -> 7 -> 9 (Main Wing) with inputs

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
        return new Vector(0, -5);
    }


    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        drawInputTails(g, col);
        g.setStroke(col == null ? Color.BLACK : col);
        g.setLineWidth(c.getLineWidth());

        BezierCurve curve = new BezierCurve(drawPoints.get(2), drawPoints.get(5), drawPoints.get(0));
        BezierCurve curve2 = new BezierCurve(drawPoints.get(1), drawPoints.get(6), drawPoints.get(0));

        curve.draw(g, col, c.getLineWidth());
        curve2.draw(g, col, c.getLineWidth());

        drawWings(g, col);

        for (ConnectionNode node : connections)
            node.draw(g, col, opacity);
    }


    @Override
    public PowerValue getLocalPowerStateOf(OutputNode outputNode) {
        if (outputNode.getPowerValueFromTree() != PowerValue.UNDETERMINED)
            throw new RuntimeException();
        ArrayList<PowerValue> powerVals = outputNode.getRelevantPowerValuesAffectingMe();
        if (powerVals.size() == 0)
            return PowerValue.FLOATING_ERROR;

        PowerValue returning = null;
        for (PowerValue val : powerVals) {
            if (val == PowerValue.ON) {
                returning = PowerValue.ON;
                break;
            }
        }
        if (returning == null)
            returning = PowerValue.OFF;
        if (outputNode.isNegated())
            returning = returning.getNegated();
        if ( returning == PowerValue.ON && outputType == OutputType.ZERO_FLOATING || returning == PowerValue.OFF && outputType == OutputType.FLOATING_ONE)
            returning = PowerValue.FLOATING;
        return returning;
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
    public GateOR getCloned(Circuit onto) {
        return new GateOR(origin.clone(onto), rotation, size, numInputs, out.isNegated(), new ArrayList<>(inNots), outputType, dataBits);
    }

    @Override
    public String getDisplayName() {
        return "OR Gate";
    }


    @Override
    public String getPropertyTableHeader() {
        return "Properties For: Gate " + (!out.isNegated() ? "OR" : "NOR");
    }

}
