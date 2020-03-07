package edu.wit.yeatesg.logicgates.entity.connectible;

import javafx.scene.paint.Color;

public enum State {
    ON(Color.rgb(50, 199, 0, 1)),
    OFF(Color.rgb(34, 99, 0, 1)),
    PARTIALLY_DEPENDENT(Color.rgb(34, 205, 205, 1)),
    NO_DEPENDENT(Color.rgb(0, 67, 169, 1)),
    ILLOGICAL(Color.rgb(162, 0, 10, 1));

    private Color color;

    State(Color col) {
        color = col;
    }

    public Color getColor() {
        return color;
    }
}
