package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;

import java.util.ArrayList;

public class FlowSignature {
    private ArrayList<Dependent> signed = new ArrayList<>();
    private boolean errorOrigin;

    public FlowSignature() {
        this(false);
    }

    public FlowSignature(boolean errorOrigin) {
        this.errorOrigin = errorOrigin;
    }

    public ArrayList<Dependent> getSigned() {
        return new ArrayList<>(signed);
    }

    public boolean addSignature(Dependent d) {
        int indexOfD = signed.indexOf(d);
        int numOccurrences = 0;
        if (indexOfD != -1) {
            for (Dependent dep : signed)
                if (dep == d)
                    numOccurrences++;
        }
        if (numOccurrences > 0) {
            Circuit c = signed.get(0).getCircuit();
            ArrayList<DependencyTree> createdTrees = new ArrayList<>();
            for (int i = signed.lastIndexOf(d), o = 1; i < signed.size(); i++, o++) {
                Dependent dependent = signed.get(i);
                dependent.setOscillationIndex(o);
                DependencyTree.createDependencyTree(new FlowSignature(true), dependent, c, createdTrees);
            }
            createdTrees.forEach(tree -> tree.setPowerValue(PowerValue.SELF_DEPENDENT));
            createdTrees.forEach(DependencyTree::disablePowerDetermining); // Very important. This is necessary so the parallel tree update (2 lines below) doesn't cancel out the SELF_DEPENDENT power status set. Since these trees are on a new flow signature of error origin they will automatically know not to update further trees that have SELF_DEPENDENT power status. But if we determine these trees to have regular status 2 lines below, that update wave will never be able to stop because it wont hit any self dependent trees!
            createdTrees.forEach(DependencyTree::disconnectDependents); // We are done creating trees, so un mark all dependents
            Dependent.updateTreesInParallel(createdTrees);
            return false;
        }

        signed.add(d);
        return true;
    }

    public FlowSignature copy() {
        FlowSignature clone = new FlowSignature();
        clone.signed = new ArrayList<>(signed);
        clone.errorOrigin = errorOrigin;
        return clone;
    }

    public boolean isErrorOrigin() {
        return errorOrigin;
    }
}
