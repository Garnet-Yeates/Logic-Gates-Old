package edu.wit.yeatesg.logicgates.circuit.entity.connectible;

import edu.wit.yeatesg.logicgates.datatypes.Direction;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.ConnectionNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.InputNode;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;
import java.util.Collection;

public class ConnectionList extends ArrayList<ConnectionNode> {

    public boolean hasInputNodes() {
        return getInputNodes().size() > 0;
    }

    public boolean hasOutputNodes() {
        return getOutputNodes().size() > 0;
    }

    private ConnectibleEntity parent;

    public ConnectionList(ConnectibleEntity parent) {
        super();
        this.parent = parent;
    }

    public ConnectionList(Collection<? extends ConnectionNode> collection, ConnectibleEntity parent) {
        super(collection);
        this.parent = parent;
    }

    public ArrayList<InputNode> getInputNodes() {
        ArrayList<InputNode> inputNodes = new ArrayList<>();
        for (ConnectionNode node : this)
            if (node instanceof InputNode)
                inputNodes.add((InputNode) node);
        return inputNodes;
    }

    public ArrayList<OutputNode> getOutputNodes() {
        ArrayList<OutputNode> outputNodes = new ArrayList<>();
        for (ConnectionNode node : this)
            if (node instanceof OutputNode)
                outputNodes.add((OutputNode) node);
        return outputNodes;
    }

    @Override
    public boolean add(ConnectionNode connectionNode) {
        if (hasConnectionTo(connectionNode.getConnectedTo()))
            throw new RuntimeException(parent + " already connected to " + connectionNode.getConnectedTo() + " at " + connectionNode.getLocation());
        return super.add(connectionNode);
    }

    public ArrayList<ConnectionNode> getFullConnections() {
        ArrayList<ConnectionNode> fullConnections = new ArrayList<>(size());
        for (ConnectionNode node : this)
            if (node.hasConnectedEntity())
                fullConnections.add(node);
        return fullConnections;
    }

    public ArrayList<ConnectionNode> getEmptyConnections() {
        ArrayList<ConnectionNode> fullConnections = new ArrayList<>(size());
        for (ConnectionNode node : this)
            if (!node.hasConnectedEntity())
                fullConnections.add(node);
        return fullConnections;
    }

    public ArrayList<CircuitPoint> getEmptyConnectionLocations() {
        ArrayList<ConnectionNode> emptyConnections = getEmptyConnections();
        ArrayList<CircuitPoint> emptyLocs = new ArrayList<>(emptyConnections.size());
        for (ConnectionNode node : emptyConnections)
            emptyLocs.add(node.getLocation());
        return emptyLocs;
    }

    public ArrayList<CircuitPoint> getFullConnectionLocations() {
        ArrayList<ConnectionNode> fullConnections = getFullConnections();
        ArrayList<CircuitPoint> fullLocs = new ArrayList<>(fullConnections.size());
        for (ConnectionNode node : fullConnections)
            if (!fullLocs.contains(node.getLocation()))
                fullLocs.add(node.getLocation());
        return fullLocs;
    }

    public ArrayList<ConnectibleEntity> getConnectedEntities() {
        ArrayList<ConnectibleEntity> connectedEntities = new ArrayList<>();
        for (ConnectionNode c : this)
            if (c.hasConnectedEntity())
                connectedEntities.add(c.getConnectedTo());
        return connectedEntities;
    }

    public ArrayList<ConnectibleEntity> getEntitiesConnectedAt(CircuitPoint locationOnThisEntity) {
        ArrayList<ConnectibleEntity> entities = new ArrayList<>();
        for (ConnectionNode node : getNodesAt(locationOnThisEntity))
            if (node.hasConnectedEntity())
                entities.add(node.getConnectedTo());
        return entities;
    }

    public ArrayList<ConnectionNode> getNodesAt(CircuitPoint locationOnThisEntity) {
        ArrayList<ConnectionNode> nodes = new ArrayList<>();
        for (ConnectionNode node : this.clone())
            if (node.getLocation().equals(locationOnThisEntity))
                nodes.add(node);
        return nodes;
    }

    // Assuming that only one connection can connect at each location
    public ConnectionNode getNodeAt(CircuitPoint locationOnThisEntity) {
        return getNodesAt(locationOnThisEntity).get(0);
    }

    // Assumes that only one entity is connected at this location
    public ConnectibleEntity getEntityConnectedAt(CircuitPoint locationOnThisEntity) {
        return getEntitiesConnectedAt(locationOnThisEntity).get(0);
    }

    public boolean hasConnectionTo(ConnectibleEntity entity) {
        return getConnectionTo(entity) != null;
    }

    public int getNumEntitiesConnectedAt(CircuitPoint location) {
        return getEntitiesConnectedAt(location).size();
    }

    public boolean hasNodeAt(CircuitPoint locationOnThisEntity) {
        return getNodesAt(locationOnThisEntity).size() > 0;
    }

    public ConnectionNode getConnectionTo(ConnectibleEntity potentiallyConnectedTo) {
        for (ConnectionNode node : this)
            if (node.hasConnectedEntity() && node.getConnectedTo() == potentiallyConnectedTo)
                return node;
        return null;
    }

    public CircuitPoint getConnectionLocationOf(ConnectibleEntity potentiallyConnectedTo) {
        ConnectionNode associatedConnection = getConnectionTo(potentiallyConnectedTo);
        return associatedConnection == null ? null : associatedConnection.getLocation();
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof InputNode || o instanceof OutputNode)
            throw new UnsupportedOperationException("Input and Output Nodes cannot be removed from the ConnectionList");
        return super.remove(o);
    }

    /**
     * Sorts the list of connection nodes from lefter to righter so that index 0 is the left/top most and index
     * size - 1 is the right/bottom most
     */
    public void sort(Direction dir) {
        if (dir == Direction.HORIZONTAL)
            sort(ConnectionNode.getHorizontalComparator());
        else
            sort(ConnectionNode.getVerticalComparator());
    }

    /**
     * Obtains a shallow clone of this ConnectionList
     * @return a shallow clone of this ConnectionList
     */
    @Override
    public ConnectionList clone() {
        return new ConnectionList(this, parent);

    }
}