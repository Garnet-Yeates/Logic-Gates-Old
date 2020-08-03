package edu.wit.yeatesg.logicgates.circuit.entity.connectible;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class ConnectibleEntity extends Entity {

    protected ConnectionList connections;

    /**
     * ConnectibleEntity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * set {@link #connections} to a new ConnectionList
     * call construct()
     *
     * @param c the Circuit
     */
    public ConnectibleEntity(Circuit c) {
        super(c);
    }

    /**
     * MUST be called by every ConnectibleEntity at the end of the construct() method
     */
    public void postInit() {
        assignOutputsToInputs();
    }

    protected abstract void assignOutputsToInputs();

    // Specific To Output Entities (entities that can send power)

    public void establishOutputNode(CircuitPoint location) {
        connections.add(new OutputNode(location, this));
    }

    public ArrayList<OutputNode> getOutputNodes() {
        return connections.getOutputNodes();
    }

    public boolean hasOutputNodes() {
        return connections.hasOutputNodes();
    }

    public boolean transmitsOnly() {
        return (hasOutputNodes() && !hasInputNodes());
    }

    public final void calculateDependedBy() {
        for (ConnectionNode node : connections)
            if (node instanceof OutputNode)
                ((OutputNode) node).calculateDependedBy();
    }


    public void establishInputNode(CircuitPoint location) {
        connections.add(new InputNode(location, this));
    }

    public ArrayList<InputNode> getInputNodes() {
        return connections.getInputNodes();
    }

    public boolean hasInputNodes() {
        return connections.hasInputNodes();
    }


    // General Connecting, Disconnecting, Pulling Wires

    public abstract void connect(ConnectibleEntity e, CircuitPoint atLocation);

    public abstract boolean canCreateWireFrom(CircuitPoint locationOnThisEntity);

    public abstract boolean canPullPointGoHere(CircuitPoint gridSnap);

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
        disconnectAll();
        if (!isInConnectibleState())
            return;
        for (ConnectibleEntity ce : getInterceptingEntities().thatExtend(ConnectibleEntity.class))
            connectCheck(ce);
    }

    public boolean isInConnectibleState() {
        return !isInvalid() && existsInCircuit();
    }

    public boolean canConnectToGeneral(ConnectibleEntity other) {
        return isInConnectibleState() && !isSimilar(other);
    }

    /**
     * Obtains a <i>reference</i> to the connection list of this entity
     * @return a reference to this {@link ConnectibleEntity}'s connection list
     */
    public ConnectionList getConnections() {
        return connections;
    }

    public void clearConnectionNodes() {
        connections.clear();
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

    public abstract void determinePowerStateOf(OutputNode outputNode);
    

    public LinkedList<InputNode> getRelevantInputNodesFor(OutputNode out) {
        LinkedList<InputNode> relevants = new LinkedList<>();
        for (Powerable inputNode : out.dependingOn())
            if (inputNode.hasSuperDependencies())
                relevants.add((InputNode) inputNode);
        return relevants;
    }

    public abstract ConnectibleEntity getCloned(Circuit onto);



}