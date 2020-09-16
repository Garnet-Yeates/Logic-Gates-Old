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

    public ArrayList<InputNode> getInputsThatAffectMe() {
        return new ArrayList<>(inputsThatAffectMe);
    }

    public void assignToInput(InputNode n) {
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
            PowerValue powerVal = thatAffectsMe.getPowerValue();
            if (powerVal == PowerValue.ACTIVE || powerVal == PowerValue.DONE_ACTIVE)
                powerVal = PowerValue.ON;
            else if (powerVal == PowerValue.INACTIVE)
                powerVal = PowerValue.OFF;
            if (powerVal.isRelevant())
                powerValues.add(thatAffectsMe.isNegated ? powerVal.getNegated() : powerVal);
        }
        return powerValues;
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        draw(g, col, opacity, 1.0);
    }

    private boolean causesHighTriggering = false;

    public boolean causesHighTriggering() {
        return causesHighTriggering;
    }

    public void setCausesHighTriggering(boolean b) {
        causesHighTriggering = b;
    }
}
