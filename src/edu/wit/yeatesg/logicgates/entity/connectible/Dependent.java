package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Circuit;
import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public interface Dependent {

    enum State {
        ON(Color.rgb(50, 199, 0, 1)),
        OFF(Color.rgb(34, 99, 0, 1)),
        PARTIALLY_DEPENDENT(Color.rgb(34, 205, 205, 1)),
        NO_DEPENDENT(Color.rgb(0, 67, 169, 1)),
        ILLOGICAL(Color.rgb(162, 0, 10, 1));

        private Color color;

        State(Color col) {
            color = col;
        }

        public Color getColor() {
            return color;
        }
    }

    static LinkedList<Dependent> getDependentEntities(Circuit c) {
        LinkedList<Dependent> dependents = new LinkedList<>();
        for (ConnectibleEntity ce : c.getAllEntitiesOfType(ConnectibleEntity.class)) {
            if (ce instanceof Dependent)
                dependents.add((Dependent) ce);
            dependents.addAll(ce.getInputNodes());
            dependents.addAll(ce.getOutputNodes());
        }
        return dependents;
    }

    static void resetDependencies(Circuit c) {
        for (Dependent d : getDependentEntities(c)) {
            d.clearDependencies();
        }
    }

    static void resetStates(Circuit c) {
        for (Dependent d : getDependentEntities(c))
            d.setState(null);
    }

    static void calculateDependencies(Circuit c) {
        for (ConnectibleEntity ce : c.getAllEntitiesOfType(ConnectibleEntity.class))
            ce.calculateDependedBy();
    }

    static void determinePowerStates(Circuit c) {
        for (Dependent d : getDependentEntities(c))
            d.determinePowerState();
    }

    /**
     * Should be called AFTER updateDependencies on the Circuit
     * @param c
     */
    static void calculateSuperDependencies(Circuit c) {
        for (Dependent d : getDependentEntities(c))
            d.getDependencyList().calculateSuperDependencies();
    }

    static void illogicalCheck(Circuit c) {
        for (Dependent d : getDependentEntities(c))
            d.illogicalCheck();
    }

    DependentParentList getDependencyList();
    void setState(State state);
    State getState();

    default void clearDependencies() {
        getDependencyList().clear();
    }

    default int getNumDependencies() {
        return getDependencyList().size();
    }

    default boolean hasDependencies() {
        return getDependencyList().size() > 0;
    }

    default boolean hasPartialDependencies() {
        return getDependencyList().size() > 0;
    }

    default boolean hasSuperDependencies() {
        return getDependencyList().getSuperDependencies().size() > 0;
    }

    default void determinePowerState() {
        assureDependentsPowerDetermined();
        if (getState() != State.ILLOGICAL) {
            setState(State.OFF);
            if (this instanceof OutputNode)
                ((OutputNode) this).parent.determinePowerStateOf((OutputNode) this);
            else if (hasSuperDependencies())
                setState(getDependencyList().get(0).getState());
            else if (hasDependencies())
                setState(State.PARTIALLY_DEPENDENT);
            else
                setState(State.NO_DEPENDENT);
        }
    }

    default void assureDependentsPowerDetermined() {
        for (Dependent d : getDependencyList())
            if (d.getState() == null)
                d.determinePowerState();
    }

    /**
     * Should be called ALL dependencies are calculated for the circuit
     */
    default void illogicalCheck() {
        if ((this instanceof InputNode || this instanceof Wire) && getNumDependencies() > 1) {
            setState(State.ILLOGICAL);
            for (Dependent outputNode : getDependencyList())
                outputNode.setState(State.ILLOGICAL);
        }
        else if (getDependencyList().getSuperDependencies().isCircular())
            setState(State.ILLOGICAL);
    }

    class DependentParentList extends LinkedList<Dependent> {

        private Dependent reference;

        public DependentParentList(Dependent representing) {
            reference = representing;
        }

        private SuperDependencyList superDependencies;

        public SuperDependencyList getSuperDependencies() {
            if (superDependencies == null)
                calculateSuperDependencies();
            return superDependencies;
        }

        @Override
        public boolean add(Dependent dep) {
            if (reference instanceof InputNode && dep instanceof InputNode)
                throw new RuntimeException("Invalid Dependency");
            if (reference instanceof OutputNode && dep instanceof  OutputNode)
                throw new RuntimeException("Invalid Dependency");
            if (contains(dep))
                return false;
            return super.add(dep);
        }

        @Override
        public boolean addAll(Collection<? extends Dependent> c) {
            c.forEach(outNode -> {
                if (!contains(outNode))
                    add(outNode);
            });
            return true;
        }

        @Override
        public void clear() {
            if (reference instanceof OutputNode)
                return;
            super.clear();
        }

        /*

        // INPUT NODES OR WIRES WIth more than one dependency should be invalidated
        // WHEN DOING DEPENDENCYCHECK AKA SCANNING ALL OUTPUT NODES, GOING DOWN THE PATH, AND FINDING STUFF
        // THAT THEY DEPEND ON, WHEN YOU HIT A WIRE, ADD THE OUT NODE TO ITS DEPENDENCY LIST. WHEN YOU HIT
        // AN INPUT NODE, ADD THAT TO THE DEPENDENCY LIST AS WELL. THEN, GO TO THE PARENT OF THE INPUT NODE
        // AND ADD THE OUT TO THE DEPENDENCY LIST OF THE PARENT AS WELL. PARENT ENTITIES AKA LOGIC GATES
        // AND STUFF LIKE THAT WILL OBVIOUSLY HAVE MULTIPLE DEPENDENCIES, BUT THAT IS OKAY AND REQUIRED


        // DEPENDENT INTERFACE
        //   IMPLEMENTED BY WIRE AND INPUTNODE
        //   'GETDEPENDENCYLIST  METHOD' FOR A REFERENCE TO A FIELD THAT REPRESENTS A DEPENDENCYLIST OF THE IMPLEMENTER
        //   DependencyList Class
        //      recursive method getSuperDependencyList(SuperDependencyList addingTo, List<OutputNode> alreadyChecked)
        //        method to return a super dependency list. This method will look at all of the OutputNodes in the DependencyList,
        //        get the reference to their parent. If the parent is independent (no input nodes), add it to the 'addingTo' list.
        //        else, loop through the input nodes of that parent, and call getSuperDependencyList()
        //        on the dependencyList with the same 'addingTo' and the same 'alreadyChecked'. If we hit an InputNode that was
        //        already checked, set circular to true and return.

        // FOR GETTING SUPER DEPENDENCIES OF A DEPENDENCYLIST

*/

        private void calculateSuperDependencies() {
            superDependencies = new SuperDependencyList();
            calculateSuperDependencies(superDependencies, new LinkedList<>(), new IgnoreMap(), 0);
        }


        private static class IgnoreMap extends HashMap<Dependent, Integer> {

            public void put(Dependent d) {
                if (!containsKey(d))
                    put(d, 0);
                else
                    put(d, get(d) + 1);
            }

            // Returns true if invalid
            public boolean ignorePoll(Dependent d) {
                put(d, get(d) - 1);
                return get(d) == -1;
            }

        }


        // Dependent depend on dependents. Input nodes depend on output nodes which depend on input nodes etc.
        // A dependent is considered a super dependent if it is an OutputNode and has no dependency. The dependency
        // of OutputNodes are pre determined
        private void calculateSuperDependencies(SuperDependencyList addingTo,
                                                LinkedList<Dependent> cantRepeat,
                                                IgnoreMap numTillInvalid,
                                                int callNum) {
            cantRepeat.add(reference);
            for (Dependent dep : this) {
                if (cantRepeat.contains(dep)) {
                    addingTo.isCircular = true;
                    return;
                }
                else if (dep instanceof OutputNode && dep.getDependencyList().isEmpty()) {
                    addingTo.add((OutputNode) dep);
                }
                else {
                    cantRepeat = new LinkedList<>(cantRepeat); // <-- CLONE IS VERY NECESSARY. If 2 different input nodes for a logic gate share a dependency (this is allowed), obviously one sub call will happen at a time, in1, then in2, so if we dont clone the list and the in1 sub-call states that we can't repeat the output node, it shld NOT make it so in2 cant repeat the output node as well!
                    dep.getDependencyList().calculateSuperDependencies(addingTo, cantRepeat, numTillInvalid, callNum);
                    if (addingTo.isCircular)
                        return;
                }
            }
        }

        static class SuperDependencyList extends LinkedList<OutputNode> {

            private boolean isCircular;

            @Override
            public boolean add(OutputNode outputNode) {
                if (!outputNode.isIndependent())
                    throw new RuntimeException(outputNode + "'s parent is not an Independent entity");
                return super.add(outputNode);
            }

            public boolean isCircular() {
                return isCircular;
            }

            @Override
            public boolean addAll(Collection<? extends OutputNode> c) {
                throw new UnsupportedOperationException("Cannot be called on SuperDependencyList");
            }
/*
            public boolean addAll(SuperDependencyList other) {
                if (other.isCircular)
                    isCircular = true;
                for (OutputNode out : other)
                    add(out);
                return true;
            }*/
        }
    }



}
