package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.EntityList;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;
import java.util.Comparator;

public interface InputNegatable {

    /** Should be a direct reference to the Input nodes in the Entity */
    ArrayList<InputNode> getInputList();

    /** Should be a direct reference */
    ArrayList<Integer> getNegatedInputIndices();

    Vector getNonNegateToNegateUnitVectorFor(InputNode inNode);

    void addInterceptPointForNegate(CircuitPoint location);

    default void negateInput(int index) {
        if (index < 0 || index >= getInputList().size())
            throw new IndexOutOfBoundsException();
        ArrayList<Integer> negatedIndicies = getNegatedInputIndices();
        if (negatedIndicies.contains(index))
            negatedIndicies.remove((Object) index); // We want it to be casted to object so we use remove(Object o) not remove(int index)
        else
            negatedIndicies.add(index);
        if (this instanceof Entity)
            ((Entity) this).reconstruct();
    }

    default boolean negateInput(CircuitPoint location) {
        int inputIndex;
        if ((inputIndex = indexOfInput(location)) != -1) {
            negateInput(inputIndex);
            return true;
        }
        return false;
    }

    default int indexOfInput(InputNode input) {
        return getInputList().indexOf(input);
    }

    default int indexOfInput(CircuitPoint location) {
        ArrayList<InputNode> inputs = getInputList();
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).getLocation().isSimilar(location))
                return i;
        }
        return -1;
    }

    /**
     * MAKE SURE NEGATED INPUT INDICES LIST IS SORTED ASCENDINGLY BY NUMBER DURING CONSTRUCT
     * @param negating
     */
    default void negateInput(InputNode negating) {
        negateInput(getInputList().indexOf(negating));
    }

    /**
     * Must be the first call in the construct() method of the implementingentity
     */
    default void preInputConstruct() {
        getNegatedInputIndices().sort(Comparator.comparingInt(Integer::intValue));
    }

    /**
     * Must be called right after construct, REMEMEMBER THIS
     */
    default void postInputConstruct(int rotation) {
        ArrayList<InputNode> inputNodes = getInputList();
        inputNodes.sort(rotation == 0 || rotation == 180 ? ConnectionNode.getHorizontalComparator() : ConnectionNode.getVerticalComparator());
        ArrayList<Integer> negatedInputIndices = getNegatedInputIndices();
        for (int i = 0; i < inputNodes.size(); i++) {
            if (negatedInputIndices.contains(i)) { // If this InputNode is now being negated
                InputNode negating = inputNodes.get(i);
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
