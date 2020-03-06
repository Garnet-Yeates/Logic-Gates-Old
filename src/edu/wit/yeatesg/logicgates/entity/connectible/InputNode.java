package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.LogicGates;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.HashMap;
import java.util.LinkedList;

public class InputNode extends ConnectionNode {

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    protected HashMap<OutputNode, LinkedList<Wire>> dependencies = new HashMap<>();

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public HashMap<OutputNode, LinkedList<Wire>> getDependencies() {
        return dependencies;
    }

    // Prob wont need to cache because this is prob only gonna be called once on nodes, during the determine
    // wireIllogicies() method.

    public LinkedList<ConnectibleEntity> getSuperDependencies() {
        LinkedList<ConnectibleEntity> superDependencies = new LinkedList<>();
        for (OutputNode outDependentOn : dependencies.keySet()) {
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



    public void determineIllogical() {
        LinkedList<ConnectibleEntity> superDependencies = getSuperDependencies();
        LogicGates.debug("This InputNode's superDependencies: ", superDependencies);
        if (superDependencies == null || getDependencies().size() > 1)
            setState(State.ILLOGICAL);
        else if (superDependencies.size() > 0)
            setState(State.OFF); // Setting it to OFF state basically flags it as a logical (not illogical) entity
        else if (getDependencies().size() > 0)
            setState(State.PARTIALLY_DEPENDENT);
        else
            setState(State.NO_DEPENDENT);
    }
}
