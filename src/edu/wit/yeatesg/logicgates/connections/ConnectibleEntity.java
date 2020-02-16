package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.Entity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.awt.*;
import java.util.ArrayList;

public abstract class ConnectibleEntity extends Entity {

    protected ConnectionList connections;

    public ConnectibleEntity(CircuitPoint location) {
        super(location);
        connections = new ConnectionList();
    }

    public abstract void connect(ConnectibleEntity e, CircuitPoint atLocation);

    public abstract boolean canPullConnectionFrom(CircuitPoint locationOnThisEntity);

    public abstract void disconnect(ConnectibleEntity e);

    public abstract boolean canConnectTo(ConnectibleEntity e, CircuitPoint at);

    public void disconnectAll() {
        for (ConnectibleEntity e : getConnectedEntities()) {
            disconnect(e);
            e.disconnect(this);
        }
    }


    public abstract void connectCheck(ConnectibleEntity e);

    public void connectCheck() {
        System.out.println("CONNECT CHEK CALLED () ON " + this);
        disconnectAll();
        ArrayList<ConnectibleEntity> connectibles = c.getAllEntitiesOfType(ConnectibleEntity.class);
        connectibles.remove(this);
        for (ConnectibleEntity e : connectibles)
                connectCheck(e);
        c.getEditorPanel().repaint();
    }

    public boolean isPowerSource() {
        return false;
    }

    protected boolean receivedPowerThisUpdate;
    protected boolean powered;

    public static final Color POWER = new Color(50, 199, 0);
    public static final Color NO_POWER = new Color(34, 99, 0);

    public void onPowerReceive() {
        receivedPowerThisUpdate = true;
        powered = true;
    }

    public void resetPower() {
        if (!isPowerSource())
            powered = false;
        receivedPowerThisUpdate = false;
    }

    public Color getColor() {
        return overrideColor != null ? overrideColor : powered ? POWER : NO_POWER;
    }

    private Color overrideColor;
    public void setOverrideColor(Color color) {
        overrideColor = color;
    }

    public boolean isPowered() {
        return powered;
    }

    public boolean hasReceivedPowerThisUpdate() {
        return receivedPowerThisUpdate;
    }

    public ArrayList<ConnectionNode> getConnections() {
        return connections;
    }

    public void establishConnectionNode(CircuitPoint location) {
        connections.add(new ConnectionNode(location, this));
    }

    public void clearConnectionNodes() {
        connections.clear();;
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
}