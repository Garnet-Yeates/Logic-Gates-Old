package edu.wit.yeatesg.logicgates.def;

import edu.wit.yeatesg.logicgates.connections.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.connections.EntityList;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static java.awt.event.KeyEvent.VK_CONTROL;

public class Circuit {

    public static final Color COL_BG = Color.white;
    public static final Color COL_GRID = Color.darkGray;
    public static final Color COL_ORIGIN = Color.red;

    private EditorPanel editorPanel;

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public void setEditorPanel(EditorPanel editorPanel) {
        this.editorPanel = editorPanel;
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
        System.out.println("scale: " + scale + " SCALE_MAX: " + SCALE_MAX);
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

    public int getStrokeSize() {
        switch (scale) {
            case 10: return 3;
            case 20:
            case 30:
                return 5;
            case 40:
            case 50:
                return 7;
            case 60:
            case 70:
                return 9;
            case 80:
            case 90:
                return 11;
            default: return 0;
        }
    }


    public Stroke getStroke() {
        return new BasicStroke(getStrokeSize());
    }

    public Stroke getGridStroke() {
        int size = (int) (getStrokeSize() / 2.5);
        if (size == 0) size++;
        return new BasicStroke(size);
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
        return getAllEntities(true);
    }

    public EntityList<Entity> getAllEntitiesThatIntercept(CircuitPoint p) {
        EntityList<Entity> list = new EntityList<>();
        for (Entity e : getAllEntities())
            if (e.intercepts(p))
                list.add(e);
        return list;
    }

    public void refreshTransmissions() {
        for (ConnectibleEntity e : getAllEntitiesOfType(ConnectibleEntity.class))
            e.resetPower();
        for (ConnectibleEntity e : getAllEntitiesOfType(ConnectibleEntity.class))
            if (e.isPowerSource())
                e.onPowerReceive();
    }

    public EntityList<Entity> getAllEntities(boolean clone) {
        return clone ? allEntities.clone() : allEntities;
    }

    public boolean removeEntity(Entity e) {
        if (e instanceof ConnectibleEntity)
            ((ConnectibleEntity) e).disconnectAll();
        boolean removed = allEntities.remove(e);
        if (removed) e.onDelete();
        return removed;
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

}
