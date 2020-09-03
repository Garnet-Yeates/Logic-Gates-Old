package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

public enum OutputType {

    ONE_AS_FLOATING("0/f"), ZERO_AS_FLOATING("f/1"), ZERO_ONE("0/1"), ANY("ANY");

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
