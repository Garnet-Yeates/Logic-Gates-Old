package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.*;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.awt.*;

public class LogicGate extends ConnectibleEntity implements Pokable, Rotatable {


    public LogicGate(Circuit c) {
        super(c);
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {

    }

    @Override
    public boolean canPullConnectionFrom(CircuitPoint locationOnThisEntity) {
        return false;
    }

    @Override
    public void disconnect(ConnectibleEntity e) {

    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        return false;
    }

    @Override
    protected void connectCheck(ConnectibleEntity e) {

    }

    @Override
    public void onPoke() {

    }

    @Override
    public Entity getRotated(int rotation) {
        return null;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public RelativePointSet getRelativePointSet() {
        return null;
    }

    @Override
    public int getStrokeSize() {
        return 0;
    }

    @Override
    public PointSet getInterceptPoints() {
        return new PointSet();
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
        return null;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return null;
    }

    @Override
    public boolean canMoveBy(Vector vector) {
        return false;
    }

    @Override
    public void draw(Graphics2D g) {

    }

    @Override
    public void onDelete() {

    }

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo, PermitList exceptions, boolean strictWithWires) {
        return false;
    }
}
