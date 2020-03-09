package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.entity.*;
import edu.wit.yeatesg.logicgates.entity.connectible.*;
import edu.wit.yeatesg.logicgates.gui.EditorPanel;
import edu.wit.yeatesg.logicgates.gui.Project;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.LinkedList;


public class Circuit implements Dynamic {

    public static final Color COL_BG = Color.WHITE;
    public static final Color COL_GRID = Color.DARKGREY;
    public static final Color COL_ORIGIN = Color.RED;

    private Project project;

    public Circuit(Project p, String circuitName) {
        this.circuitName = circuitName;
        this.project = p;
        p.addCircuit(this);
    }

    private String circuitName;

    public String getCircuitName() {
        return circuitName;
    }

    public EditorPanel getEditorPanel() {
        return project.getEditorPanel();
    }

    // Pressing left arrow key on the panel shud shift the origin to the right
    // that means tht nums have to be added to the circuit point to get to the panel point
    // so left means xOff++, for the panel the

    /**
     * CircuitPoint to CircuitDrawPoint
     * Multiply the x and y of a CircuitPoint by this value to get the CircuitDrawPoint
     * Represents the distance between CircuitPoint 0,0 and 0,1 on the editor panel
     * (how many CircuitDrawPoints can fit between two CircuitPoints aka Grid Points?)
     * */
    private int scale = 30;

    private static final int SCALE_MAX = 80;
    private static final int SCALE_MIN = 10;
    private static final int SCALE_INC = 10;

    /**
     * Returns the number that you have to multiply a {@link CircuitPoint} by to get its {@link PanelDrawPoint}
     * @return CircuitPoint * this = PanelDrawPoint
     */
    public double getScale() {
        return scale;
    }

    public boolean canScaleUp() {
        return scale < SCALE_MAX;
    }

    public void scaleUp() {
        scale += canScaleUp() ? SCALE_INC : 0;
    }

    public boolean canScaleDown() {
        return scale > SCALE_MIN;
    }

    public void scaleDown() {
        scale -= canScaleDown() ? SCALE_INC : 0;
    }

    public int getLineWidth() {
        switch (scale) {
            case 10:
                return 2;
            case 20:
            case 30:
                return 3;
            case 40:
            case 50:
                return 5;
            case 60:
            case 70:
                return 7;
            case 80:
            case 90:
                return 9;
            default: return 0;
        }
    }

    public int getGridLineWidth() {
        int size = (int) (getLineWidth() / 1.5);
        if (size == 0) size++;
        return size;
    }

    /** def.Circuit Draw x to Panel draw x
     * you add this number to a CircuitDrawPoint x to get its PanelDrawPoint x */
    private int xoff = 0;

    /** def.Circuit Draw y to Panel draw y
     * you add this number to a CircuitDrawPoint y to get its PanelDrawPoint y */
    private int yoff = 0;

    /**
     * Returns the number that you have to add to a {@link CircuitPoint} x to get its {@link PanelDrawPoint} x
     * @return CircuitDrawPoint x +-> PanelDrawPoint x
     */
    public int getXOffset() {
        return xoff;
    }

    /**
     * Returns the number that you have to add to a {@link CircuitPoint} y to get its {@link PanelDrawPoint} y
     * @return CircuitDrawPoint y + this = PanelDrawPoint y
     */
    public int getYOffset() {
        return yoff;
    }

    /**
     * Returns the the vector that you have to add to a {@link CircuitPoint} to get its {@link PanelDrawPoint}
     * @return CircuitDrawPoint + this = PanelDrawPoint
     */
    public Vector getOffset() {
        return new Vector(xoff, yoff);
    }

    public void modifyOffset(Vector vector) {
        xoff += vector.x;
        yoff += vector.y;
    }

    public void setOffset(Vector vector) {
        xoff = (int) vector.x;
        yoff = (int) vector.y;
    }

    public void setXOffset(int x) {
        xoff = x;
    }

    public void setYOffset(int y) {
        yoff = y;
    }

    private EntityList<Entity> allEntities = new EntityList<>();

    public EntityList<Entity> getAllEntities() {
        return getEntityList(true);
    }

    public EntityList<Entity> getEntitiesWithinScope(BoundingBox scope) {
        return scope.getInterceptingEntities();
    }

    public EntityList<Entity> getAllEntitiesThatIntercept(CircuitPoint p) {
        EntityList<Entity> list = new EntityList<>();
        for (Entity e : getAllEntities())
            if (e.intercepts(p))
                list.add(e);
        return list;
    }

    public void refreshTransmissions() {
   //     System.out.print("Refresh Transmissions Took " + LogicGates.doTimeTest(() -> {
            Dependent.resetDependencies(this);
            Dependent.resetStates(this);
            Dependent.calculateDependencies(this);
            Dependent.calculateSuperDependencies(this);
            Dependent.illogicalCheck(this);
            Dependent.resetDependencies(this);
            Dependent.calculateDependencies(this);
            Dependent.calculateSuperDependencies(this);
            Dependent.determinePowerStates(this);
   //     }));

    }

    public LinkedList<Dependent> getDependents() {
        return Dependent.getDependentEntities(this);
    }

    public EntityList<Entity> getEntityList(boolean clone) {
        return clone ? allEntities.clone() : allEntities;
    }

    public void setEntityList(EntityList<Entity> list) {
        allEntities = list;
    }

    public void removeEntity(Entity e) {
        if (e instanceof ConnectibleEntity)
            ((ConnectibleEntity) e).disconnectAll();
        boolean removed = allEntities.remove(e);
        if (removed) e.onDelete();
    }

    public void addEntity(Entity entity) {
        allEntities.add(entity);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityList<T> getAllEntitiesOfType(Class<T> type, boolean clone) {
        return allEntities.ofType(type, clone);
    }

    public <T extends Entity> EntityList<T> getAllEntitiesOfType(Class<T> type) {
        return getAllEntitiesOfType(type, true);
    }

    @Override
    public String getPropertyTableHeader() {
        return "Properties For: Circuit";
    }

    @Override
    public PropertyList getPropertyList() {
        PropertyList propList = new PropertyList(this);
        propList.add(new Property("Circuit Name", "", ""));
        return propList;
    }

    @Override
    public void onPropertyChange(ObservableValue<? extends String> observableValue, String s, String t1) {
        System.out.println("OBS VAL " + observableValue + " CHANGED FROM " + s + " TO " + t1);
    }

    private static final String[] properties = new String[] { "Circuit Name" };

    @Override
    public boolean hasProperty(String propertyName) {
        return Arrays.asList(properties).contains(propertyName);
    }

    public void deepCloneEntitiesFrom(Circuit c) {
        for (Entity e : c.getEntityList(false))
            e.clone(this);
    }

    public Circuit cloneOntoProject(String newName) {
        Circuit c = new Circuit(project, newName);
        return c;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }
}
