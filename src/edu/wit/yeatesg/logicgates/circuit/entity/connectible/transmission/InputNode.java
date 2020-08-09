package edu.wit.yeatesg.logicgates.circuit.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.datatypes.Vector;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import edu.wit.yeatesg.logicgates.datatypes.PanelDrawPoint;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class InputNode extends ConnectionNode {

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom, ConnectibleEntity connectedTo) {
        super(location, connectingFrom, connectedTo);
    }

    public InputNode(CircuitPoint location, ConnectibleEntity connectingFrom) {
        this(location, connectingFrom, null);
    }

    public CircuitPoint tailLocation;

    public void setTailLocation(CircuitPoint tailLoc) {
        tailLocation = tailLoc;
    }

    public void drawTail(GraphicsContext g, Color col) {
        if (!tailLocation.isSimilar(location)) {
            PowerValue powerValue = isNegated ? getPowerValueFromTree().getNegated() : getPowerValueFromTree();
            col = col == null ? powerValue.getColor() : col;
            g.setLineWidth(Wire.getLineWidth(location.getCircuit()));
            g.setStroke(col);
            CircuitPoint nodeLoc = location.getSimilar();
            if (isNegated)
                nodeLoc = nodeLoc.getIfModifiedBy(getVectorToParent());
            CircuitPoint tailCurvePoint = tailLocation;
            Vector l1ToL2 = new Vector(nodeLoc, tailCurvePoint);
            if (l1ToL2.getLength() < 0.15)
                return;
            double cuttingOff = 0.1;
            double cuttingOffPercent = cuttingOff / l1ToL2.getLength();
            l1ToL2 = l1ToL2.getMultiplied(1 - cuttingOffPercent);
            tailCurvePoint = nodeLoc.getIfModifiedBy(l1ToL2);

            PanelDrawPoint p1 = nodeLoc.toPanelDrawPoint();
            PanelDrawPoint p2 = tailCurvePoint.toPanelDrawPoint();

            g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        if (isNegated) {
            CircuitPoint negCenter = location.getIfModifiedBy(getVectorToParent().getMultiplied(0.5));
            ConnectionNode.drawNegationCircle(g, col == null ? Color.BLACK : col, negCenter, 0.8);
        }
        col = col == null ? getPowerValueFromTree().getColor() : col;
        g.setFill(col);
        double circleSize = parent.getCircuit().getScale() * 0.55;
        circleSize *= getLocation().getCircuit().getScale() < 10 ? 1.1 : 1;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
