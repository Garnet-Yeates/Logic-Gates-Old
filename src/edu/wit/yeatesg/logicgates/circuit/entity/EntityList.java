package edu.wit.yeatesg.logicgates.circuit.entity;

import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.datatypes.BoundingBox;
import edu.wit.yeatesg.logicgates.datatypes.Direction;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission.Wire;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


// TODO We WILL do MemoryEntityList. The CircuitEntityList/InterceptMap Lists/InvalidEntity list will all be tracked via memory
// TODO we just need to make sure that when we have methods like memoryRemove(Entity e) that makes sure it is '==' in memory
// when we need to
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

    // swaps the element at the end of the list with the one at the index
    public E efficientRemove(int index) {
        if (index < 0 || index > size() - 1)
            throw new IndexOutOfBoundsException("Fack yuuu");
        int sizeBefore = size();
        E actuallyRemoving = get(index);
        E removed = super.remove(sizeBefore - 1);
        if (index != sizeBefore - 1) {
            set(index, removed);
            onSwap(removed, index);
        }
        return actuallyRemoving;
    }

    // Can be implemented
    public void onSwap(Entity e, int toIndex) { }

    @Override
    public boolean remove(Object o) {
        efficientRemove(indexOf(o));
        return true;
    }

    @Override
    public E remove(int index) {
        return efficientRemove(index);
    }

    @SafeVarargs
    public EntityList(E... ents) {
        this(Arrays.asList(ents));
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityList<T> thatExtend(Class<T> type) {
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

    public boolean contains(E entity) {
        return super.contains(entity);
    }

    public EntityList<E> except(Entity e) {
        remove(e);
        return this;
    }


    @Override
    public EntityList<E> clone() {
        return new EntityList<>(this);
    }

    @SuppressWarnings("unchecked")
    public EntityList<E> deepClone() {
        EntityList<E> deepClone = new EntityList<>();
        for (Entity e : this)
            deepClone.add((E) e.getSimilarEntity());
        return deepClone;
    }

    @SuppressWarnings("unchecked")
    public EntityList<E> deepCloneOnto(Circuit c) {
        EntityList<E> deepClonedOntoC = new EntityList<>();
        for (Entity e : this)
            deepClonedOntoC.add((E) e.getCloned(c));
        return deepClonedOntoC;
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
