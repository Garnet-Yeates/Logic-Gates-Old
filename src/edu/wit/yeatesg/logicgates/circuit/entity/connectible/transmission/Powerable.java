package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import edu.wit.yeatesg.logicgates.datatypes.Map;

import java.util.ArrayList;
import java.util.Random;

public interface Powerable {
    DependencyTree getDependencyTree();
    void setDependencyTree(DependencyTree tree);
    Circuit getCircuit();
    void setMarked(boolean marked);
    boolean isMarked();
    boolean isNegated();
    PowerValue getPowerValue();
    void setPowerValue(PowerValue val);


 /*   static void updateTreesInSeries(ArrayList<DependencyTree> initialTrees, Map<ConnectibleEntity, FlowSignature> polling) {
        if (polling == null)
            polling = new Map<>();
        if (initialTrees != null) {
            for (DependencyTree tree : initialTrees)
                polling.addAll(tree.determinePowerStatus());
        }
        if (!polling.isEmpty()) {
            for (Map.MapEntry<ConnectibleEntity, FlowSignature> entry : polling.getEntries()) {
                ConnectibleEntity connectibleEntity = entry.getKey();
                FlowSignature signature = entry.getValue();
                for (DependencyTree tree : connectibleEntity.pollOutputs(signature))
                    updateTreesInSeries(null, tree.determinePowerStatus());
            }
        }
    }*/


    static void updateTreesByLevel(ArrayList<DependencyTree> initialTrees) {
        boolean print = false;
        if (initialTrees.size() > 0)
            print = true;

        if (print)
            System.out.println("UPDATE TREES BY LEVEL (" + initialTrees.size() + " trees) (" + new Random().nextInt() + ")");
        Map<ConnectibleEntity, FlowSignature> currPolling = new Map<>();
        Map<ConnectibleEntity, FlowSignature> nextPolling = new Map<>();
        int treeLevel = 0;
        while (!currPolling.isEmpty() || initialTrees != null) {
            nextPolling.clear();
            if (initialTrees != null) {
                if (print)
                    System.out.println("  TREE LEVEL " + treeLevel++);
                for (DependencyTree tree : initialTrees)
                    nextPolling.addAll(tree.determinePowerStatus());
                initialTrees = null;
            }
            boolean updatedATree = false;
            for (Map.MapEntry<ConnectibleEntity, FlowSignature> entry : currPolling.getEntries()) {
                ConnectibleEntity connectibleEntity = entry.getKey();
                FlowSignature signature = entry.getValue();
                for (DependencyTree tree : connectibleEntity.pollOutputs(signature)) {
                    nextPolling.addAll(tree.determinePowerStatus());
                    updatedATree = true;
                }
            }
            if (updatedATree && print)
                System.out.println("  TREE LEVEL " + treeLevel++);
            currPolling = new Map<>(nextPolling);

        }
    }

    default void updateTreesAroundMe() {
        updateTreesByLevel(getTreesAroundMe());
    }

    static void updateTreesAroundMultiple(ArrayList<Powerable> roots) {
        updateTreesByLevel(getTreesAroundMultiple(roots));
    }

    default ArrayList<DependencyTree> getTreesAroundMe() {
        return getTreesAroundMe(new FlowSignature());
    }

    default ArrayList<DependencyTree> getTreesAroundMe(FlowSignature signature) {
        Circuit c = getCircuit();
        ArrayList<OutputNode> outs = new ArrayList<>();
        ArrayList<InputNode> ins = new ArrayList<>();
        resetPowerValues(outs, ins);
        boolean debug = false;
        if (debug) {
               System.out.println("INS:");
               ins.forEach(inputNode -> System.out.println(" " + inputNode));
               System.out.println("OUTS:");
               outs.forEach(outputNode -> System.out.println(" " + outputNode));
        }
        ArrayList<DependencyTree> createdTrees = new ArrayList<>();
        outs.forEach(out -> DependencyTree.createDependencyTree(signature, out, c, createdTrees));
        ins.forEach(in -> DependencyTree.createDependencyTree(signature, in, c, createdTrees));
        createdTrees.forEach(DependencyTree::disconnectPowerables); // We are done creating trees, so un mark all powerables
        return createdTrees;
    }


    static ArrayList<DependencyTree> getTreesAroundMultiple(ArrayList<Powerable> roots) {
        if (roots.isEmpty())
            return new ArrayList<>();
        Circuit c = roots.get(0).getCircuit();
        ArrayList<OutputNode> outs = new ArrayList<>();
        ArrayList<InputNode> ins = new ArrayList<>();
        Powerable.resetPowerValues(roots, outs, ins);
        ArrayList<DependencyTree> createdTrees = new ArrayList<>();
        outs.forEach(out -> DependencyTree.createDependencyTree(new FlowSignature(), out, c, createdTrees));
        ins.forEach(in -> DependencyTree.createDependencyTree(new FlowSignature(), in, c, createdTrees));
        createdTrees.forEach(DependencyTree::disconnectPowerables); // We are done creating trees, so un mark all powerables
        return createdTrees;
    }


    /**
     * Main method to reset power values, used by both of the other reset power values methods. This method
     * examines the root to check if it is marked or not. If it is marked, the method ends.
     * If it is not marked, it will mark it and add it to the ins or outs list if it is an {@link InputNode} or
     * {@link OutputNode} then recursively call the method on all {@link Powerable}s that exist at the connectible
     * locations of the root. Since this method marks entities to tell whether or not it has already called on it,
     * the methods that use this method will clear the marked dependents of the circuit before and after this method
     * is used.
     * @param root the {@link Powerable} object that this was called on
     * @param outs you can supply a list when you initially call this method and it will add any Output node whose
     *             power was reset
     * @param ins  same thing as with outs
     */
    private static void setPowerValues(Powerable root, PowerValue val,
                                       ArrayList<OutputNode> outs, ArrayList<InputNode> ins,
                                       boolean dontSetOuts, boolean dontSetIns) {
        if (root.isMarked())
            return;
        root.mark();

        boolean setting = true;
        if (root instanceof InputNode && ins != null) {
            ins.add((InputNode) root);
            if (dontSetIns)
                setting = false;
        }
        if (root instanceof OutputNode && outs != null) {
            outs.add((OutputNode) root);
            if (dontSetOuts)
                setting = false;
        }
        if (setting) {
            if (val == PowerValue.UNDETERMINED)
                root.resetPowerValue();
            else
                root.setPowerValue(val);
        }
        ArrayList<Powerable> nextCallPows = new ArrayList<>();
        root.getConnectibleLocations().forEach(circuitPoint -> nextCallPows.addAll(getPowerablesAt(circuitPoint)));
        nextCallPows.forEach(powerable -> setPowerValues(powerable, val, outs, ins, dontSetOuts, dontSetIns));
    }

    static void setPowerValuesAround(Powerable root, PowerValue value, boolean dontSetOuts, boolean dontSetIns) {
        setPowerValues(root, value, new ArrayList<>(), new ArrayList<>(), dontSetOuts, dontSetIns);
    }

    /**
     * Resets multiple power values for multiple roots at once. It is overall more efficient because it
     * @param roots
     * @param outs
     * @param ins
     */
    private static void resetPowerValues(ArrayList<Powerable> roots, ArrayList<OutputNode> outs, ArrayList<InputNode> ins) {
        if (roots.isEmpty())
            return;
        Circuit c = roots.get(0).getCircuit();
        c.clearMarkedPowerables();
        for (Powerable d : roots)
            Powerable.setPowerValues(d, PowerValue.UNDETERMINED, outs, ins, false, false);
        c.clearMarkedPowerables();
    }


    private void resetPowerValues(ArrayList<OutputNode> outs, ArrayList<InputNode> ins) {
        Circuit c = this.getCircuit();
        c.clearMarkedPowerables();
        Powerable.setPowerValues(this, PowerValue.UNDETERMINED, outs, ins, false, false);
        c.clearMarkedPowerables();
    }

    static ArrayList<Powerable> getPowerablesAt(CircuitPoint cp) {
        ArrayList<Powerable> pows = new ArrayList<>();
        for (Entity e : cp.getInterceptingEntities()) {
            if (e instanceof Wire)
                pows.add((Wire) e);
            else if (e instanceof ConnectibleEntity)
                for (ConnectionNode n : ((ConnectibleEntity) e).getConnections())
                    if (n.getLocation().isSimilar(cp))
                        pows.add(n);
        }
        return pows;
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
