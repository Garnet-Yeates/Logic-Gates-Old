package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

public class MultiBitPowerValue extends PowerValue {

    public MultiBitPowerValue(String bits) {
        super(bits, 2, MULTI_BIT_COLOR);
    }

    public MultiBitPowerValue getAND(MultiBitPowerValue other) {
        return null;
    }

    @Override
    public PowerValue getNegated() {
        String data = getData();
        StringBuilder newData = new StringBuilder();
        for (char c : data.toCharArray())
            if (c == '0')
                newData.append('1');
            else
                newData.append('0');
        return new MultiBitPowerValue(newData.toString());
    }
}
