package edu.wit.yeatesg.logicgates.circuit.entity;


import java.util.Collection;

public class ExactEntityList<E extends Entity> extends EntityList<E> {

    public ExactEntityList() {
        super();
    }

    public ExactEntityList(Collection<? extends E> list) {
        super(list);
    }

    @SafeVarargs
    public ExactEntityList(E... ents) {
        super(ents);
    }

    @Override
    public ExactEntityList<E> clone() {
        return new ExactEntityList(this);
    }

    @SuppressWarnings("unchecked")
    public ExactEntityList<E> deepClone() {
        ExactEntityList<E> deepClone = new ExactEntityList<>();
        for (Entity e : this)
            deepClone.add((E) e.getSimilarEntity());
        return deepClone;
    }

    public boolean containsExact(E entity) {
        return indexOfExact(entity) != -1;
    }

    public boolean containsSimilar(E entity) {
        return super.contains(entity);
    }

    @Override
    public final boolean contains(E entity) {
        throw new UnsupportedOperationException();
    }

    public int indexOfExact(E ent) {
        for (int i = 0; i < size(); i++)
            if (get(i) == ent)
                return i;
        return -1;
    }

    public int indexOfSimilar(E ent) {
        for (int i = 0; i < size(); i++)
            if (get(i).isSimilar(ent))
                return i;
        return -1;
    }

    public boolean removeExact(E e) {
        int indexOfExact;
        if ((indexOfExact = indexOfExact(e)) != -1) {
            efficientRemove(indexOfExact);
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
