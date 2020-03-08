package edu.wit.yeatesg.logicgates.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class YesNoGUI {

    private Stage stage;
    private Button yes;
    private Button no;

    public YesNoGUI(Stage stage, String title, String labelMsg) {
        this.stage = stage;
        stage.initModality(Modality.APPLICATION_MODAL);
        Pane pane = new Pane();
        final int buttonWidth = 60;
        final int buttonHeight = 25;
        final int buttonGap = 5;
        final int yGapBtwnLabelAndCeil = 15;
        final int yGapBtwnBtnsAndLbl = 5;
        final int edgeGap = 10;

        Button yes = new Button("Yes");
        yes.setPrefWidth(buttonWidth);
        yes.setPrefHeight(buttonHeight);
        Button no = new Button("No");
        no.setPrefWidth(buttonWidth);
        no.setPrefHeight(buttonHeight);

        yes.setOnAction((e) -> {
            if (YesNoGUI.this.yesEventHandler != null)
                YesNoGUI.this.yesEventHandler.handle(e);
            stage.close();
        });

        no.setOnAction((e) -> {
            if (YesNoGUI.this.noEventHandler != null)
                YesNoGUI.this.noEventHandler.handle(e);
            stage.close();
        });

        stage.setOnCloseRequest(null);

        Label label = new Label(labelMsg);
        Font font = new Font("Arial", 18);
        label.setFont(font);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.RIGHT);
        Text toFindSize = new Text(labelMsg);
        toFindSize.setTextAlignment(TextAlignment.CENTER);
        toFindSize.setFont(font);
        toFindSize.applyCss();
        double lblWidth = toFindSize.getLayoutBounds().getWidth();
        double lblHeight = toFindSize.getLayoutBounds().getHeight();

        double sceneWidth = Math.max(2*edgeGap + 2*buttonWidth + buttonGap, lblWidth) * 1.2;
        label.setLayoutY(yGapBtwnLabelAndCeil);
        label.setLayoutX(0);
        label.setPrefWidth(sceneWidth);
        label.setPrefHeight(lblHeight);

        double sceneHeight = edgeGap*2 + yGapBtwnBtnsAndLbl + buttonHeight + lblHeight + yGapBtwnLabelAndCeil;

        double middleX = sceneWidth / 2;
        yes.setLayoutX(middleX - buttonGap/2.0 - buttonWidth);
        no.setLayoutX(middleX + buttonGap/2.0);
        double buttonY = sceneHeight - (edgeGap + buttonHeight);
        yes.setLayoutY(buttonY);
        no.setLayoutY(buttonY);

        pane.getChildren().add(yes);
        pane.getChildren().add(no);
        pane.getChildren().add(label);
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setScene(new Scene(pane, sceneWidth, sceneHeight));
    }

    private EventHandler<ActionEvent> yesEventHandler = null;

    public void setYesAction(EventHandler<ActionEvent> handler) {
        yesEventHandler = handler;
    }

    private EventHandler<ActionEvent> noEventHandler = null;

    public void setNoAction(EventHandler<ActionEvent> handler) {
        noEventHandler = handler;
    }

    public void showAndWait() {
        stage.showAndWait();
    }

    public void close() {
        stage.close();
    }
}
