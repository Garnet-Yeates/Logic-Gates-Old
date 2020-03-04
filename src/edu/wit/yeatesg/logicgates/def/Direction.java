package edu.wit.yeatesg.logicgates.def;

public enum Direction {
    VERTICAL, HORIZONTAL;

    public Direction getPerpendicular() {
        return this == VERTICAL ? HORIZONTAL : VERTICAL;
    }

    public static String cardinalFromRotation(int rotation) {
        switch (rotation) {
            case 1:
            case 90:
                return "WEST";
            case 2:
            case 180:
                return "NORTH";
            case 3:
            case 270:
                return "EAST";
            default: // Covers 0, 360 case as well obv
                return "SOUTH";
        }
    }

    public static int rotationFromCardinal(String cardinal) {
        switch (cardinal) {
            case "WEST":
                return 90;
            case "NORTH":
                return 180;
            case "EAST":
                return 270;
            default: // Covers South Obv
                return 0;
        }
    }
}
