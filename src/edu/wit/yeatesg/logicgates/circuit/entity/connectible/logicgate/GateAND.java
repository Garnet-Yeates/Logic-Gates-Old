package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;


import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.BezierCurve;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPointList;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class GateAND extends LogicGate {


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
    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate,
                     ArrayList<Integer> negatedInputIndices, OutputType outType, int dataBits) {
        super(origin, rotation, size, numInputs, negate, negatedInputIndices, outType, dataBits);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate,
                   ArrayList<Integer> negatedInputIndices, OutputType outType) {
        super(origin, rotation, size, numInputs, negate, negatedInputIndices, outType, 1);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate,
                   ArrayList<Integer> negatedInputIndices) {
        super(origin, rotation, size, numInputs, negate, negatedInputIndices, OutputType.ZERO_ONE);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate) {
        super(origin, rotation, size, numInputs, negate, null);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs) {
        super(origin, rotation, size, numInputs, false);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size) {
        super(origin, rotation, size, getNumBaseInputs(size));
    }

    public GateAND(CircuitPoint origin, int rotation) {
        super(origin, rotation, Size.NORMAL);
    }

    public GateAND(CircuitPoint origin) {
        super(origin, 270);
    }
    
    public static GateAND parse(String s, Circuit c) {
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
        return new GateAND(origin, rotation, size, numInputs, negate, nots, outType, dataBits);
    }

    @Override
    protected double getOuterWingXOffset() {
        return 0;
    }

    @Override
    protected double getOuterWingYOffset() {
        return 0;
    }

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet relatives = new RelativePointSet();
        Circuit c = getCircuit();
        // Origin (middle bot of curve (u shaped curve))

        if (size == NORMAL) {
            relatives.add(0, 0, c);             // 0

            // Top left of curve
            relatives.add(-2.5, -2.5, c);    //  1
            // Bottom left of curve
            relatives.add(-2.5, 0.8, c);      //  2
            // Bottom right of curve
            relatives.add(2.5, 0.8, c);       //  3
            // Top right of curve
            relatives.add(2.5, -2.5, c);     //  4

            // Top right
            relatives.add(2.5, -5, c);        //  5
            // Top left
            relatives.add(-2.5, -5, c);       //  6
        } else {
            relatives.add(0, 0, c);             // 0

            // Top left of curve
            relatives.add(-1.5, -1.5, c);    //  1
            // Bottom left of curve
            relatives.add(-1.5, 0.5, c);      //  2
            // Bottom right of curve
            relatives.add(1.5, 0.5, c);       //  3
            // Top right of curve
            relatives.add(1.5, -1.5, c);     //  4

            // Top right
            relatives.add(1.5, -3, c);        //  5
            // Top left
            relatives.add(-1.5, -3, c);       //  6
        }


  //      relatives.add();

        return relatives;
    }

    @Override
    public Vector getOriginToInputOrigin() {
        return new Vector(0, size == NORMAL ? -5 : -3);
    }

    @Override
    public InputWing getRelativeMainInputWing() {
        RelativePointSet rps = getRelativePointSet();
        return new LineInputWing(rps.get(5), rps.get(6));
    }

    @Override
    public InputWing getInputWing(CircuitPoint relativeStart, CircuitPoint relativeControl, CircuitPoint relativeEnd) {
        return new LineInputWing(relativeStart.getRotated(origin, rotation), relativeEnd.getRotated(origin, rotation));
    }

    @Override
    public PowerValue getLocalPowerStateOf(OutputNode outputNode) {
        ArrayList<PowerValue> powerVals = outputNode.getRelevantPowerValuesAffectingMe();
        if (powerVals.size() == 0)
            return PowerValue.FLOATING_ERROR;

        PowerValue returning = null;
        for (PowerValue val : powerVals) {
            if (val == PowerValue.OFF) {
                returning = PowerValue.OFF;
                break;
            }
        }
        if (returning == null)
            returning = PowerValue.ON;
        if (outputNode.isNegated())
            returning = returning.getNegated();
        if ( returning == PowerValue.ON && outputType == OutputType.ZERO_FLOATING || returning == PowerValue.OFF && outputType == OutputType.FLOATING_ONE)
            returning = PowerValue.FLOATING;
        return returning;
    }


    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof GateAND
                && ((GateAND) other).origin.isSimilar(origin)
                && ((GateAND) other).rotation == rotation
                && ((GateAND) other).size == size
                && hasSimilarNots((GateAND) other);
    }


    @Override
    public GateAND getCloned(Circuit onto) {
        return new GateAND(origin.clone(onto), rotation, size, numInputs, out.isNegated(), new ArrayList<>(inNots), outputType, dataBits);
    }

    @Override
    public String getDisplayName() {
        return "AND Gate";
    }

    @Override
    public String getPropertyTableHeader() {
        return "Properties For: Gate " + (!out.isNegated() ? "AND" : "NAND");
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        drawInputTails(g, col);
        Color strokeCol = col == null ? Color.BLACK : col;
        strokeCol = Color.rgb((int) (255*strokeCol.getRed()), (int) (255*strokeCol.getGreen()), (int) (255*strokeCol.getBlue()), opacity);
        g.setStroke(strokeCol);
        g.setLineWidth(getLineWidth());
        CircuitPointList ps = drawPoints;

        PanelDrawPoint p5 = ps.get(5).toPanelDrawPoint();
        PanelDrawPoint p6 = ps.get(6).toPanelDrawPoint();
        PanelDrawPoint p1 = ps.get(1).toPanelDrawPoint();
        PanelDrawPoint p4 = ps.get(4).toPanelDrawPoint();

        double itMult = size == Size.NORMAL ? 1 : 1;

        // Curve 7, 1, 2, 0, 3, 4, 8
        BezierCurve curve = new BezierCurve(itMult, ps.get(1), ps.get(2), ps.get(3), ps.get(4));
        curve.draw(g, col, getLineWidth());

        // Line 8 to 5
        g.strokeLine(p4.x, p4.y, p5.x, p5.y);

        // Line 6 to 1
        g.strokeLine(p6.x, p6.y, p1.x, p1.y);

        drawWings(g, col);

        for (ConnectionNode node : connections)
            node.draw(g, col, opacity);
    }




}
