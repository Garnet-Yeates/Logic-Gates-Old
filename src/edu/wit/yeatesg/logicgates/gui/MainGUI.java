package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.SimpleGateAND;
import edu.wit.yeatesg.logicgates.entity.Dynamic;
import edu.wit.yeatesg.logicgates.entity.Property;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.entity.connectible.InputBlock;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;


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
        initGUI();
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
            YesNoGUI yesNoGUI = new YesNoGUI(new Stage(), "Option", "Open Project In New Window?");
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

    private MenuBar menuBar;
    MenuItem undoMenuItem;

    private void initGUI() {

    }

    public void setCurrentProject(Project p) {
        p.setGUI(this);
        this.currProject = p;
        if (!currProject.hasCircuits()) {
            new Circuit(currProject, "main");
        }

        BorderPane borderPane = new BorderPane();

        menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open...     ");
        openMenuItem.setOnAction((e) -> {
            Platform.runLater(this::onMenuOpenPress);
        });
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
        borderPane.setTop(menuBar);

        // Edit menu

        Menu editMenu = new Menu("Edit");
        menuBar.getMenus().add(editMenu);

        undoMenuItem = new MenuItem("Undo");
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(undoMenuItem);

        MenuItem megaUndoMenuItem = new MenuItem("Mega Undo");
        megaUndoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().add(megaUndoMenuItem);

        MenuItem redoMenuItem = new MenuItem("Redo");
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(redoMenuItem);

        MenuItem megaRedoItem = new MenuItem("Mega Redo");
        megaRedoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().add(megaRedoItem);

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

        setPropertyTable(editorPanel.c());

        stage.setMinHeight(menuBar.getMinHeight() + EDITOR_MIN_SIZE + 125);
        stage.setMinWidth(EDITOR_MIN_SIZE + LEFT_MIN_WIDTH + 75);
        borderPane.setPrefSize(1250, 750);

        editorPanel.repaint(currProject.getCurrentCircuit());

        postInit();
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public Project getCurrProject() {
        return currProject;
    }

    public void postInit() {
        editorPanel.repaint(currProject.getCurrentCircuit());
        Circuit c = currProject.getCurrentCircuit();

        new InputBlock(new CircuitPoint(13, -5, c), 0);
        new InputBlock(new CircuitPoint(17, -5, c), 0);

        new InputBlock(new CircuitPoint(23, -5, c), 0);
        new InputBlock(new CircuitPoint(27, -5, c), 0);

        new InputBlock(new CircuitPoint(33, -5, c), 0);
        new InputBlock(new CircuitPoint(37, -5, c), 0);

        new SimpleGateAND(new CircuitPoint(15, 5, c), 0);
        new SimpleGateAND(new CircuitPoint(25, 5, c), 0);
        new SimpleGateAND(new CircuitPoint(35, 5, c), 0);

        new SimpleGateAND(new CircuitPoint(20, 15, c), 0);

        new SimpleGateAND(new CircuitPoint(30, 15, c), 0);


// 14, -9
        // 16, -9


        c.refreshTransmissions();
        editorPanel.repaint(currProject.getCurrentCircuit());

    }

    public void setPropertyTable(Dynamic dynamic) {
        TableView<Property> table = dynamic.getPropertyList().toTableView();
        propertiesGoHere.setCenter(table);
        propertiesGoHere.setMinHeight(table.getMinHeight());
    }
}
