package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.entity.Dynamic;
import edu.wit.yeatesg.logicgates.entity.Property;
import edu.wit.yeatesg.logicgates.entity.connectible.InputBlock;
import edu.wit.yeatesg.logicgates.entity.connectible.Wire;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.def.GateAND;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.concurrent.CountDownLatch;


// Rename to LogicGates
public class MainGUI extends Application {

    private BorderPane propertiesGoHere;
    private EditorPanel editorPanel;

    private Stage stage;
    private Project currProject;

    public static void main(String[] args) {
        Platform.runLater(() -> {
            new MainGUI(new Stage(), null);
        });
    }

    public MainGUI(Stage stage, Project project) {
        start(stage);
        Project p = project;
        if (p == null)
            p = new Project("untitled");
        setCurrentProject(p);
    }

    public File getFileFromFileDialog(Stage stage, boolean open) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(open ? "Open Project" : "Save Project To");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Circuit", "*.cxml"));

        File f =  fileChooser.showOpenDialog(stage);
        return f;
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
    }

    public void onMenuOpenPress() {
        File f = getFileFromFileDialog(stage, true);
        Project proj;
        try {
            proj = Project.fromFile(f);
            YesNoGUI yesNoGUI = new YesNoGUI(new Stage(), "Garnet FUCKS", "Open Project In New Window?");
            yesNoGUI.setNoAction((e) -> {
                // TODO ask if they want to save
                setCurrentProject(proj);
            });
            yesNoGUI.setYesAction((e) -> Platform.runLater(() -> new MainGUI(new Stage(), proj)));
            yesNoGUI.showAndWait();
        } catch (Project.LoadFailedException e) {
            // TODO show "Load Failed" dialog bs
            System.out.println(e.getMessage());
        }
    }

    public void setCurrentProject(Project p) {
        p.setGUI(this);
        this.currProject = p;
        if (!currProject.hasCircuits())
            new Circuit(currProject, "main");

        BorderPane borderPane = new BorderPane();

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open...     ");
        openMenuItem.setOnAction((e) -> {
            Platform.runLater(this::onMenuOpenPress);
        });
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        borderPane.setTop(menuBar);

        SplitPane horizSplitPane = new SplitPane();
        horizSplitPane.setOrientation(Orientation.HORIZONTAL);
        borderPane.setCenter(horizSplitPane);

        TreeItem<String> treeRoot = new TreeItem<>("Root");
        TreeItem<String> item1 = new TreeItem<>("Item1");
        item1.getChildren().add(new TreeItem<>("ItemA"));
        item1.getChildren().add(new TreeItem<>("ItemB"));
        item1.getChildren().add(new TreeItem<>("ItemC"));
        TreeItem<String> item2 = new TreeItem<>("Item2");
        TreeItem<String> item3 = new TreeItem<>("Item3");
        treeRoot.getChildren().add(item1);
        treeRoot.getChildren().add(item2);
        treeRoot.getChildren().add(item3);
        TreeView<String> treeView = new TreeView<>(treeRoot);
        treeView.setMinHeight(150);
        stage.heightProperty().addListener((observableValue, number, t1) -> treeView.setPrefHeight(Integer.MAX_VALUE));

        BorderPane paneWhereIconsGo = new BorderPane();
        paneWhereIconsGo.setCenter(new Label("Buttons and shit go here"));
        paneWhereIconsGo.setMinHeight(100);

        propertiesGoHere = new BorderPane();
        propertiesGoHere.setCenter(new Label("Item Properties and shit go here"));

        final int LEFT_MIN_WIDTH = 275;
        final int LEFT_MAX_WIDTH = 375;
        VBox leftOfSplitPane = new VBox();
        leftOfSplitPane.setMinWidth(LEFT_MIN_WIDTH);
        leftOfSplitPane.setMaxWidth(LEFT_MAX_WIDTH);
        leftOfSplitPane.getChildren().add(paneWhereIconsGo);
        leftOfSplitPane.getChildren().add(treeView);
        leftOfSplitPane.getChildren().add(propertiesGoHere);

        horizSplitPane.getItems().add(leftOfSplitPane);

        final int EDITOR_MIN_SIZE = 500;
        editorPanel = new EditorPanel(currProject);
        editorPanel.setPrefSize(700, 700);
        editorPanel.setMinSize(EDITOR_MIN_SIZE, EDITOR_MIN_SIZE);
        // Bind canvas size to stack pane size.

        horizSplitPane.getItems().add(editorPanel);

        Scene scene = new Scene(borderPane, 1250, 750, true, SceneAntialiasing.DISABLED);
        stage.setScene(scene);
        stage.setTitle("Logic Gates");
        stage.show();

        setPropertyTable(editorPanel.getCurrentCircuit());

        stage.setMinHeight(menuBar.getMinHeight() + EDITOR_MIN_SIZE + 100);
        stage.setMinWidth(EDITOR_MIN_SIZE + LEFT_MIN_WIDTH + 50);
        borderPane.setPrefSize(1250, 750);

        editorPanel.repaint();

        postInit();
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public Project getCurrProject() {
        return currProject;
    }

    public void postInit() {
        editorPanel.repaint();
       /* Circuit currentCircuit = editorPanel.getCurrentCircuit();
        new GateAND(new CircuitPoint(0, 10, currentCircuit), 90);
        new GateAND(new CircuitPoint(-10, 10, currentCircuit), 180);

        new Wire(new CircuitPoint(0, 0, currentCircuit), new CircuitPoint(-5, 0, currentCircuit));
        new Wire(new CircuitPoint(0, 0, currentCircuit), new CircuitPoint(5, 0, currentCircuit));

        new InputBlock(new CircuitPoint(5, -5, currentCircuit), 180);*/
    }

    public void setPropertyTable(Dynamic propetiable) {
        TableView<Property> table = propetiable.getPropertyList().toTableView();
        propertiesGoHere.setCenter(table);
        propertiesGoHere.setMinHeight(table.getMinHeight());
    }
}
