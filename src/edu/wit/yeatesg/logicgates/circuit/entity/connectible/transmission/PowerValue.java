package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import javafx.scene.paint.Color;

import java.util.Objects;

public class PowerValue {

    public static final PowerValue UNDETERMINED = new PowerValue("UNDETERMINED", -1, Color.rgb(120, 120, 120, 1));
    public static final PowerValue FLOATING_ERROR = new PowerValue("FLOATING_ERROR", 0, Color.rgb(53, 200, 226, 1));
    public static final PowerValue FLOATING = new PowerValue("FLOATING", 1, Color.rgb(0, 67, 169, 1));
    public static final PowerValue OFF = new PowerValue("OFF", 2, Color.rgb(31, 108, 0, 1));
    public static final PowerValue ON = new PowerValue("ON", 2, Color.rgb(55, 219, 0, 1));
    public static final Color MULTI_BIT_COLOR = Color.rgb(154, 222, 17);
    public static final PowerValue INCOMPATIBLE_TYPES = new PowerValue("INCOMPATIBLE_TYPES", 3, Color.rgb(120, 0, 10, 1));
    public static final PowerValue MULTI_MULTI_BIT = new PowerValue("MULTI_MULTI_BIT", 3, Color.rgb(250, 0, 136, 1));
    public static final PowerValue INCOMPATIBLE_BITS = new PowerValue("INCOMPATIBLE_BITS", 3, Color.rgb(255, 105, 6, 1));
    public static final PowerValue SELF_DEPENDENT = new PowerValue("SELF_DEPENDENT", 4, Color.rgb(244, 29, 26, 1));

    private String data;
    private int priority;
    private Color color;

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return data;
    }

    protected PowerValue(String data, int priority, Color color) {
        if (this instanceof MultiBitPowerValue) {
            Integer.parseInt(data); // Will throw exception if this was done wrong
        }
        this.data = data;
        this.priority = priority;
        this.color = color;
    }

    public boolean isRelevantForCalculations() {
        return equals("ON") || equals("OFF") || this instanceof MultiBitPowerValue;
    }

    public PowerValue getNegated() {
        if (equals(OFF))
            return ON;
        if (equals(ON))
            return OFF;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    private int getNumBits() {
        return 1;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof String && ((String) o).equalsIgnoreCase(data)
                || o instanceof PowerValue && ((PowerValue) o).data.equalsIgnoreCase(data);
    }

    public boolean equals(String s) {
        return s.equalsIgnoreCase(data);
    }
}

