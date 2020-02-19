package edu.wit.yeatesg.logicgates.connections;

import com.sun.jdi.connect.spi.Connection;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.Entity;
import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static edu.wit.yeatesg.logicgates.def.LogicGates.*;

public class ddd {

    public static void main(String[] args) {
        ArrayList<Entity> arr = new ArrayList<>(44);
        arr.set(4, new InputBlock(new CircuitPoint(4, 4, new Circuit()), 0));

        /**star: ( 5.0 , -7.0 )
         End: ( 5.0 , 2.0 )*/
        Circuit c = new Circuit();
        CircuitPoint start = new CircuitPoint(5, -7, c);
        CircuitPoint end = new CircuitPoint(5, 2, c);
        System.out.println(Vector.directionVectorFrom(start, end, Direction.VERTICAL));
        System.out.println(Vector.directionVectorFrom(start, end, Direction.HORIZONTAL));

        Integer[] arry = new Integer[] { 1, 2, 3, 4, 5 };
        List<Integer> l = Arrays.asList(arry);
        debug("Length", 1, "Size", 2, "Array", arry, "List", Arrays.asList(arry));
    }
}
