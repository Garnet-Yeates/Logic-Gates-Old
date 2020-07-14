package edu.wit.yeatesg.logicgates.entity.connectible;

import edu.wit.yeatesg.logicgates.def.BezierCurve;
import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Dependent;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.OutputNode;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public abstract class LogicGate extends ConnectibleEntity implements Dependent, Rotatable, PropertyMutable {


    /**
     * ConnectibleEntity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * set {@link #connections} to a new ConnectionList
     * call construct()
     *
     * @param c the Circuit
     */
    public LogicGate(Circuit c, CircuitPoint origin, int rotation) {
        super(c);
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        construct();
    }

    protected Size size;
    protected CircuitPoint origin;
    protected int rotation;
    protected BoundingBox boundingBox;

    /**
     * Should be implemented by sub classes and be different depending on size/numinputs
     */
    public abstract void constructInterceptPoints();

    @Override
    public void construct() {
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        getCircuit().pushIntoMapRange(drawPoints);
        connections = new ConnectionList(this);
        constructInterceptPoints();
        boundingBox = new BoundingBox(interceptPoints, this);
    }

    /**
     *
     * @return
     */
    public abstract RelativePointSet getRelativePointSet();

    public abstract void constructOutputNodes();

    public abstract void constructInputNodes();
/*
    drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
    getCircuit().pushIntoMapRange(drawPoints);
    connections = new ConnectionList();
    interceptPoints = getBoundingBox().getInterceptPoints();
    establishOutputNode(drawPoints.get(0));
    out = (OutputNode) getNodeAt(drawPoints.get(0));
    establishInputNode(drawPoints.get(7));
    establishInputNode(drawPoints.get(8));
    update();
    assignOutputsToInputs();

 */

    @Override
    protected void assignOutputsToInputs() {
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {

    }

    @Override
    public boolean canCreateWireFrom(CircuitPoint locationOnThisEntity) {
        return false;
    }

    @Override
    public boolean canPullPointGoHere(CircuitPoint gridSnap) {
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
    public boolean isSimilar(Entity other) {
        return false;
    }

    @Override
    public ConnectibleEntity clone(Circuit onto) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public void move(Vector v) {

    }

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox.clone();
    }

    @Override
    public PointSet getInvalidInterceptPoints(Entity e) {
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
    public void draw(GraphicsContext g, Color col, double opacity) {

    }

    @Override
    public double getLineWidth() {
        return 0;
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
    public void onPropertyChange(String propertyName, String old, String newVal) {

    }

    @Override
    public String getPropertyValue(String propertyName) {
        return null;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return false;
    }

    @Override
    public DependencyList dependingOn() {
        return null;
    }

    @Override
    public void setPowerStatus(PowerStatus status) {

    }

    @Override
    public PowerStatus getPowerStatus() {
        return null;
    }
}
