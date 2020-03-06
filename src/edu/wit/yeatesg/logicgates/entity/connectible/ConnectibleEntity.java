package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.LogicGates;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class ConnectibleEntity extends Entity {

    protected ConnectionList connections;

    public ConnectibleEntity(Circuit c, boolean isPreview) {
        super(c, isPreview);
        connections = new ConnectionList();
    }



    // State stuff. All connectible entities will have a state, even entities that don't have output nodes.
    // an example of this is an output block, because it doesn't have an output node but still needs to have a state
    // to display output

    protected State state;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    private boolean powerStateDetermined = false;

    public final void resetPowerState() {
        powerStateDetermined = false;
        state = null;
    }

    public boolean isPowerStateDetermined() {
        return powerStateDetermined;
    }

    /**
     * MAKE SURE YOU CALL SUPER FIRST WHEN OVERRIDING THIS
     */
    public void determinePowerState() {
        if (!powerStateDetermined)
            for (ConnectibleEntity dependingOn : dependencies.keySet())
                if (!dependingOn.powerStateDetermined)
                    dependingOn.determinePowerState();
    }



    // Specific Output Entities (entities that can send power)

    public void establishOutputNode(CircuitPoint location) {
        connections.add(new OutputNode(location, this));
    }

    public ArrayList<OutputNode> getOutputNodes() {
        return connections.getOutputNodes();
    }

    public boolean hasOutputNodes() {
        return connections.hasOutputNodes();
    }

    public boolean isIndependent() {
        return (hasOutputNodes() && !hasInputNodes());
    }

    public void calculateDependedBy() {
        for (ConnectionNode node : connections)
            if (node instanceof OutputNode)
                ((OutputNode) node).calculateDependedBy();
    }



    // Input Entities (entities that receive power)
    // Input entities depend on output entities. Input nodes depend on output nodes

    public void establishInputNode(CircuitPoint location) {
        connections.add(new InputNode(location, this));
    }

    public ArrayList<InputNode> getInputNodes() {
        return connections.getInputNodes();
    }

    public boolean hasInputNodes() {
        return connections.hasInputNodes();
    }

    protected HashMap<ConnectibleEntity, LinkedList<Wire>> dependencies = new HashMap<>();

    public HashMap<ConnectibleEntity, LinkedList<Wire>> getDependencies() {
        return dependencies;
    }

    public SuperDependencyCache superDependenciesCache = null;

    public void resetSuperdependencyCache() {
        superDependenciesCache = null;
    }

    public LinkedList<ConnectibleEntity> getSuperDependencies() {
        if (superDependenciesCache == null) {
            LinkedList<ConnectibleEntity> roots = new LinkedList<>();
            roots.add(this);
            superDependenciesCache = getSuperDependencies(new SuperDependencyCache(), roots);
        }
        return superDependenciesCache.isCircular ? null : superDependenciesCache;
    }

    private SuperDependencyCache getSuperDependencies(SuperDependencyCache superDependencies, List<ConnectibleEntity> roots) {
        for (ConnectibleEntity dependsOn : dependencies.keySet()) {
            if (roots.contains(dependsOn)) {
                superDependencies.isCircular = true;
            }
            if (superDependencies.isCircular) // dont merge with the above if, needs to be checked because sub calls
                return superDependencies;
            else if (dependsOn.isIndependent())
                superDependencies.add(dependsOn);
            else {
                roots.add(dependsOn); // All sub method calls share the same roots ref
                dependsOn.getSuperDependencies(superDependencies, roots);
                if (superDependencies.isCircular)
                    return superDependencies; // If ANY path hits ANY of the shared roots, it is circular and therefore illogical
            }
        }
        return superDependencies;
    }



    // General Connecting, Disconnecting, Pulling Wires

    public void establishVolatileNode(CircuitPoint location) {
        connections.add(new ConnectionNode(location, this));
    }

    public abstract void connect(ConnectibleEntity e, CircuitPoint atLocation);

    public abstract boolean canPullConnectionFrom(CircuitPoint locationOnThisEntity);

    public abstract void disconnect(ConnectibleEntity e);

    /**
     * This method shouldn't care about whether any of the entities are preview entities, deleted entities,
     * or invalid intercepting entities. It assumes that any auto-connecting methods in my code will check
     * those variables on their own.
     * @param e the Entity that is potentially going to be connected to this
     * @param at the CircuitPoint location that the connection is at
     * @return true if these entities can connect
     */
    public abstract boolean canConnectTo(ConnectibleEntity e, CircuitPoint at);

    public void disconnectAll() {
        for (ConnectibleEntity e : getConnectedEntities()) {
            disconnect(e);
            e.disconnect(this);
        }
    }

    protected abstract void connectCheck(ConnectibleEntity e);

    public void connectCheck() {
        if (isInvalid() || isPreview || isDeleted())
            return;
        disconnectAll();
        for (ConnectibleEntity e : c.getAllEntitiesOfType(ConnectibleEntity.class)
                .thatAreNotDeleted()
                .except(this))
            connectCheck(e);
        c.refreshTransmissions();
        c.getEditorPanel().repaint();
    }

    // Power flow check -> goes from all user inputs / powers / grounds... basically any absolute beginning of
    // a wire, and flows to the logic gates/power processors. When it hits them, it determines if they should
    // transmit power (based on back-tracking to the nearest  , then calls power flow check on the output wire. A


    public ArrayList<ConnectionNode> getConnections() {
        return connections;
    }

    public void clearConnectionNodes() {
        connections.clear();
    }

    public static void checkEntities(CircuitPoint... checking) {
        Circuit c = checking[0].getCircuit();
        for (CircuitPoint edge : checking) {
            for (ConnectibleEntity ce :  c.getAllEntities()  // When a wire is shortened/deleted, we need
                            .thatIntercept(edge)             // to check every connectible entity that intercepts
                            .ofType(ConnectibleEntity.class) // the pullPoint and release pointbecause they may now be
                            .thatAreNotDeleted()) {          // disconnected, or connected in some cases
                ce.connectCheck();
            }
        }
    }

    // Redundant ConnectionList methods

    public int getNumEntitiesConnectedAt(CircuitPoint location) {
        return connections.getNumEntitiesConnectedAt(location);
    }

    public int getNumWiresConnectedAt(CircuitPoint location) {
        return getWiresConnectedAt(location).size();
    }

    public ArrayList<Wire> getWiresConnectedAt(CircuitPoint location) {
        ArrayList<Wire> list = new ArrayList<>();
        for (Entity e : getEntitiesConnectedAt(location))
            if (e instanceof Wire)
                list.add((Wire) e);
        return list;
    }

    public ArrayList<ConnectibleEntity> getConnectedEntities() {
        return connections.getConnectedEntities();
    }

    public ArrayList<ConnectionNode> getFullConnections() {
        return connections.getFullConnections();
    }

    public ArrayList<ConnectionNode> getEmptyConnections() {
        return connections.getEmptyConnections();
    }

    public ArrayList<CircuitPoint> getFullConnectionLocations() {
        return connections.getFullConnectionLocations();
    }

    public ArrayList<CircuitPoint> getEmptyConnetionLocations() {
        return connections.getEmptyConnectionLocations();
    }

    public ArrayList<ConnectibleEntity> getEntitiesConnectedAt(CircuitPoint location) {
        return connections.getEntitiesConnectedAt(location);
    }

    public ArrayList<ConnectionNode> getNodesAt(CircuitPoint location) {
        return connections.getNodesAt(location);
    }

    public ConnectionNode getNodeAt(CircuitPoint locationOnThisEntity) {
        return connections.getNodeAt(locationOnThisEntity);
    }

    public boolean hasNodeAt(CircuitPoint locationOnThisEntity) {
        return connections.hasNodeAt(locationOnThisEntity);
    }

    public ConnectibleEntity getEntityConnectedAt(CircuitPoint locationOnThisEntity) {
        return connections.getEntityConnectedAt(locationOnThisEntity);
    }

    public CircuitPoint getConnectionLocationOf(ConnectibleEntity potentiallyConnected) {
        return connections.getConnectionLocationOf(potentiallyConnected);
    }

    public ConnectionNode getConnectionTo(ConnectibleEntity potentiallyConnectedEntity) {
        return connections.getConnectionTo(potentiallyConnectedEntity);
    }

    public boolean hasConnectionTo(ConnectibleEntity potentiallyConnectedEntity) {
        return connections.hasConnectionTo(potentiallyConnectedEntity);
    }

    static class SuperDependencyCache extends LinkedList<ConnectibleEntity> {
        boolean isCircular;
    }
}
