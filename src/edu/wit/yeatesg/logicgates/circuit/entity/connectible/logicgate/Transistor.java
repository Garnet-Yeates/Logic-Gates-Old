package edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.Property;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyList;
import edu.wit.yeatesg.logicgates.circuit.entity.Rotatable;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectionList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.*;
import edu.wit.yeatesg.logicgates.datatypes.*;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Transistor extends ConnectibleEntity implements Rotatable {


    private CircuitPoint origin;
    private int rotation;


    /**
     * ConnectibleEntity constructor template:
     * set main fields such as rotation, origin, etc based on the params in the constructor
     * set {@link #connections} to a new ConnectionList
     * call construct()
     *
     * @param origin the Circuit
     */
    public Transistor(CircuitPoint origin, int rotation, boolean nType) {
        super(origin.getCircuit());
        this.origin = origin.getSimilar();
        this.rotation = rotation;
        this.isNType = nType;
        construct();
    }

    public Transistor(CircuitPoint origin, int rotation) {
        this(origin, rotation, false);
    }

    public Transistor(CircuitPoint origin) {
        this(origin, 0);
    }

    public static Transistor parse(String s, Circuit c) {
        String[] fields = s.split(",");
        return new Transistor(new CircuitPoint(fields[0], fields[1], c), Integer.parseInt(fields[2]), Boolean.parseBoolean(fields[3]));
    }

    private InputNode base;  // 0
    private InputNode collector; // 9
    private OutputNode emitter; // 8

    @Override
    public void construct() {
        drawPoints = getRelativePointSet().applyToOrigin(origin, rotation);
        connections = new ConnectionList(this);
        establishInputNode(origin);
        establishInputNode(drawPoints.get(9));
        establishOutputNode(drawPoints.get(8));
        collector = (InputNode) getNodeAt(drawPoints.get(9));
        base = (InputNode) getNodeAt(drawPoints.get(0));
        emitter = (OutputNode) getNodeAt(drawPoints.get(8));


        emitter.setOutputType(OutputType.ANY);
        assignOutputsToInputs();

        interceptPoints = new CircuitPointList();
        for (int i = 13; i <= 17; i++)
            interceptPoints.add(drawPoints.get(i));
        interceptPoints.add(collector.getLocation());
        interceptPoints.add(base.getLocation());
        interceptPoints.add(emitter.getLocation());
        boundingBox = new BoundingBox(interceptPoints, this);

    }

    @Override
    protected void assignOutputsToInputs() {
        emitter.assignToInput(collector);
        emitter.assignToInput(base);
    }

    double negCircleRadius = 0.35;

    @Override
    public RelativePointSet getRelativePointSet() {
        double distToBar = 1;
        Rotatable.RelativePointSet ps = new Rotatable.RelativePointSet();
        ps.add(0, 0, c); //     0 Origin                              (Base Input)      0
        ps.add(distToBar, 0, c); //   1 Line center
        ps.add(1.1, 1.1, c); //   2 Tail of bot arrow
        ps.add(distToBar, 1.45, c);//  3 Line top
        ps.add(1.95, 1.95, c); //   4 Head of bot arrow
        ps.add(1.1, -1.1, c); //  5 Tail of top slant line
        ps.add(distToBar, -1.5, c);// 6 Line bot
        ps.add(1.95, -1.95, c); //  7 Head of top slant line;
        ps.add(2, 3, c); //   8 bot down from arrow                   (Output/Emitter)  8
        ps.add(2, -3, c); //  9 top of top straight line (tail of 12) (Collector Input) 9
        ps.add(1.8, 1.8, c); //  10 arrow mid ish
        ps.add(2, 2.05, c); //  11 arrow mid ish
        ps.add(2, -2.05, c); // 12 bot of top straight line (head of 9)

        ps.add(1, 0, c); // From 13
        ps.add(1, -1, c);
        ps.add(2, -2, c);
        ps.add(1, 1, c);
        ps.add(2, 2, c); // to 17 is int points not including node locs

        ps.add(distToBar - negCircleRadius, 0, c); // neg center

        return ps;
    }

    private boolean isNType;

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        g.setStroke(col == null ? Color.BLACK : col);
        g.setLineWidth(getLineWidth());
        CircuitPointList dps = drawPoints;
        PanelDrawPoint origin = dps.get(0).toPanelDrawPoint();
        PanelDrawPoint lineCenter = dps.get(1).toPanelDrawPoint();
        PanelDrawPoint arrowTail = dps.get(2).toPanelDrawPoint();
        PanelDrawPoint lineTop = dps.get(3).toPanelDrawPoint();
        PanelDrawPoint arrowHead = dps.get(4).toPanelDrawPoint();
        PanelDrawPoint topSlantTail = dps.get(5).toPanelDrawPoint();
        PanelDrawPoint lineBot = dps.get(6).toPanelDrawPoint();
        PanelDrawPoint topSlantHead = dps.get(7).toPanelDrawPoint();
        PanelDrawPoint botOut = dps.get(8).toPanelDrawPoint();
        PanelDrawPoint topIn = dps.get(9).toPanelDrawPoint();
        PanelDrawPoint arrowMiddleIsh = dps.get(10).toPanelDrawPoint();
        PanelDrawPoint botOutTop = dps.get(11).toPanelDrawPoint();
        PanelDrawPoint topInHead = dps.get(12).toPanelDrawPoint();

        EditorPanel.drawArrow(g, col, 0.25, c.getLineWidth(), arrowTail.toCircuitPoint(), arrowMiddleIsh.toCircuitPoint());

        g.setStroke(col == null ? Color.BLACK : col);
        if (!isNType)
            g.strokeLine(origin.x, origin.y, lineCenter.x, lineCenter.y);
        g.strokeLine(lineTop.x, lineTop.y, lineBot.x, lineBot.y);
        g.strokeLine(topSlantHead.x, topSlantHead.y, topSlantTail.x, topSlantTail.y);
        g.strokeLine(botOutTop.x, botOutTop.y, botOut.x, botOut.y);
        g.strokeLine(topInHead.x, topInHead.y, topIn.x, topIn.y);
        g.strokeLine(arrowMiddleIsh.x, arrowMiddleIsh.y, arrowHead.x, arrowHead.y);

        if (isNType) {
            CircuitPoint negCenter = drawPoints.get(18);
            ConnectionNode.drawNegationCircle(g, col == null ? Color.BLACK : col, negCenter, negCircleRadius*2);
        }

        collector.draw(g, col, 1);
        base.draw(g, col, 1);
        emitter.draw(g, col, 1);
    }

    @Override
    public void connect(ConnectibleEntity e, CircuitPoint atLocation) {
        if (!canConnectTo(e, atLocation))
            throw new RuntimeException("Cannot connect these 2 entities");
        if (!hasNodeAt(atLocation))
            throw new RuntimeException("Can't connect to LogicGate here, no ConnectionNode at this CircuitPoint");
        getNodeAt(atLocation).setConnectedTo(e);
        e.getConnections().add(new ConnectionNode(atLocation, e, this));
    }

    @Override
    public boolean isPullableLocation(CircuitPoint gridSnap) {
        return hasNodeAt(gridSnap);
    }

    @Override
    public boolean canCreateWireFrom(CircuitPoint locationOnThisEntity) {
        return hasNodeAt(locationOnThisEntity) && getNodeAt(locationOnThisEntity).getConnectedTo() == null;
    }

    @Override
    public void disconnect(ConnectibleEntity e) {
        getConnectionTo(e).setConnectedTo(null);
    }

    @Override
    public boolean canConnectTo(ConnectibleEntity e, CircuitPoint at) {
        return e instanceof Wire
                && canConnectToGeneral(e)
                && hasNodeAt(at)
                && (getNumEntitiesConnectedAt(at) == 0);
    }

    @Override
    protected void connectCheck(ConnectibleEntity e) {
        for (CircuitPoint nodeLoc : connections.getEmptyConnectionLocations())
            if (canConnectTo(e, nodeLoc) && e.canConnectTo(this, nodeLoc))
                connect(e, nodeLoc);
    }

    @Override
    public PowerValue getLocalPowerStateOf(OutputNode root) {
        PowerValue collector = this.collector.getPowerValue();
        PowerValue base = this.base.getPowerValue();
        return (isNType == base.isOn()) ? PowerValue.FLOATING : collector;
    }

    @Override
    public boolean isSimilar(Entity other) {
        return other instanceof Transistor
                && ((Transistor) other).origin.isSimilar(origin);
    }

    @Override
    public Transistor getCloned(Circuit onto) {
        return new Transistor(origin, rotation, isNType);
    }

    private BoundingBox boundingBox;

    @Override
    public BoundingBox getBoundingBox() {
        return boundingBox.clone();
    }

    @Override
    public CircuitPointList getInvalidInterceptPoints(Entity e) {
        if (e instanceof Wire)
            return e.getInvalidInterceptPoints(this);
        return getInterceptPoints(e); // If it's not a wire, any intersect point is invalid
    }

    @Override
    public String getDisplayName() {
        return "Transistor";
    }

    @Override
    public void move(Vector v) {
        origin = origin.getIfModifiedBy(v);
        reconstruct();
    }

    public boolean doesGenWireInvalidlyInterceptThis(Wire.TheoreticalWire theo, PermitList permits, boolean strictWithWires) {
        permits = new PermitList(permits);
        if (theo.invalidlyIntercepts(this))
            return true;
        else
            for (CircuitPoint p : theo.getInterceptPoints(this))
                if (!permits.contains(new InterceptPermit(this, p)))
                    return true;
        return false;
    }

    @Override
    public String toParsableString() {
        // [class name] <origin.x>,<origin.y>,<rotation>
        return "[" + getClass().getSimpleName() + "]" +
                origin.x + ","
                + origin.y + ","
                + rotation + ","
                + isNType;
    }


    @Override
    public String getPropertyTableHeader() {
        return "Properties for Transistor";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this, c);
        propList.add(new Property("Facing", Direction.cardinalFromRotation(rotation), "NORTH", "SOUTH", "EAST", "WEST"));
        return propList;
    }

    @Override
    public void onPropertyChangeViaTable(String propName, String old, String newVal) {
        if (isTemplateEntity())
            onPropertyChange(propName, old, newVal);
        else
            c.new PropertyChangeOperation(this, propName, newVal, true).operate();

    }

    @Override
    public void onPropertyChange(String propertyName, String old, String newVal) {
        if (propertyName.equalsIgnoreCase("facing")) {
            rotation = Direction.rotationFromCardinal(newVal);
            reconstruct();
        }
    }

    @Override
    public String getPropertyValue(String propertyName) {
        if (propertyName.equalsIgnoreCase("facing"))
            return Direction.cardinalFromRotation(rotation);
        return null;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyName.equalsIgnoreCase("facing");
    }
}