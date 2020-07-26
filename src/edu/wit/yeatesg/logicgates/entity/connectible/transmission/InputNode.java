package edu.wit.yeatesg.logicgates.entity.connectible.transmission;

import edu.wit.yeatesg.logicgates.def.Vector;
import edu.wit.yeatesg.logicgates.entity.connectible.ConnectibleEntity;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import edu.wit.yeatesg.logicgates.points.PanelDrawPoint;
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
            col = col == null ? getTruePowerValue().getColor() : col;
            g.setLineWidth(Wire.getLineWidth(location.getCircuit()));
            g.setStroke(col);
            CircuitPoint l1 = location.getSimilar();
            if (isNegated)
                l1 = l1.getIfModifiedBy(getVectorToParent());
            CircuitPoint l2 = tailLocation;
            Vector l1ToL2 = new Vector(l1, l2);
            if (l1ToL2.getLength() < 0.05)
                return;
            l2 = l1.getIfModifiedBy(l1ToL2.getUnitVector().getMultiplied(l1ToL2.getLength()*0.8));

            PanelDrawPoint p1 = l1.toPanelDrawPoint();
            PanelDrawPoint p2 = l2.toPanelDrawPoint();

            g.strokeLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    @Override
    public void draw(GraphicsContext g, Color col, double opacity) {
        if (isNegated) {
            CircuitPoint negCenter = location.getIfModifiedBy(getVectorToParent().getMultiplied(0.5));
            ConnectionNode.drawNegationCircle(g, col == null ? Color.BLACK : col, negCenter, 0.8);
        }
        col = col == null ? getPowerStatus().getColor() : col;
        g.setFill(col);
        double circleSize = parent.getCircuit().getScale() * 0.55;
        circleSize *= getLocation().getCircuit().getScale() < 10 ? 1.1 : 1;
        PanelDrawPoint drawPoint = getLocation().toPanelDrawPoint();
        g.fillOval(drawPoint.x - circleSize/2.00, drawPoint.y - circleSize/2.00, circleSize, circleSize);
    }
}
