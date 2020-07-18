package edu.wit.yeatesg.logicgates.entity;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface Rotatable {

    RelativePointSet getRelativePointSet();

    List<Integer> rotations = Arrays.asList(0, 90, 180, 270);

    static int getNextRotation(int rotation) {
        return rotations.get((rotations.indexOf(rotation) + 1) % 4);
    }

    default boolean validRotation(int rotation) {
        return Arrays.asList(new Integer[]{360, 90, 180, 270}).contains(rotation);
    }

    class RelativePointSet extends PointSet {
        public PointSet applyToOrigin(CircuitPoint origin, int rotation) {
            ArrayList<Vector> relationshipToOriginSet = new ArrayList<>();
            CircuitPoint relativeOrigin = this.get(0);
            for (CircuitPoint point : this) {
                Vector relationToOrigin = new Vector(relativeOrigin, point);
                relationshipToOriginSet.add(relationToOrigin);
            }
            for (int i = 0; i < relationshipToOriginSet.size(); i++)
                relationshipToOriginSet.set(i, relationshipToOriginSet.get(i).getRotated(rotation));
            PointSet drawSet = new PointSet();
            for (int i = 0; i < size(); i++)
                drawSet.add(origin.getIfModifiedBy(relationshipToOriginSet.get(i)));
            return drawSet;
        }

        public void add(double x, double y, Circuit c) {
            add(new CircuitPoint(x, y, c));
        }
    }
}
