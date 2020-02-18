package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.Entity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class EntityList<T extends Entity> extends ArrayList<T> {

    public EntityList(int size) {
        super(size);
    }

    public EntityList() {
        super();
    }

    @SuppressWarnings("unchecked")
    public <E extends Entity> EntityList<E> ofType(Class<E> type, boolean loopThruClone) {
        EntityList<E> list = new EntityList<>(size());
        EntityList<T> iteratingThru = loopThruClone ? clone() : this;
        for (Entity e : iteratingThru) {
            if (e.getClass().equals(type)) {
                list.add((E) e);
            } else {
                ArrayList<Class<?>> classes = new ArrayList<>();
                Class<?> curr = e.getClass();
                while (!curr.getSimpleName().equals("Object")) {
                    classes.add(curr.getSuperclass());
                    curr = curr.getSuperclass();
                }
                if (classes.contains(type))
                    list.add((E) e);
            }
        }
        return list;
    }

    public <E extends Entity> EntityList<E> ofType(Class<E> type) {
        return ofType(type, true);
    }

    @SuppressWarnings("unchecked")
    public EntityList<T> thatInterceptAll(CircuitPoint... allOfThese) {
        EntityList<T> interceptors = new EntityList<>();
        for (Entity e : this)
            if (e.interceptsAll(allOfThese))
                interceptors.add((T) e);
        return interceptors;
    }

    public EntityList<T> thatAreNotDeleted() {
        EntityList<T> list = new EntityList<>();
        for (T entity : this)
            if (!entity.isDeleted())
                list.add(entity);
        return list;
    }

    public EntityList<T> thatAreDeleted() {
        EntityList<T> list = new EntityList<>();
        for (T entity : this)
            if (entity.isDeleted())
                list.add(entity);
        return list;
    }

    public EntityList<T> thatInterceptNone(CircuitPoint... noneOfThese) {
        EntityList<T> thatInterceptNone = this.clone();
        for (T entity : this)
            if (entity.interceptsNone(noneOfThese))
                thatInterceptNone.add(entity);
        return null;
    }

    public EntityList<T> thatInterceptAny(CircuitPoint... points) {
        EntityList<T> thatInterceptAny = this.clone();
        for (T entity : this)
            if (entity.interceptsAny(points))
                thatInterceptAny.add(entity);
        return null;
    }

    public EntityList<Wire> getWiresGoingInDirection(Direction dir) {
        EntityList<Wire> list = new EntityList<>();
        for (Entity e : this)
            if (e instanceof Wire && ((Wire) e).getDirection() == dir)
                list.add((Wire) e);
        return list;
    }

    public EntityList<Wire> getWiresGoingInOppositeDirection(Direction dir) {
        EntityList<Wire> list = new EntityList<>();
        for (Entity e : this)
            if (e instanceof Wire && ((Wire) e).getDirection() != dir)
                list.add((Wire) e);
        return list;
    }

    public EntityList<Wire> getWiresGoingInOppositeDirection(Wire w) {
        return getWiresGoingInOppositeDirection(w.getDirection());
    }

    public EntityList<Wire> getWiresGoingInSameDirection(Wire w) {
        return getWiresGoingInDirection(w.getDirection());
    }

    public EntityList<T> except(Entity e) {
        remove(e);
        return this;
    }


    public EntityList<T> thatIntercept(CircuitPoint p) {
        return thatInterceptAll(p);
    }

    @SuppressWarnings("unchecked")
    public EntityList<T> clone() {
        return (EntityList<T>) super.clone();
    }
}
