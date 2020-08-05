package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;


import edu.wit.yeatesg.logicgates.datatypes.BezierCurve;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPointList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Powerable;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.LinkedList;

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
    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate, ArrayList<Integer> nots) {
        super(origin, rotation, size, numInputs, negate, nots);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, boolean negate) {
        super(origin, rotation, size, numInputs, negate);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs) {
        super(origin, rotation, size, numInputs);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size) {
        super(origin, rotation, size);
    }

    public GateAND(CircuitPoint origin, int rotation) {
        super(origin, rotation);
    }

    public GateAND(CircuitPoint origin) {
        super(origin);
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
        return new GateAND(origin, rotation, size, numInputs, negate, nots);
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
        relatives.add(0, 0, c);             // 0

        // Top left of curve
        relatives.add(-2.49, -2.49, c);    //  1
        // Bottom left of curve
        relatives.add(-2.49, 0.8, c);      //  2
        // Bottom right of curve
        relatives.add(2.49, 0.8, c);       //  3
        // Top right of curve
        relatives.add(2.49, -2.49, c);     //  4

        // Top right
        relatives.add(2.49, -5, c);        //  5
        // Top left
        relatives.add(-2.49, -5, c);       //  6

        relatives.add(0, -5, c); // Input Origin (index 7)

  //      relatives.add();

        return relatives;
    }

    @Override
    public Vector getOriginToInputOrigin() {
        return new Vector(0, -5);
    }

    @Override
    public InputWing getRelativeMainInputWing() {
        return new LineInputWing(new CircuitPoint(-2, -5, c), new CircuitPoint(2, -5, c));
    }

    @Override
    public InputWing getInputWing(CircuitPoint relativeStart, CircuitPoint relativeControl, CircuitPoint relativeEnd) {
        return new LineInputWing(relativeStart.getRotated(origin, rotation), relativeEnd.getRotated(origin, rotation));
    }

    @Override
    public void determinePowerStateOf(OutputNode outputNode) {
        if (outputNode.getPowerStatus() == Powerable.PowerStatus.UNDETERMINED) {
            LinkedList<InputNode> relevants = getRelevantInputNodesFor(outputNode);
            if (relevants.size() == 0)
                outputNode.setPowerStatus(Powerable.PowerStatus.PARTIALLY_DEPENDENT);
            if (outputNode.getPowerStatus() == Powerable.PowerStatus.UNDETERMINED) {
                outputNode.setPowerStatus(Powerable.PowerStatus.ON);
                for (InputNode n : getRelevantInputNodesFor(outputNode)) {
                    if (n.getTruePowerValue() == Powerable.PowerStatus.OFF) {
                        outputNode.setPowerStatus(Powerable.PowerStatus.OFF);
                        return;
                    }
                }
            }
        }
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
        return new GateAND(origin.clone(onto), rotation, size, numInputs, out.isNegated(), new ArrayList<>(inNots));
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

        // Curve 7, 1, 2, 0, 3, 4, 8
        BezierCurve curve = new BezierCurve(ps.get(1), ps.get(2), ps.get(3), ps.get(4));
        curve.draw(g, col, getLineWidth());

        // Line 8 to 5
        g.strokeLine(p4.x, p4.y, p5.x, p5.y);
        // Line 5 to 6
        g.strokeLine(p5.x, p5.y, p6.x, p6.y);
        // Line 6 to 1
        g.strokeLine(p6.x, p6.y, p1.x, p1.y);

        drawWings(g, col);

        for (ConnectionNode node : connections)
            node.draw(g, col, opacity);
    }




}
