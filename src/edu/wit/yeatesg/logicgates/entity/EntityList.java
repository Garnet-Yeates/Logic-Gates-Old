package edu.wit.yeatesg.logicgates.entity;

import edu.wit.yeatesg.logicgates.def.Direction;
import edu.wit.yeatesg.logicgates.def.BoundingBox;
import edu.wit.yeatesg.logicgates.entity.connectible.Wire;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

public class EntityList<E extends Entity> extends ArrayList<E> {

    public EntityList(int size) {
        super(size);
    }

    public EntityList() {
        super();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> EntityList<T> ofType(Class<T> type, boolean loopThruClone) {
        EntityList<T> list = new EntityList<>(size());
        EntityList<E> iteratingThru = loopThruClone ? clone() : this;
        for (Entity e : iteratingThru) {
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

    public <T extends Entity> EntityList<T> ofType(Class<T> type) {
        return ofType(type, false);
    }

    @SuppressWarnings("unchecked")
    public EntityList<E> thatInterceptAll(CircuitPoint... allOfThese) {
        EntityList<E> interceptors = new EntityList<>();
        for (Entity e : this)
            if (e.interceptsAll(allOfThese))
                interceptors.add((E) e);
        return interceptors;
    }

    public EntityList<E> thatAreNotDeleted() {
        EntityList<E> list = new EntityList<>();
        for (E entity : this)
            if (!entity.isDeleted())
                list.add(entity);
        return list;
    }

    public EntityList<E> thatAreDeleted() {
        EntityList<E> list = new EntityList<>();
        for (E entity : this)
            if (entity.isDeleted())
                list.add(entity);
        return list;
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

    public EntityList<E> except(Entity e) {
        remove(e);
        return this;
    }

    /**
     * Represents an Iterable interval [start,end) where start is inclusive and end is exclusive
     */
    private static class Interval implements Iterable<Integer> {

        int start;
        int end;

        public Interval(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public static Interval[] splitIntoIntervals(int range, int numIntervals) {
            Interval[] intervals = new Interval[numIntervals];
            for (int i = 0, j = 1; i < numIntervals; i++, j++)
                intervals[i] = new Interval((i / numIntervals) * range, (j / numIntervals) * range);
            return intervals;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new IntervalIterator();
        }

        public class IntervalIterator implements Iterator<Integer> {
            int curr = start;

            @Override
            public boolean hasNext() {
                return curr < end;
            }

            @Override
            public Integer next() {
                return curr++;
            }
        }
    }

    public void multiThreadForEach(Consumer<E> consumer, int numThreads) {
        Thread[] threads = new Thread[numThreads];
        Interval[] intervals = Interval.splitIntoIntervals(size(), numThreads);
        for (int i = 0; i < intervals.length; i++) {
            Interval interval = intervals[i];
            threads[i] = new Thread(() -> {
                interval.iterator().forEachRemaining((index) -> {
                    consumer.accept(get(index));
                });
            });
            threads[i].start();
        }
        for (Thread t : threads)
            try { t.join(); } catch (InterruptedException ignored) { }
    }




    @SuppressWarnings("unchecked")
    public EntityList<E> clone() {
        return (EntityList<E>) super.clone();
    }
}
