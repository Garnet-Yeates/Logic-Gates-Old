package edu.wit.yeatesg.logicgates.connections;

public enum Direction {
    VERTICAL, HORIZONTAL;

    public Direction getPerpendicular() {
        return this == VERTICAL ? HORIZONTAL : VERTICAL;
    }
}
