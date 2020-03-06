package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.LogicGates;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class InputNode extends ConnectionNode {

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    protected LinkedList<OutputNode> dependencies = new LinkedList<>();

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public LinkedList<OutputNode> getDependencies() {
        return dependencies;
    }

    public boolean hasPartialDependencies() {
        return getDependencies().size() > 0;
    }

    // Prob wont need to cache because this is prob only gonna be called once on nodes, during the determine
    // wireIllogicies() method.

    public LinkedList<ConnectibleEntity> getSuperDependencies() {
        LinkedList<ConnectibleEntity> superDependencies = new LinkedList<>();
        for (OutputNode outDependentOn : dependencies) {
            if (outDependentOn.parent.getSuperDependencies() == null)
                return null;
            else if (outDependentOn.parent.isIndependent())
                superDependencies.add(outDependentOn.parent);
            else {
                LinkedList<ConnectibleEntity> parentDepeondsOn = outDependentOn.parent.getSuperDependencies();
                if (parentDepeondsOn == null)
                    return null;
                else
                    superDependencies.addAll(parentDepeondsOn);
            }
        }
        return superDependencies;
    }

    public boolean hasSuperDependencies() {
        return getSuperDependencies().size() > 0;
    }

    public void determineIllogical() {
        if (getSuperDependencies() == null) // If circular
            setState(State.ILLOGICAL);
        if (getDependencies().size() > 1) {
            setState(State.ILLOGICAL);
            for (OutputNode dependsOnOut : getDependencies())
                dependsOnOut.setState(State.ILLOGICAL);
        }
    }

    public void calculateDependencies() {
        if (getState() != State.ILLOGICAL && !hasSuperDependencies()) {
            if (getDependencies().size() > 0)
                setState(State.PARTIALLY_DEPENDENT);
            else
                setState(State.NO_DEPENDENT);
        }
        if (getState() == null)
            setState(State.OFF);
    }

}
