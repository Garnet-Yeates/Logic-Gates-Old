package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.entity.connectible.OutputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.canvas.GraphicsContext;

public class LogicGate extends ConnectibleEntity implements Pokable, Rotatable {


    public LogicGate(Circuit c, boolean isPreview) {
        super(c, true);
    }

    @Override
    public ConnectibleEntity clone(Circuit c) {
        return null;
    }

    @Override
    protected void determineOutputDependencies() {

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
    public void determinePowerStateOf(OutputNode outputNode) {

    }

    @Override
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return false;
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
    public int getLineWidth() {
        return 0;
    }

    @Override
    public PointSet getInterceptPointsRef() {
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
    public void draw(GraphicsContext g) {

    }

    @Override
    public String getDisplayName() {
        return "null";
    }

    @Override
    public void onDelete() {

    }

    @Override
    public boolean isSimilar(Entity other) {
        return false;
    }

    @Override
    public Entity getSimilarEntity() {
        return null;
    }

    @Override
    public boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo, PermitList exceptions, boolean strictWithWires) {
        return false;
    }

    @Override
    public String toParsableString() {
        return null;
    }

    @Override
    public boolean canMove() {
        return false;
    }

    @Override
    public String getPropertyTableHeader() {
        return null;
    }

    @Override
    public PropertyList getPropertyList() {
        return null;
    }

    @Override
    public void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1) {

    }

    @Override
    public boolean hasProperty(String propertyName) {
        return false;
    }
}
