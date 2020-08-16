package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.Map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

public class DependencyTree {

    private ArrayList<OutputNode> roots;
    private ArrayList<Wire> branches;
    private ArrayList<InputNode> leaves;

    private FlowSignature signature;

    public static void createDependencyTree(FlowSignature signature, Powerable startingFrom, Circuit c, ArrayList<DependencyTree> addingTo) {
        if (!(startingFrom instanceof OutputNode) && !(startingFrom instanceof InputNode))
            throw new RuntimeException("Can only create from Input/Output nodes");
        if (startingFrom.hasDependencyTree())
            return;
        DependencyTree created = new DependencyTree(signature, startingFrom, c);
        addingTo.add(created);
    }

    private DependencyTree(FlowSignature signature,Powerable startingFrom, Circuit c) {
        if (!(startingFrom instanceof OutputNode) && !(startingFrom instanceof InputNode))
            throw new RuntimeException("Can only create from Input/Output nodes");
        this.signature = signature.copy();
        roots = new ArrayList<>();
        branches = new ArrayList<>();
        leaves = new ArrayList<>();

        c.clearMarkedPowerables();
        grow(startingFrom);
        c.clearMarkedPowerables();
    }

    private void grow(Powerable start) {
        if (start.isMarked())
            return;
        start.mark();
        add(start);
        if (start instanceof InputNode && ((InputNode) start).getConnectedTo() instanceof Powerable)
            grow((Powerable) ((InputNode) start).getConnectedTo());
        if (start instanceof OutputNode && ((OutputNode) start).getConnectedTo() instanceof Powerable)
            grow((Powerable) ((OutputNode) start).getConnectedTo());
        if (start instanceof Wire) {
            Wire w = (Wire) start;
            ConnectionNode connectionTo;
            for (ConnectibleEntity e : w.getConnectedEntities())
                if (e instanceof Powerable)
                    grow((Powerable) e);
                else if ((connectionTo = e.getConnectionTo(w)) != null)
                    grow(connectionTo);
        }
    }

    private boolean causesHighTriggering = false;

    public void add(Powerable pow) {
        if (!(pow instanceof OutputNode || pow instanceof Wire || pow instanceof InputNode))
            throw new RuntimeException("Dumb");
        if (pow.hasDependencyTree())
            throw new RuntimeException("Cannot add " + pow + " to this tree. It already has a tree");

        pow.setDependencyTree(this);

        if (pow instanceof OutputNode) {
            if (((OutputNode) pow).causesHighTriggering())
                causesHighTriggering = true;
            roots.add((OutputNode) pow);
        }
        else if (pow instanceof Wire)
            branches.add((Wire) pow);
        else {
            leaves.add((InputNode) pow); // If pows is an input, its parent now depends on this tree
        }
    }

    public void disconnectPowerables() {
        roots.forEach(out -> out.setDependencyTree(null));
        branches.forEach(wire -> wire.setDependencyTree(null));
        leaves.forEach(in -> in.setDependencyTree(null));
    }

    public FlowSignature getSignature() {
        return signature;
    }

    private boolean canDeterminePowerStatus = true;

    public void disablePowerDetermining() {
        canDeterminePowerStatus = false;
    }

    // O = num roots * num leaves
    /** Returns the entities that have to be polled to get the trees to update next */
    public Map<ConnectibleEntity, FlowSignature> determinePowerStatus() {
        if (!canDeterminePowerStatus) // Very important. When the signature is added to the FlowSignature, it may determine that the power flow has gone in a Circle. If this happens, the power status of all signatures (ins/outs) are set to circular
            return new Map<>();

        for (OutputNode root : roots) {
            for (InputNode leaf : leaves) {
                if (root.getParent().equalsExact(leaf.getParent())) {
                    setPowerValue(PowerValue.SELF_DEPENDENT); // <-- Now 'powerDetermined' is set to true
                    signature = new FlowSignature(true);
                    return getEntitiesToPollNext();
                }
            }
        }
        ArrayList<Powerable> signaturesBeingAdded = new ArrayList<>();
        signaturesBeingAdded.addAll(roots);
        signaturesBeingAdded.addAll(leaves);

        for (Powerable powerable : signaturesBeingAdded)
            if (!signature.addSignature(powerable))
                return new Map<>();

        PowerValue powerValue = PowerValue.UNDETERMINED;
        // Data bit multi dependency check
        if (roots.size() > 1) {
            for (OutputNode root : roots) {
                if (root.getNumBits() > 1) {
                    powerValue = PowerValue.MULTI_MULTI_BIT;
                    break;
                }
            }
        }

        // Data bit compatibility check
        if (powerValue == PowerValue.UNDETERMINED) {
            for (ConnectionNode n : roots) {
                for (ConnectionNode n2 : leaves) {
                    if (n.getNumBits() != n2.getNumBits()) {
                        powerValue = PowerValue.INCOMPATIBLE_BITS;
                        break;
                    }
                }
            }
        }

        // Root compatibility check
        if (roots.size() > 1 && powerValue == PowerValue.UNDETERMINED) {
            for (OutputNode root : roots) {
                if (root.getOutputType() == OutputType.ZERO_ONE) {
                    powerValue = PowerValue.INCOMPATIBLE_TYPES;
                    break; // If we have any 0/1 mixed with any other OutputType (0/1, 0/f, f/1) it is invalid
                }
            }
            if (powerValue == PowerValue.UNDETERMINED) {
                for (OutputNode root : roots) {
                    for (OutputNode root2 : roots) {
                        if (root.getOutputType() != root2.getOutputType()) {
                            powerValue = PowerValue.INCOMPATIBLE_TYPES;
                            break; // We don't have any 0/1's, but they are not all equivalent
                        }
                    }
                }
            }
        }



     /*   for (InputNode in : leaves)
            if (in.getPowerValue() == PowerValue.DONE_ACTIVE)
                in.setPowerValue(PowerValue.INACTIVE);*/ // NOT RLLY NECESSARY, BECAUSE POLL ONLY CARES ABT ACTIVE ANYWAYS. LEAVING THIS COMMENTED MEANS IT LOOKS PRETTIER

        if (powerValue == PowerValue.UNDETERMINED) {
            LinkedList<PowerValue> possibleValues = new LinkedList<>();
            for (OutputNode root : roots)
                possibleValues.add(root.getParent().getLocalPowerStateOf(root));
            possibleValues.sort(Comparator.comparingInt(PowerValue::getPriority));

            powerValue = possibleValues.isEmpty() ? PowerValue.FLOATING : possibleValues.get(possibleValues.size() - 1);
        }

        if (powerValue == PowerValue.ON && causesHighTriggering)
            powerValue = PowerValue.ACTIVE;
        setPowerValue(powerValue);
        return getEntitiesToPollNext();
    }

    public Map<ConnectibleEntity, FlowSignature> getEntitiesToPollNext() {
        Map<ConnectibleEntity, FlowSignature> toPoll = new Map<>();
        leaves.forEach(inputNode -> toPoll.put(inputNode.getParent(), signature));
        return toPoll;
    }

    public void setPowerValue(PowerValue val) {
        roots.forEach(outputNode -> outputNode.setPowerValue(val));
        branches.forEach(wire -> wire.setPowerValue(val));
        leaves.forEach(inputNode -> inputNode.setPowerValue(val));
    }


    public ArrayList<OutputNode> getRoots() {
        return roots;
    }

    public ArrayList<Wire> getBranches() {
        return branches;
    }

    public ArrayList<InputNode> getLeaves() {
        return leaves;
    }
}

