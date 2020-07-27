package edu.wit.yeatesg.logicgates.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.EntityList;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Comparator;

public interface OutputNegatable {

    /** Should be a direct reference to the output nodes in the Entity */
    ArrayList<OutputNode> getOutputList();

    /** Should be a direct reference */
    ArrayList<Integer> getNegatedOutputIndices();

    /** Should be a direct reference */
    ArrayList<Integer> getOldNegatedOutputIndices();

    Vector getNegateVectorFor(OutputNode inNode);

    void addInterceptPointForNegate(CircuitPoint location);

    void setCanOutputNegateMoveWires(boolean b);

    boolean getCanOutputNegateMoveWires();

    default boolean negateOutput(CircuitPoint location) {
        ArrayList<OutputNode> outputList = getOutputList();
        for (int i = 0; i < outputList.size(); i++) {
            if (outputList.get(i).getLocation().isSimilar(location)) {
                negateOutput(i);
                return true;
            }
        }
        return false;
    }

    /**
     * MAKE SURE NEGATED INPUT INDICES LIST IS SORTED ASCENDINGLY BY NUMBER DURING CONSTRUCT
     * @param negating
     */
    default void negateOutput(OutputNode negating) {
        negateOutput(getOutputList().indexOf(negating));
    }

    default void negateOutput(int index) {
        ArrayList<Integer> negatedIndicies = getNegatedOutputIndices();
        if (negatedIndicies.contains(index))
            negatedIndicies.remove((Object) index);
        else
            negatedIndicies.add(index);
        setCanOutputNegateMoveWires(true);
        if (this instanceof Entity) {
            ((Entity) this).reconstruct();
        }
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
        ArrayList<Integer> oldNegatedOutputIndices = getOldNegatedOutputIndices();
        for (int i = 0; i < outputNodes.size(); i++) {
            if (negatedOutputIndices.contains(i)) {
                OutputNode negating = outputNodes.get(i);
                Vector negateDirection = getNegateVectorFor(negating);
                CircuitPoint newLocation = negating.getLocation().getIfModifiedBy(negateDirection);
                EntityList<Wire> wiresInNegateDir = negating.getLocation().getInterceptingEntities().getWiresGoingInSameDirection(Vector.getGeneralDirection(negateDirection));
                if (getCanOutputNegateMoveWires() && !wiresInNegateDir.isEmpty()) {
                    for (Wire w : wiresInNegateDir) {
                        if (w.isEdgePoint(negating.getLocation()) && w.intercepts(newLocation)) {
                            w.set(negating.getLocation(), newLocation);
                            getOldNegatedOutputIndices().add(i);
                            break;
                        }
                    }
                }
                negating.setLocation(negating.getLocation().getIfModifiedBy(negateDirection));
                negating.setVectorToParent(negateDirection.getMultiplied(-1));
                negating.negate();
                addInterceptPointForNegate(newLocation);
            }
            else if (oldNegatedOutputIndices.contains(i)) {
                OutputNode noLongerNegated = outputNodes.get(i);
                Vector negateDirection = getNegateVectorFor(noLongerNegated);
                CircuitPoint oldLocation = noLongerNegated.getLocation().getIfModifiedBy(negateDirection);
                oldLocation.getCircuit().addEntity(new Wire(oldLocation, noLongerNegated.getLocation()));
                oldNegatedOutputIndices.remove((Object) i);
            }
        }
        setCanOutputNegateMoveWires(false);
    }

}
