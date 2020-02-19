package edu.wit.yeatesg.logicgates.connections;

import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Entity;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Arrays;

public interface Rotatable {

    Entity getRotated(int rotation);
    int getRotation();
    RelativePointSet getRelativePointSet();

    default boolean validRotation(int rotation) {
        return Arrays.asList(new Integer[] { 0, 360, 1, 90, 2, 180, 3, 270 }).contains(rotation);
    }

    class RelativePointSet extends PointSet {
        public PointSet applyToOrigin(CircuitPoint origin, int rotation) {
            ArrayList<Vector> relationshipToOriginSet = new ArrayList<>();
            CircuitPoint relativeOrigin = this.get(0);
            for (CircuitPoint point : this) {
                Vector relationToOrigin = new Vector(relativeOrigin, point);
                relationshipToOriginSet.add(relationToOrigin);
            }
            for (Vector v : relationshipToOriginSet) {
                double xv = v.x;
                double yv = v.y;
                switch (rotation) {
                    case 1: case 90:
                        rotation = 90;
                        v.x = -yv;
                        v.y = xv;
                        break;
                    case 2: case 180:
                        rotation = 180;
                        v.x = -xv;
                        v.y = -yv;
                        break;
                    case 3: case 270:
                        rotation = 270;
                        v.x = yv;
                        v.y = -xv;
                        break;
                }
            }
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
