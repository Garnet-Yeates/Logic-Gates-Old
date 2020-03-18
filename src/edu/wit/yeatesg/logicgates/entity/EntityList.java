package edu.wit.yeatesg.logicgates.entity;

import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Collection;

public class EntityList<E extends Entity> extends ArrayList<E> {

    public EntityList(int size) {
        super(size);
    }

    public EntityList(Collection<? extends E> list) {
        super(list);
    }

    public EntityList() {
        super();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityList<T> ofType(Class<T> type) {
        EntityList<T> list = new EntityList<>(size());
        for (Entity e : clone()) {
            if (e.getClass().equals(type)) {
                list.add((T) e);
            } else {
                ArrayList<Class<?>> classes = new ArrayList<>();
                Class<?> curr = e.getClass();
                while (!curr.getSimpleName().equals("Object")) {
                    classes.add(curr.getSuperclass());
                    curr = curr.getSuperclass();
                }
                if (classes.contains(type))
                    list.add((T) e);
            }
        }
        return list;
    }


    @SuppressWarnings("unchecked")
    public EntityList<E> thatInterceptAll(CircuitPoint... allOfThese) {
        EntityList<E> interceptors = new EntityList<>();
        for (Entity e : this)
            if (e.interceptsAll(allOfThese))
                interceptors.add((E) e);
        return interceptors;
    }

    public EntityList<E> thatInterceptNone(CircuitPoint... noneOfThese) {
        EntityList<E> thatInterceptNone = this.clone();
        for (E entity : this)
            if (entity.interceptsNone(noneOfThese))
                thatInterceptNone.add(entity);
        return thatInterceptNone;
    }

    public EntityList<E> thatInterceptAny(CircuitPoint... points) {
        EntityList<E> thatInterceptAny = this.clone();
        for (E entity : this)
            if (entity.interceptsAny(points))
                thatInterceptAny.add(entity);
        return thatInterceptAny;
    }

    public EntityList<E> thatIntercept(CircuitPoint p) {
        return thatInterceptAll(p);
    }

    public EntityList<E> thatIntercept(BoundingBox b) {
        EntityList<E> interceptList = new EntityList<>();
        for (E e : this)
            if (b.intercepts(e))
                interceptList.add(e);
        return interceptList;
    }

    public EntityList<E> intersection(EntityList<E> other) {
        EntityList<E> intersect = new EntityList<>();
        if (other != null) {
            other = other.clone();
            for (E e : this) {
                if (other.contains(e)) {
                    other.remove(e);
                    intersect.add(e);
                }
            }
        }
        return intersect;
    }

    public EntityList<E> except(Entity e) {
        remove(e);
        return this;
    }


    @SuppressWarnings("unchecked")
    public EntityList<E> clone() {
        return (EntityList<E>) super.clone();
    }

    @SuppressWarnings("unchecked")
    public EntityList<E> deepClone() {
        EntityList<E> deepClone = new EntityList<>();
        for (Entity e : this)
            deepClone.add((E) e.getSimilarEntity());
        return deepClone;
    }

    public EntityList<Wire> getWiresGoingInSameDirection(Direction dir) {
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
        return getWiresGoingInSameDirection(w.getDirection());
    }
}
