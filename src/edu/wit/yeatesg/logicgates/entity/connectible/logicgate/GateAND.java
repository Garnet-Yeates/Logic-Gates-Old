package edu.wit.yeatesg.logicgates.entity.connectible.logicgate;


import edu.wit.yeatesg.logicgates.def.BezierCurve;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
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
    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs, ArrayList<Integer> nots) {
        super(origin, rotation, size, numInputs, nots);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size, int numInputs) {
        super(origin, rotation, size, numInputs, null);
    }

    public GateAND(CircuitPoint origin, int rotation, Size size) {
        super(origin, rotation, size, getNumBaseInputs(size));
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
        ArrayList<Integer> nots = new ArrayList<>();
        String[] notsString = fields[5].split(";");
        for (String str : notsString)
            nots.add(Integer.parseInt(str));
        return new GateAND(origin, rotation, size, numInputs, nots);
    }

    @Override
    protected int getInputOriginIndex() {
        return 7;
    }

    @Override
    public RelativePointSet getRelativePointSet() {
        RelativePointSet relatives = new RelativePointSet();
        Circuit c = getCircuit();
        // Origin (middle bot of curve (u shaped curve))
        relatives.add(0, 0, c);

        // Top left of curve
        relatives.add(-2.49, -2.49, c);
        // Bottom left of curve
        relatives.add(-2.49, 0.8, c);
        // Bottom right of curve
        relatives.add(2.49, 0.8, c);
        // Top right of curve
        relatives.add(2.49, -2.49, c);

        // Top right
        relatives.add(2.49, -5, c);
        // Top left
        relatives.add(-2.49, -5, c);

        relatives.add(0, -5, c); // Input Origin (index 7)

  //      relatives.add();

        return relatives;
    }

    @Override
    public void determinePowerStateOf(OutputNode outputNode) {
        if (outputNode.getPowerStatus() == Dependent.PowerStatus.UNDETERMINED) {
            LinkedList<InputNode> relevants = getRelevantInputNodesFor(outputNode);
            if (relevants.size() == 0)
                outputNode.setPowerStatus(Dependent.PowerStatus.PARTIALLY_DEPENDENT);
            if (outputNode.getPowerStatus() == Dependent.PowerStatus.UNDETERMINED) {
                outputNode.setPowerStatus(Dependent.PowerStatus.ON);
                for (InputNode n : getRelevantInputNodesFor(outputNode)) {
                    if (n.getPowerStatus() == Dependent.PowerStatus.OFF) {
                        outputNode.setPowerStatus(Dependent.PowerStatus.OFF);
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
    public GateAND clone(Circuit onto) {
        return new GateAND(origin.clone(onto), rotation, size, numInputs, nots);
    }

    @Override
    public String getDisplayName() {
        return "AND Gate";
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        Color strokeCol = col == null ? Color.BLACK : col;
        strokeCol = Color.rgb((int) (255*strokeCol.getRed()), (int) (255*strokeCol.getGreen()), (int) (255*strokeCol.getBlue()), opacity);
        g.setStroke(strokeCol);
        g.setLineWidth(getLineWidth());
        PointSet ps = drawPoints;

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
