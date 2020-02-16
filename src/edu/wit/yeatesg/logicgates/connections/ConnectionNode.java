package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.points.CircuitPoint;

public class ConnectionNode {

   protected CircuitPoint location;
   protected ConnectibleEntity connectedTo;
   protected ConnectibleEntity parent;

   public ConnectionNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
       this.location = location;
       this.connectedTo = connectedTo;
       this.parent = connectingFrom;
   }

   public ConnectionNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
       this(location, connectingFrom, null);
   }

    public CircuitPoint getLocation() {
        return location;
    }

    public void setLocation(CircuitPoint location) {
        this.location = location;
    }

    public ConnectibleEntity getConnectedTo() {
        return connectedTo;
    }

    public boolean hasConnectedEntity() {
       return getConnectedTo() != null;
    }

    public void setConnectedTo(ConnectibleEntity connectedTo) {
        this.connectedTo = connectedTo;
    }

    public ConnectibleEntity getParent() {
        return parent;
    }

    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
       return parent.canConnectTo(e, at);
    }

    public void setParent(ConnectibleEntity parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object other) {
       if (other instanceof  ConnectionNode) {
           ConnectionNode o = (ConnectionNode) other;
           if ((connectedTo == null || o.connectedTo == null) && !(o.connectedTo == null && connectedTo == null))
               return false;
           else return (((ConnectionNode) other).parent.equals(parent)
                   && ((ConnectionNode) other).location.equals(location)
                   && ((ConnectionNode) other).connectedTo.equals(connectedTo));
       }
       return false;
    }
}