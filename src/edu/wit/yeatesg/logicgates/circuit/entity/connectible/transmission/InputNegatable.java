package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.EntityList;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;
import java.util.Comparator;

public interface InputNegatable {

    /** Should be a direct reference to the input nodes in the Entity */
    ArrayList<InputNode> getInputList();

    /** Should be a direct reference */
    ArrayList<Integer> getNegatedInputIndices();

    /** Should be a direct reference */
    ArrayList<Integer> getOldNegatedInputIndices();

    Vector getNegateVectorFor(InputNode inNode);

    void addInterceptPointForNegate(CircuitPoint location);

    void setCanInputNegateMoveWires(boolean b);

    boolean getCanInputNegateMoveWires();

    default boolean negateInput(CircuitPoint location) {
        ArrayList<InputNode> inputList = getInputList();
        for (int i = 0; i < inputList.size(); i++) {
            if (inputList.get(i).getLocation().isSimilar(location)) {
                negateInput(i);
                return true;
            }
        }
        return false;
    }

    /**
     * MAKE SURE NEGATED INPUT INDICES LIST IS SORTED ASCENDINGLY BY NUMBER DURING CONSTRUCT
     * @param negating
     */
    default void negateInput(InputNode negating) {
        negateInput(getInputList().indexOf(negating));
    }

    default void negateInput(int index) {
        ArrayList<Integer> negatedIndicies = getNegatedInputIndices();
        if (negatedIndicies.contains(index))
            negatedIndicies.remove((Object) index);
        else
            negatedIndicies.add(index);
        setCanInputNegateMoveWires(true);
        if (this instanceof Entity)
            ((Entity) this).reconstruct();
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
        ArrayList<Integer> oldNegatedInputIndices = getOldNegatedInputIndices();
        for (int i = 0; i < inputNodes.size(); i++) {
            if (negatedInputIndices.contains(i)) { // If this InputNode is now being negated
                InputNode negating = inputNodes.get(i);
                Vector negateDirection = getNegateVectorFor(negating);
                CircuitPoint oldLoc = negating.getLocation();
                CircuitPoint newLocation = oldLoc.getIfModifiedBy(negateDirection);
                EntityList<Wire> wiresInNegateDir = oldLoc.getInterceptingEntities()
                        .getWiresGoingInSameDirection(Vector.getGeneralDirection(negateDirection));
                if (getCanInputNegateMoveWires() && !wiresInNegateDir.isEmpty()) {
                    for (Wire w : wiresInNegateDir) {
                        if (!w.isSelected() && w.isEdgePoint(negating.getLocation()) && w.intercepts(newLocation)) {
                            w.set(negating.getLocation(), newLocation);
                            oldNegatedInputIndices.add(i);
                            break;
                        }
                    }
                }
                negating.setLocation(negating.getLocation().getIfModifiedBy(negateDirection));
                negating.setVectorToParent(negateDirection.getMultiplied(-1));
                negating.negate();
                addInterceptPointForNegate(newLocation.getSimilar());
            }
            else if (oldNegatedInputIndices.contains(i)) {
                InputNode noLongerNegated = inputNodes.get(i);
                Vector negateDirection = getNegateVectorFor(noLongerNegated);
                CircuitPoint oldLocation = noLongerNegated.getLocation().getIfModifiedBy(negateDirection);
                oldLocation.getCircuit().addEntity(new Wire(oldLocation, noLongerNegated.getLocation()));
                oldNegatedInputIndices.remove((Object) i);
            }
        }
        setCanInputNegateMoveWires(false);
    }

}
