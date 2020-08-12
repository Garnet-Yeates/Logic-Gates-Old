package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;

public interface Dependent {
    DependencyTree getDependencyTree();
    void setDependencyTree(DependencyTree tree);
    Circuit getCircuit();
    void setMarked(boolean marked);
    boolean isMarked();
    boolean isNegated();
    PowerValue getPowerValue();
    void setPowerValue(PowerValue val);

    default void treeUpdate() {
        treeUpdate(getTreesAroundMe(new FlowSignature()));
        System.out.println("UPDATE TREES CALLED ON " + this);
    }

    static void updateTreesInSeries(ArrayList<DependencyTree> trees) {
        if (!trees.isEmpty()) {
            for (DependencyTree tree : trees)
                updateTreesInSeries(tree.determinePowerStatus());
        }
    }

    static void treeUpdate(ArrayList<DependencyTree> trees) {
        //updateTreesInSeries(trees);
        updateTreesByLevel(trees);
    }

    static void updateTreesByLevel(ArrayList<DependencyTree> trees) {
        ArrayList<DependencyTree> currUpdate = trees;
        ArrayList<DependencyTree> nextUpdate = new ArrayList<>();
        int treeLevel = 0;
        while (!currUpdate.isEmpty()) {
            System.out.println("  TREE LEVEL " + treeLevel++);
            nextUpdate.clear();
            currUpdate.forEach(tree -> nextUpdate.addAll(tree.determinePowerStatus()));
            currUpdate = new ArrayList<>(nextUpdate);
        }
    }

    default ArrayList<DependencyTree> getTreesAroundMe(FlowSignature signature) {
        Circuit c = getCircuit();
        ArrayList<OutputNode> outs = new ArrayList<>();
        ArrayList<InputNode> ins = new ArrayList<>();
        resetPowerValues(this, outs, ins);
        boolean debug = false;
        if (debug) {
               System.out.println("INS:");
               ins.forEach(inputNode -> System.out.println(" " + inputNode));
               System.out.println("OUTS:");
               outs.forEach(outputNode -> System.out.println(" " + outputNode));
        }

        ArrayList<DependencyTree> createdTrees = new ArrayList<>();
        ins.forEach(in -> DependencyTree.createDependencyTree(signature, in, c, createdTrees));
        outs.forEach(out -> DependencyTree.createDependencyTree(signature, out, c, createdTrees));
        createdTrees.forEach(DependencyTree::disconnectDependents); // We are done creating trees, so un mark all dependents
        return createdTrees;
    }

    private void resetPowerValues(Dependent root, ArrayList<OutputNode> outs, ArrayList<InputNode> ins) {
        Circuit c = root.getCircuit();
        c.clearMarkedDependents();
        resetPowerValues_(root, outs, ins);
        c.clearMarkedDependents();
    }

    private static void resetPowerValues_(Dependent root, ArrayList<OutputNode> outs, ArrayList<InputNode> ins) {
        if (root.isMarked())
            return;
        root.mark();
        root.resetPowerValue();
        if (root instanceof InputNode)
            ins.add((InputNode) root);
        if (root instanceof OutputNode)
            outs.add((OutputNode) root);
        ArrayList<Dependent> nextCallDeps = new ArrayList<>();
        root.getConnectibleLocations().forEach(circuitPoint -> nextCallDeps.addAll(getDependentsAt(circuitPoint)));
        nextCallDeps.forEach(dependent -> resetPowerValues_(dependent, outs, ins));
    }

    static ArrayList<Dependent> getDependentsAt(CircuitPoint cp) {
        ArrayList<Dependent> deps = new ArrayList<>();
        for (Entity e : cp.getInterceptingEntities()) {
            if (e instanceof Wire)
                deps.add((Wire) e);
            else if (e instanceof ConnectibleEntity)
                for (ConnectionNode n : ((ConnectibleEntity) e).getConnections())
                    if (n.getLocation().isSimilar(cp))
                        deps.add(n);
        }
        return deps;
    }

    default ArrayList<CircuitPoint> getConnectibleLocations() {
        final ArrayList<CircuitPoint> connectibleLocs = new ArrayList<>();
        if (this instanceof Wire)
            connectibleLocs.addAll(((Wire) this).getEdgePoints());
        else if (this instanceof ConnectionNode)
            connectibleLocs.add(((ConnectionNode) this).getLocation());
        return connectibleLocs;
    }

    default void mark() {
        getCircuit().mark(this);
    }

    default boolean hasDependencyTree() {
        return getDependencyTree() != null;
    }


    default ArrayList<DependencyTree> getNextTreesToUpdate(FlowSignature flowSignature) {
        if (!(this instanceof InputNode))
            throw new RuntimeException();
        ConnectibleEntity parent = ((InputNode) this).parent;
        return parent.pollOutputs(flowSignature);
    }

    default void resetPowerValue() {
        setPowerValue(PowerValue.UNDETERMINED);
        setOscillationIndex(-1);
    }

    default boolean isOscillated() {
        return getOscillationIndex() != -1;
}

    int getOscillationIndex();
    void setOscillationIndex(int index);

}
