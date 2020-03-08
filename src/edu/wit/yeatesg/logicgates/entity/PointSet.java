package edu.wit.yeatesg.logicgates.entity;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Arrays;

public class PointSet extends ArrayList<CircuitPoint> {

        public PointSet(CircuitPoint... points) {
            this.addAll(Arrays.asList(points));
        }

        public PointSet(int initialCapacity) {
            super(initialCapacity);
        }

        public PointSet(Circuit c, double... points) {
            if (points.length % 2 != 0)
                throw new RuntimeException("Invalid PointSet!");
            CircuitPoint[] circuitPoints = new CircuitPoint[points.length / 2];
            for (int i = 0, j = 1; j < points.length; i += 2, j += 2)
                circuitPoints[i / 2] = new CircuitPoint(points[i], points[j], c);
            this.addAll(Arrays.asList(circuitPoints));
        }

        public PointSet() {
            super();
        }

        public boolean intercepts(CircuitPoint p) {
            for (CircuitPoint other : this)
                if (p.equals(other))
                    return true;
            return false;
        }

        public boolean intercepts(PointSet other) {
            return intersection(other).size() > 0;
        }

        public PointSet intersection(PointSet other) {
            PointSet intersect = new PointSet();
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

    @Override
    public PointSet clone() {
        return (PointSet) super.clone();
    }
}