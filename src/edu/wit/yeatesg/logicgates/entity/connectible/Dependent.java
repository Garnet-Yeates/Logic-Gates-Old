package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Circuit;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

public interface Dependent {

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
        if (getDependencyList().getSuperDependencies().isCircular()) {
            setState(State.ILLOGICAL);
            System.out.println(this + " SET TO ILLOGICAL BECAUSE CIRCULAR SUPER");
        }
        else if ((this instanceof InputNode || this instanceof Wire) && getNumDependencies() > 1) {
            setState(State.ILLOGICAL);
            for (Dependent outputNode : getDependencyList())
                outputNode.setState(State.ILLOGICAL);
            System.out.println(this + " SET TO ILLOGICAL BECAUSE MORE THAN 1 DEPEND");
        }
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

        // TO MAKE SUPER DEPENDENCY CHECK MORE EFFICIENT, WE CAN JUST CALL IT ON ALL OF THE INPUT NODES. THEN AFTER
        // WE DO THAT, WE CAN LOOP THROUGH ALL OF THE WIRES THAT ARE CONNECTED TO EACH INPUT NODE AND SET ITS
        // SUPER DEPENDENCIES TO THE SUPER DEPENDENCIES OF THE INPUT NODE. THEN WE GO TO THE PARENTS OF THE INPUT
        // NODE THAT WE JUST CHECKED, AND ADD ALL OF THE SUPER DEPENDENCIES AS WELL. I SHOULD OVERRIDE ADDALL
        // IN DEPENDENCYLIST SO THAT WHEN A CIRCULAR DEPENDENCYLIST IS ADDED TO ANOTHER ONE, THE ONE IT WAS ADDED
        // TO IS SET TO CIRCULAR AS WELL

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
            calculateSuperDependencies(superDependencies, reference, new NumTillInvalidMap());
        }


        private static class NumTillInvalidMap extends HashMap<Dependent, Integer> {

            public void put(Dependent d) {
                if (!containsKey(d))
                    put(d, 1);
                else
                    put(d, get(d) + 1);
            }

            // Returns true if invalid
            public boolean invalidPoll(Dependent d) {
                put(d, get(d) - 1);
                return get(d) == -1;
            }

        }


        // Dependent depend on dependents. Input nodes depend on output nodes which depend on input nodes etc.
        // A dependent is considered a super dependent if it is an OutputNode and has no dependency. The dependency
        // of OutputNodes are pre determined
        private void calculateSuperDependencies(SuperDependencyList addingTo,
                                                    Dependent initialCaller,
                                                    NumTillInvalidMap numTillInvalid) {

            if (initialCaller == null) // Wires are a special case; they have dependencies but nothing will ever
                initialCaller = reference; // technically depend on them, so if they are the initialcaller it will loop
            if (reference instanceof Wire) // forever
                initialCaller = null;
            for (Dependent dep : this) {
                if (dep.equals(initialCaller)) {
                    addingTo.isCircular = true;
                    return;
                }
                else if (dep instanceof OutputNode && dep.getDependencyList().isEmpty()) {
                    addingTo.add((OutputNode) dep);
                }
                else {
                    dep.getDependencyList().calculateSuperDependencies(addingTo, initialCaller, numTillInvalid);
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
