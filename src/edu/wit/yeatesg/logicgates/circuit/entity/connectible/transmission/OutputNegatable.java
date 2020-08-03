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

    /** Should be a direct reference */
    ArrayList<Integer> getOldNegatedOutputIndices();

    Vector getNegateVectorFor(OutputNode inNode);

    void addInterceptPointForNegate(CircuitPoint location);

    void setCanOutputNegateMoveWires(boolean b);

    boolean getCanOutputNegateMoveWires();

    default boolean negateOutput(CircuitPoint location) {
        ArrayList<OutputNode> OutputList = getOutputList();
        for (int i = 0; i < OutputList.size(); i++) {
            if (OutputList.get(i).getLocation().isSimilar(location)) {
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
        if (this instanceof Entity)
            ((Entity) this).reconstruct();
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
        ArrayList<OutputNode> OutputNodes = getOutputList();
        OutputNodes.sort(rotation == 0 || rotation == 180 ? ConnectionNode.getHorizontalComparator() : ConnectionNode.getVerticalComparator());
        ArrayList<Integer> negatedOutputIndices = getNegatedOutputIndices();
        ArrayList<Integer> oldNegatedOutputIndices = getOldNegatedOutputIndices();
        for (int i = 0; i < OutputNodes.size(); i++) {
            if (negatedOutputIndices.contains(i)) { // If this OutputNode is now being negated
                OutputNode negating = OutputNodes.get(i);
                Vector negateDirection = getNegateVectorFor(negating);
                CircuitPoint oldLoc = negating.getLocation();
                CircuitPoint newLocation = oldLoc.getIfModifiedBy(negateDirection);
                EntityList<Wire> wiresInNegateDir = oldLoc.getInterceptingEntities()
                        .getWiresGoingInSameDirection(Vector.getGeneralDirection(negateDirection));
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
                addInterceptPointForNegate(newLocation.getSimilar());
            }
            else if (oldNegatedOutputIndices.contains(i)) {
                OutputNode noLongerNegated = OutputNodes.get(i);
                Vector negateDirection = getNegateVectorFor(noLongerNegated);
                CircuitPoint oldLocation = noLongerNegated.getLocation().getIfModifiedBy(negateDirection);
                oldLocation.getCircuit().addEntity(new Wire(oldLocation, noLongerNegated.getLocation()));
                oldNegatedOutputIndices.remove((Object) i);
            }
        }
        setCanOutputNegateMoveWires(false);
    }

}
