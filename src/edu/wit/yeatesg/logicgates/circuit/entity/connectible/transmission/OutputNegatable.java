package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.EntityList;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;
import java.util.Comparator;

public interface OutputNegatable {

    /** Should be a direct reference to the Output nodes in the Entity */
    ArrayList<OutputNode> getOutputList();

    /** Should be a direct reference */
    ArrayList<Integer> getNegatedOutputIndices();

    Vector getNonNegateToNegateUnitVectorFor(OutputNode inNode);

    void addInterceptPointForNegate(CircuitPoint location);

    default void negateOutput(int index) {
        if (index < 0 || index >= getOutputList().size())
            throw new IndexOutOfBoundsException();
        ArrayList<Integer> negatedIndicies = getNegatedOutputIndices();
        if (negatedIndicies.contains(index))
            negatedIndicies.remove((Object) index); // We want it to be casted to object so we use remove(Object o) not remove(int index)
        else
            negatedIndicies.add(index);
        if (this instanceof Entity)
            ((Entity) this).reconstruct();
    }

    default boolean negateOutput(CircuitPoint location) {
        int outputIndex;
        if ((outputIndex = indexOfOutput(location)) != -1) {
            negateOutput(outputIndex);
            return true;
        }
        return false;
    }

    default int indexOfOutput(OutputNode output) {
        return getOutputList().indexOf(output);
    }

    default int indexOfOutput(CircuitPoint location) {
        ArrayList<OutputNode> outputs = getOutputList();
        for (int i = 0; i < outputs.size(); i++) {
            if (outputs.get(i).getLocation().isSimilar(location))
                return i;
        }
        return -1;
    }

    /**
     * MAKE SURE NEGATED INPUT INDICES LIST IS SORTED ASCENDINGLY BY NUMBER DURING CONSTRUCT
     * @param negating
     */
    default void negateOutput(OutputNode negating) {
        negateOutput(getOutputList().indexOf(negating));
    }

    /**
     * Must be the first call in the construct() method of the implementingentity
     */
    default void preOutputConstruct() {
        getNegatedOutputIndices().sort(Comparator.comparingInt(Integer::intValue));
    }

    /**
     * Must be called right after construct, REMEMEMBER THIS
     */
    default void postOutputConstruct(int rotation) {
        ArrayList<OutputNode> outputNodes = getOutputList();
        outputNodes.sort(rotation == 0 || rotation == 180 ? ConnectionNode.getHorizontalComparator() : ConnectionNode.getVerticalComparator());
        ArrayList<Integer> negatedOutputIndices = getNegatedOutputIndices();
        for (int i = 0; i < outputNodes.size(); i++) {
            if (negatedOutputIndices.contains(i)) { // If this OutputNode is now being negated
                OutputNode negating = outputNodes.get(i);
                Vector negateDirection = getNonNegateToNegateUnitVectorFor(negating);
                CircuitPoint oldLoc = negating.getLocation();
                CircuitPoint newLocation = oldLoc.getIfModifiedBy(negateDirection);
                negating.setLocation(negating.getLocation().getIfModifiedBy(negateDirection));
                negating.setVectorToParent(negateDirection.getMultiplied(-1));
                negating.negate();
                addInterceptPointForNegate(newLocation.getSimilar());
            }

        }
    }

}
