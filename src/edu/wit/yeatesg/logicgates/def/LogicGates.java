package edu.wit.yeatesg.logicgates.def;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LogicGates {

    @SuppressWarnings("unchecked")
    public static void debug(Object... args) {
        if (args.length % 2 != 0)
            throw new RuntimeException("Invalid Debug");
        for (int i = 0; i < args.length; i++) {
            if (i % 2 == 0)
                System.out.println(args[i]);
            else {
                if (args[i] == null)
                    System.out.println("null");
                else if (args[i].getClass().isArray()) {
                    Object[] arr = (Object[]) args[i];
                    for (Object o : arr)
                        System.out.print("  " + o);
                    System.out.println();
                } else if (args[i] instanceof List) {
                    for (Object o : (List<Object>) args[i])
                        System.out.print("  " + o);
                    System.out.println();
                } else
                    System.out.println("  " + args[i]);
            }
        }
    }
}
