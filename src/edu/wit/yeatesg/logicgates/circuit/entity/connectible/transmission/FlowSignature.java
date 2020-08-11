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
        System.out.println("SIGN " + d);
        int indexOfD = signed.indexOf(d);
        if (indexOfD != -1) {
            Circuit c = signed.get(0).getCircuit();
            c.clearDependencyTrees();
            System.out.println("OSCILLATION APPARENT: AFFECTED NODES: ");
            for (int i = indexOfD; i < signed.size(); i++) {
                Dependent dependent = signed.get(i);
                System.out.println("  " + dependent);
                DependencyTree.createDependencyTree(new FlowSignature(true), dependent, c);
            }
            ArrayList<DependencyTree> circuitTrees = new ArrayList<>(c.getDependencyTrees());
            c.clearDependencyTrees();
            circuitTrees.forEach(tree -> tree.setPowerValue(PowerValue.SELF_DEPENDENT));
            circuitTrees.forEach(DependencyTree::poll);

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
