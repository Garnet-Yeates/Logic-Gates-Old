package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;

public interface Dependent {
    DependencyTree getDependencyTree();
    void setDependencyTree(DependencyTree tree);
    Circuit getCircuit();
    void setMarked(boolean marked);
    boolean isMarked();
    boolean isNegated();

    default PowerValue getPowerValueFromTree() {
        return hasDependencyTree() ? getDependencyTree().getPowerValue() : PowerValue.UNDETERMINED;
    }

    default void mark() {
        getCircuit().mark(this);
    }

    default boolean hasDependencyTree() {
        return getDependencyTree() != null;
    }

}
