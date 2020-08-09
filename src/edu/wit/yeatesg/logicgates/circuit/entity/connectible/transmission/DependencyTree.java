package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

public class DependencyTree {

    private Circuit c;

    private ArrayList<OutputNode> roots;
    private ArrayList<Wire> branches;
    private ArrayList<InputNode> leaves;

    public static DependencyTree createDependencyTree(Dependent startingFrom, Circuit c) {
        if (!(startingFrom instanceof OutputNode) && !(startingFrom instanceof InputNode))
            throw new RuntimeException("Can only create from Input/Output nodes");
        if (startingFrom.hasDependencyTree())
            return null;
        return new DependencyTree(startingFrom, c);
    }

    private DependencyTree(Dependent startingFrom, Circuit c) {
        if (!(startingFrom instanceof OutputNode) && !(startingFrom instanceof InputNode))
            throw new RuntimeException("Can only create from Input/Output nodes");
        this.c = c;
        roots = new ArrayList<>();
        branches = new ArrayList<>();
        leaves = new ArrayList<>();
        powerValue = PowerValue.UNDETERMINED;

        c.clearMarkedDependents();
        grow(startingFrom);
        c.clearMarkedDependents();
    }

    private void grow(Dependent start) {
        if (start.isMarked())
            return;
        start.mark();
        add(start);
        if (start instanceof InputNode && ((InputNode) start).getConnectedTo() instanceof Dependent)
            grow((Dependent) ((InputNode) start).getConnectedTo());
        if (start instanceof OutputNode && ((OutputNode) start).getConnectedTo() instanceof Dependent)
            grow((Dependent) ((OutputNode) start).getConnectedTo());
        if (start instanceof Wire) {
            Wire w = (Wire) start;
            ConnectionNode connectionTo;
            for (ConnectibleEntity e : w.getConnectedEntities())
                if (e instanceof Dependent)
                    grow((Dependent) e);
                else if ((connectionTo = e.getConnectionTo(w)) instanceof Dependent)
                    grow((Dependent) connectionTo);
        }
    }

    public void add(Dependent dep) {
        if (!(dep instanceof OutputNode || dep instanceof Wire || dep instanceof InputNode))
            throw new RuntimeException("Dumb");
        if (dep.hasDependencyTree())
            throw new RuntimeException("Cannot add " + dep + " to this tree. It already has a tree");

        dep.setDependencyTree(this);

        if (dep instanceof OutputNode)
            roots.add((OutputNode) dep);
        else if (dep instanceof Wire)
            branches.add((Wire) dep);
        else {
            leaves.add((InputNode) dep); // If dep is an input, its parent now depends on this tree
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * MARK STUFF - TEMPORARILY MARK TREES TO SAY "WE ALREADY ITERATED OVER THIS TREE"
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private boolean isMarked;

    public void setMarked(boolean mark) {
        this.isMarked = mark;
    }

    private void mark() {
        c.markTree(this);
    }

    private boolean isMarked() {
        return isMarked;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * CALCULATE TREES ABOVE ME - RECURSIVELY GAIN REFERENCES TO THE TREES ABOVE THIS ONE. GOES FROM TOP TO BOTTOM
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private ArrayList<DependencyTree> allTreesAboveMe = null;
    private ArrayList<DependencyTree> treesDirectlyAboveMe = null;

    private boolean areTreesAboveMeDetermined = false;

    public void determineTreesAboveMe() {
        allTreesAboveMe = new ArrayList<>();
        treesDirectlyAboveMe = new ArrayList<>();
        mark(); // At the beginning of a call, we mark this Entity. At the end, we set areTreesAboveMeDetermined to true.
        // if we run into a marked entity where areTreesAboveMeDetermined == false it means we went in a circle
        allTreesAboveMe = new ArrayList<>();
        for (OutputNode root : roots) {
            for (DependencyTree directlyAboveMe : root.getTreesIDependOn()) {
                if (!directlyAboveMe.areTreesAboveMeDetermined) {
                    if (directlyAboveMe.isMarked()) {
                        powerValue = PowerValue.SELF_DEPENDENT; // We went in a Circle
                        continue;
                    }
                    directlyAboveMe.determineTreesAboveMe(); // Wont be performed if Circular
                }
                // If it is circular, none of this will be performed
                treesDirectlyAboveMe.add(directlyAboveMe);
                allTreesAboveMe.add(directlyAboveMe);
                allTreesAboveMe.addAll(directlyAboveMe.allTreesAboveMe);
            }
        }
        areTreesAboveMeDetermined = true;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * CALCULATE POWER VALUE BASED ON TREES DIRECTLY ABOVE ME. GOES FROM TOP TO BOTTOM
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

    private PowerValue powerValue;

    public boolean isPowerStatusDetermined() {
        return powerValue != PowerValue.UNDETERMINED;
    }

    public void resetPowerStatus(boolean resetIfCircular) {
        if (powerValue != PowerValue.SELF_DEPENDENT || resetIfCircular)
            powerValue = PowerValue.UNDETERMINED;
    }

    // O = (num roots + num leaves) ^ 2
    public void determinePowerStatus() {
        if (isPowerStatusDetermined())
            return;
        if (powerValue == PowerValue.SELF_DEPENDENT)
            return;

        // Make sure power statuses above me are determined
        for (DependencyTree dirAbove : treesDirectlyAboveMe)
            if (!dirAbove.isPowerStatusDetermined())
                dirAbove.determinePowerStatus();

        // Self dependent check
        for (DependencyTree dirAbove : treesDirectlyAboveMe)
            if (dirAbove.getPowerValue().equals(PowerValue.SELF_DEPENDENT)) {
                powerValue = PowerValue.SELF_DEPENDENT;
                return; // If anything above me is self dependent, I am self dependent
            }

        // Data bit multi dependency check
        if (roots.size() > 1) {
            for (OutputNode root : roots) {
                if (root.getNumBits() > 1) {
                    powerValue = PowerValue.MULTI_MULTI_BIT;
                    return;
                }
            }
        }

        // Data bit compatibility check
        ArrayList<ConnectionNode> rootsAndLeaves = new ArrayList<>(roots);
        rootsAndLeaves.addAll(leaves);
        for (ConnectionNode n : rootsAndLeaves) {
            for (ConnectionNode n2 : rootsAndLeaves) {
                if (n.getNumBits() != n2.getNumBits()) {
                    powerValue = PowerValue.INCOMPATIBLE_BITS;
                    return;
                }
            }
        }

        // Root compatibility check
        if (roots.size() > 1) {
            for (OutputNode root : roots) {
                if (root.getOutputType() == OutputType.ZERO_ONE) {
                    powerValue = PowerValue.INCOMPATIBLE_TYPES;
                    return; // If we have any 0/1 mixed with any other OutputType (0/1, 0/f, f/1) it is invalid
                }
            }
            for (OutputNode root : roots) {
                for (OutputNode root2 : roots) {
                    if (root.getOutputType() != root2.getOutputType()) {
                        powerValue = PowerValue.INCOMPATIBLE_TYPES;
                        return; // We don't have any 0/1's, but they are not all equivalent
                    }
                }
            }
        }

        PowerValue floating = PowerValue.FLOATING;

        LinkedList<PowerValue> possibleValues = new LinkedList<>();
        for (OutputNode root : roots) {
            if (root.getPowerValueFromTree() != PowerValue.UNDETERMINED)
                throw new RuntimeException("fuk up at " + root.getLocation().toParsableString());
            possibleValues.add(root.getParent().getLocalPowerStateOf(root));
        }
        possibleValues.sort(Comparator.comparingInt(PowerValue::getPriority));


        // At this point, we don't have any errors so we can calculate the PowerStatus of this tree!

        powerValue = possibleValues.isEmpty() ? floating : possibleValues.get(possibleValues.size() - 1);
    }

    public PowerValue getPowerValue() {
        return powerValue;
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

    public void disconnectDependents() {
        roots.forEach(out -> out.setDependencyTree(null));
        branches.forEach(wire -> wire.setDependencyTree(null));
        leaves.forEach(in -> in.setDependencyTree(null));

    }
}

