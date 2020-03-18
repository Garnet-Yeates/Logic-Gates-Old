package edu.wit.yeatesg.logicgates.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.LinkedList;

public interface Dependent
{
    /**
     * A Dependent's DependencyList is the list of all of the other Dependents that this Dependent's power
     * status relies on. For example, if you connect a {@link Wire} from an {@link OutputNode} to an
     * {@link InputNode}, that Wire, as well as the InputNode, will now depend on the OutputNode's power status
     * for its power status to be determined. As you can see, the DependencyList for InputNodes and Wires can be modified
     * at the will of the user and this is the basis of how the Circuits in this program are made. For OutputNodes,
     * however, the DependencyList is different. It is calculated internally for whatever type of ConnectibleEntity
     * that it is the child of. For example, an OutputNode that is on an AND gate with 3 inputs will have a pre-set
     * DependencyList containing those 3 inputs and those 3 inputs only. It will stay this way for the remainder of
     * that Entity's lifespan.
     * @return a <b>reference</b> to the DependencyList of this Circuit Object. A reference, not shallow/deep clone is
     * required so that this interface can make direct changes.
     */
    DependencyList dependingOn();

    default void clearDependencies() {
        dependingOn().clear();
    }

    default int getNumDependencies() {
        return dependingOn().size();
    }

    default boolean hasDependencies() {
        return dependingOn().size() > 0;
    }

    default boolean hasSuperDependencies() {
        return dependingOn().getSuperDependencies().size() > 0;
    }

    class DependencyList extends LinkedList<Dependent> {

        private Dependent theDependent;

        public DependencyList(Dependent representing) {
            theDependent = representing;
        }

        private SuperDependencyList superDependencies;

        public SuperDependencyList getSuperDependencies() {
            if (superDependencies == null)
                calculateSuperDependencies();
            return superDependencies;
        }

        @Override
        public boolean add(Dependent dependingOn) {
            if (theDependent instanceof InputNode && dependingOn instanceof InputNode)
                throw new RuntimeException("Invalid Dependency");
            if (theDependent instanceof OutputNode && dependingOn instanceof OutputNode)
                throw new RuntimeException("Invalid Dependency. An OutputNode's power status can only depend on InputNodes");
            if (theDependent instanceof Wire && !(dependingOn instanceof OutputNode))
                throw new RuntimeException("Invalid Dependency. A Wire's power status can only depend on OutputNodes");
            if (contains(dependingOn))
                return false;
            return super.add(dependingOn);
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
            if (theDependent instanceof OutputNode)
                return;
            super.clear();
        }


        private void calculateSuperDependencies() {
            superDependencies = new SuperDependencyList();
            calculateSuperDependencies(superDependencies, new LinkedList<>());
        }


        // Dependent depend on dependents. Input nodes depend on output nodes which depend on input nodes etc.
        // A dependent is considered a super dependent if it is an OutputNode and has no dependency. The dependency
        // of OutputNodes are pre determined
        private void calculateSuperDependencies(SuperDependencyList addingTo, LinkedList<Dependent> cantRepeat) {
            cantRepeat.add(theDependent);
            for (Dependent dep : this) {
                if (cantRepeat.contains(dep)) {
                    addingTo.isCircular = true;
                    return;
                }
                else if (dep instanceof OutputNode && dep.dependingOn().isEmpty()) {
                    addingTo.add((OutputNode) dep);
                }
                else {
                    cantRepeat = new LinkedList<>(cantRepeat); // <-- CLONE IS VERY NECESSARY. If 2 different input nodes for a logic gate share a dependency (this is allowed), obviously one sub call will happen at a time, in1, then in2, so if we dont clone the list and the in1 sub-call states that we can't repeat the output node, it shld NOT make it so in2 cant repeat the output node as well!
                    dep.dependingOn().calculateSuperDependencies(addingTo, cantRepeat);
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
        }
    }

    /**
     * Anything that implements the Dependent interface is something that has a {@link PowerStatus} that can be set.
     * Each PowerStatus enumeration is associated with a Color so that the PowerStatus of each Dependent can be
     * displayed. Classes that implement this interface should have a field to represent this PowerStatus, and they
     * should implement this method to update said field.
     * @param status the new PowerStatus for this Dependent
     */
    void setPowerStatus(PowerStatus status);

    /**
     * Obtains the {@link PowerStatus} of this {@link Dependent}. Classes that implement this interface should have
     * a field representing the PowerStatus. This method, along with {@link #setPowerStatus(PowerStatus)} should
     * be the getter and setters for said field so that this interface can make modifications to that object's
     * power status.
     * @return the enumeration value for this Dependent's PowerStatus
     */
    PowerStatus getPowerStatus();

    /**
     * Enumeration used to represent the PowerStatus of this Dependent instance. For the {@link InputNode} and
     * {@link Wire} classes, their PowerStatus will always be computed to be the PowerStatus of whatever they depend
     * on. For example if a Wire is connected from an OutNode to an InNode, the Wire and the InNode will both depend
     * on the OutNode. So when their PowerStatus is determined, it will simply look at whatever the connected OutNode's
     * power is and choose that. For OutputNodes it works different; their dependencies are set-in-stone for whatever
     * type of ConnectibleEntity they are the child of. They use specific types of logic for determining their power
     * status, such as AND, OR, XOR logic, based on the status of the InputNodes they depend on. Sometimes OutputNodes
     * don't even have dependencies at all, and their power status is based on a user-chosen state (such as the
     * OutputNodes on an InputBlock)
     */
    enum PowerStatus
    {
        /** Used to display the 'true', '1', 'on', 'powered', 'active', etc power status */
        ON(Color.rgb(50, 199, 0, 1)),

        /** Used to display the 'false', '0', 'off', etc power status */
        OFF(Color.rgb(34, 99, 0, 1)),

        /** Used to show that this Dependent instance has a dependency, but not a super dependency */
        PARTIALLY_DEPENDENT(Color.rgb(35, 147, 169, 1)),

        /** Used to show that this Dependent instance has no partial or super dependencies */
        NO_DEPENDENT(Color.rgb(0, 67, 169, 1)),

        /**
         * Used to show that this Dependent instance is illogical; it either has multiple dependencies when it
         * should not (such as for Wires or InputNodes), or that it depends on itself in some way (meaning that
         * it was wired together circularly)
         */
        ILLOGICAL(Color.rgb(180, 0, 10, 1)),

        /** Used to flag Dependent instances as not having a determined state yet. When dependencies are calculated,
         * it is done using a recursive method called on each OutputNode as the root of the call. If the OutputNode
         * is not in the undetermined state, then the method will return and do nothing. This comes in handy with the
         * illogical check because illogical entities won't have their state set back to undetermined for the second
         * dependency check, so they essentially won't be checked and wont affect the circuit.
         * @see OutputNode#calculateDependedBy()
         * @see #illogicalCheck()
         * @see #resetDependencies(Circuit)
         * */
        UNDETERMINED(Color.rgb(125, 125, 125, 1));

        private Color color;

        PowerStatus(Color col) {
            color = col;
        }

        public Color getColor() {
            return color;
        }
    }

    default void determinePowerStatus() {
        assureDependentsPowerStatusDetermined();
        if (getPowerStatus() != PowerStatus.ILLOGICAL) {
            setPowerStatus(PowerStatus.OFF);
            if (this instanceof OutputNode)
                ((OutputNode) this).parent.determinePowerStateOf((OutputNode) this);
            else if (hasSuperDependencies())
                setPowerStatus(dependingOn().get(0).getPowerStatus());
            else if (hasDependencies())
                setPowerStatus(PowerStatus.PARTIALLY_DEPENDENT);
            else
                setPowerStatus(PowerStatus.NO_DEPENDENT);
        }
    }

    default void assureDependentsPowerStatusDetermined() {
        for (Dependent d : dependingOn())
            if (d.getPowerStatus() == PowerStatus.UNDETERMINED)
                d.determinePowerStatus();
    }


    default void illogicalCheck() {
        if ((this instanceof InputNode || this instanceof Wire) && getNumDependencies() > 1) {
            setPowerStatus(PowerStatus.ILLOGICAL);
            for (Dependent outputNode : dependingOn())
                outputNode.setPowerStatus(PowerStatus.ILLOGICAL);
        }
        else if (dependingOn().getSuperDependencies().isCircular())
            setPowerStatus(PowerStatus.ILLOGICAL);
    }

    static LinkedList<Dependent> getDependents(Circuit c) {
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
        for (Dependent d : getDependents(c)) {
            d.clearDependencies();
        }
    }

    static void resetPowerStatus(Circuit c, boolean resetIllogicals) {
        for (Dependent d : getDependents(c))
            if (resetIllogicals || d.getPowerStatus() != PowerStatus.ILLOGICAL)
                d.setPowerStatus(PowerStatus.UNDETERMINED);
    }

    static void calculateDependencies(Circuit c) {
        for (ConnectibleEntity ce : c.getAllEntitiesOfType(ConnectibleEntity.class))
            ce.calculateDependedBy();
    }

    static void determinePowerStatuses(Circuit c) {
        for (Dependent d : getDependents(c))
            d.determinePowerStatus();
    }

    /**
     * Should be called AFTER updateDependencies on the Circuit
     * @param c
     */
    static void calculateSuperDependencies(Circuit c) {
        for (Dependent d : getDependents(c))
            d.dependingOn().calculateSuperDependencies();
    }

    static void illogicalCheck(Circuit c) {
        for (Dependent d : getDependents(c))
            d.illogicalCheck();
    }

}
