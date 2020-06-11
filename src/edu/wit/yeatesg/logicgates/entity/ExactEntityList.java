package edu.wit.yeatesg.logicgates.entity;


import java.util.Collection;

public class ExactEntityList<E extends Entity> extends EntityList<E> {

    public ExactEntityList() {
        super();
    }

    public ExactEntityList(Collection<? extends E> list) {
        super(list);
    }

    public boolean containsExact(E entity) {
        for (E e : this)
            if (e == entity)
                return true;
        return false;
    }

    public boolean containsSimilar(E entity) {
        return super.contains(entity);
    }

    @Override
    public final boolean contains(E entity) {
        throw new UnsupportedOperationException();
    }

    public int indexOfExact(Object o) {
        for (int i = 0; i < size(); i++)
            if (get(i) == o)
                return i;
        return -1;
    }

    public int indexOfSimilar(Object o) {
        return super.indexOf(o);
    }

    @Override
    public final int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }


    public boolean removeExact(E e) {
        int indexOfExact;
        if ((indexOfExact = indexOfExact(e)) != -1) {
            remove(indexOfExact);
            return true;
        }
        return false;
    }

    @Override
    public EntityList<E> except(Entity e) {
        throw new UnsupportedOperationException();
    }

    public EntityList<E> exceptExact(E e) {
        removeExact(e);
        return this;
    }

    public EntityList<E> exceptSimilar(E e) {
        removeSimilar(e);
        return this;
    }


    public boolean removeSimilar(E e) {
        return super.remove(e);
    }

    @Override
    public final boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }
}
