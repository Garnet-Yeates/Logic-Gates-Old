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

    default void parallelTreeUpdate() {
        updateTreesInParallel(getTrees(new FlowSignature()));
    }

    static void updateTreesInParallel(ArrayList<DependencyTree> trees) {
        trees.forEach(DependencyTree::determinePowerStatus);
        trees.forEach(DependencyTree::poll);
    }

    default ArrayList<DependencyTree> getTrees(FlowSignature signature) {
        Circuit c = getCircuit();
        c.clearDependencyTrees();
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

        ins.forEach(in -> DependencyTree.createDependencyTree(signature, in, c));
        outs.forEach(out -> DependencyTree.createDependencyTree(signature, out, c));
        ArrayList<DependencyTree> circuitTrees = new ArrayList<>(c.getDependencyTrees());
        c.clearDependencyTrees();
        return circuitTrees;
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


    default ArrayList<DependencyTree> pollParent(FlowSignature flowSignature) {
        if (!(this instanceof InputNode))
            throw new RuntimeException();
        ConnectibleEntity parent = ((InputNode) this).parent;
        return parent.poll(flowSignature);
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
