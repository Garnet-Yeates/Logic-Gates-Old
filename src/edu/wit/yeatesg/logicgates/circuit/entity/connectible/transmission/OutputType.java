package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

public enum OutputType {

    ZERO_FLOATING("0/f"), FLOATING_ONE("f/1"), ZERO_ONE("0/1");

    String simpleString;

    OutputType(String simpleString) {
        this.simpleString = simpleString;
    }

    public String getSimpleString() {
        return simpleString;
    }

    public static String[] getSimpleStrings() {
        String[] simpleStrings = new String[values().length];
        for (int i = 0; i < values().length; i++)
            simpleStrings[i] = values()[i].simpleString;
        return simpleStrings;
    }

    public static OutputType parse(String s) {
        for (OutputType type : values())
            if (type.toString().equalsIgnoreCase(s) || type.simpleString.equalsIgnoreCase(s))
                return type;
        return null;
    }
}
