package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class OutputNode extends ConnectionNode {

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public OutputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    private OutputType outType = OutputType.ZERO_ONE;

    public void setOutputType(OutputType type) {
        this.outType = type;
    }

    public OutputType getOutputType() {
        return outType;
    }

    private ArrayList<InputNode> inputsThatAffectMe = new ArrayList<>();

    public void addInputThatAffectsMe(InputNode n) {
        inputsThatAffectMe.add(n);
    }

    public ArrayList<DependencyTree> getTreesIDependOn() {
        ArrayList<DependencyTree> trees = new ArrayList<>();
        for (InputNode n : inputsThatAffectMe)
            if (n.hasDependencyTree())
                trees.add(n.getDependencyTree());
        return trees;
    }

    public ArrayList<PowerValue> getRelevantPowerValuesAffectingMe() {
        ArrayList<PowerValue> powerValues = new ArrayList<>();
        for (InputNode thatAffectsMe : inputsThatAffectMe) {
            PowerValue powerVal = thatAffectsMe.getPowerValueFromTree();
            if (powerVal.isRelevantForCalculations())
                powerValues.add(thatAffectsMe.isNegated ? powerVal.getNegated() : powerVal);
        }
        return powerValues;
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        if (isNegated) {
            CircuitPoint negCenter = location.getIfModifiedBy(getVectorToParent().getMultiplied(0.5));
            ConnectionNode.drawNegationCircle(g, col == null ? Color.BLACK : col, negCenter, 1);
        }
        col = col == null ? getPowerValueFromTree().getColor() : col;
        g.setFill(col);
        double circleSize = parent.getCircuit().getScale() * 0.55;
        circleSize *= getLocation().getCircuit().getScale() < 10 ? 1.1 : 1;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
