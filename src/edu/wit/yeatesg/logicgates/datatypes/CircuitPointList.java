package edu.wit.yeatesg.logicgates.datatypes;

import edu.wit.yeatesg.logicgates.circuit.Circuit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class CircuitPointList extends ArrayList<CircuitPoint> {

    public CircuitPointList(CircuitPoint... points) {
        this.addAll(Arrays.asList(points));
    }

    public CircuitPointList(int initialCapacity) {
        super(initialCapacity);
    }

    public CircuitPointList(Circuit c, double... points) {
        if (points.length % 2 != 0)
            throw new RuntimeException("Invalid PointSet!");
        CircuitPoint[] circuitPoints = new CircuitPoint[points.length / 2];
        for (int i = 0, j = 1; j < points.length; i += 2, j += 2)
            circuitPoints[i / 2] = new CircuitPoint(points[i], points[j], c);
        this.addAll(Arrays.asList(circuitPoints));
    }

    public CircuitPointList(Collection<? extends CircuitPoint> coll) {
        super(coll);
    }

    public CircuitPointList() {
        super();
    }

    public boolean intercepts(CircuitPoint p) {
        for (CircuitPoint other : this)
            if (p.equals(other))
                return true;
        return false;
    }

    public boolean intercepts(CircuitPointList other) {
        return intersection(other).size() > 0;
    }

    public CircuitPointList intersection(CircuitPointList other) {
        CircuitPointList intersect = new CircuitPointList();
        if (other != null) {
            other = other.clone();
            for (CircuitPoint p : this) {
                if (other.contains(p)) {
                    other.remove(p);
                    intersect.add(p);
                }
            }
        }
        return intersect;
    }

    public CircuitPointList complement(CircuitPointList universe) {
        CircuitPointList complement = new CircuitPointList(universe);
        for (CircuitPoint p : universe)
            if (contains(p))
                complement.remove(p);
        return complement;
    }

    @Override
    public CircuitPointList clone() {
        return new CircuitPointList(this);
    }

    public CircuitPointList getIfModifiedBy(Vector v) {
        CircuitPointList modified = new CircuitPointList();
        for (CircuitPoint cp : this)
            modified.add(cp.getIfModifiedBy(v));
        return modified;
    }

    public CircuitPointList deepClone() {
       CircuitPointList deepClone = new CircuitPointList(size());
       for (CircuitPoint p : this)
           deepClone.add(p.clone());
       return deepClone;
    }

    /**
     * Mutates the Points in this PointSet
     * @param v
     */
    public void addVectorToAllPoints(Vector v) {
        for (CircuitPoint p : this) {
            p.x += v.x;
            p.y += v.y;
        }
    }

    public CircuitPointList union(CircuitPointList other) {
        CircuitPointList union = new CircuitPointList(this);
        union.addAll(other);
        return union;
    }
}