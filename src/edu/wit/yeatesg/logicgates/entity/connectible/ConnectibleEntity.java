package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.LogicGates;
import edu.wit.yeatesg.logicgates.entity.Entity;
import edu.wit.yeatesg.logicgates.entity.PointSet;
import edu.wit.yeatesg.logicgates.entity.PropertyList;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static edu.wit.yeatesg.logicgates.gui.EditorPanel.*;

public abstract class ConnectibleEntity extends Entity {

    protected ConnectionList connections;

    public ConnectibleEntity(Circuit c, boolean addToCircuit) {
        super(c, addToCircuit);
        connections = new ConnectionList();
    }

    @Override
    public void postInit(boolean addToCircuit) {
        determineOutputDependencies();
        if (addToCircuit)  {
            c.addEntity(this);
            connectCheck();
            c.refreshTransmissions();
            if (!c.getCircuitName().contains("theoretical"))
                c.getEditorPanel().repaint(c);
        }
    }

    protected abstract void determineOutputDependencies();


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
        if (isInvalid() || isDeleted())
            return;
        disconnectAll();
        for (Entity e : c.getAllEntities())
            if (e instanceof ConnectibleEntity
                && e.intercepts(this)
                && !e.isDeleted()
                && !e.equals(this))
                    connectCheck((ConnectibleEntity) e);
        c.refreshTransmissions();
        c.getEditorPanel().repaint(c);

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
            for (Entity e : c.getAllEntities())
                if (e instanceof ConnectibleEntity
                    && e.intercepts(edge)
                    && !e.isDeleted())
                        ((ConnectibleEntity) e).connectCheck();
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

    public abstract void determinePowerStateOf(OutputNode outputNode);

    public abstract boolean isPullableLocation(CircuitPoint gridSnap);


    public LinkedList<InputNode> getRelevantInputNodesFor(OutputNode out) {
        LinkedList<InputNode> relevants = new LinkedList<>();
        for (Dependent inputNode : out.getDependencyList())
            if (inputNode.hasSuperDependencies())
                relevants.add((InputNode) inputNode);
        return relevants;
    }

    public abstract ConnectibleEntity clone(Circuit onto);



}
