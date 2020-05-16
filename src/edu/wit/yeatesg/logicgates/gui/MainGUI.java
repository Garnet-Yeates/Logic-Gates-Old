package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.def.SimpleGateAND;
import edu.wit.yeatesg.logicgates.entity.PropertyMutable;
import edu.wit.yeatesg.logicgates.entity.Property;
import edu.wit.yeatesg.logicgates.def.Circuit;
import edu.wit.yeatesg.logicgates.entity.connectible.InputBlock;
import edu.wit.yeatesg.logicgates.points.CircuitPoint;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;


// Rename to LogicGates
public class MainGUI extends Application {

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

    // INIT WINDOW

    private BorderPane mainBorderPane;

        // Top of the main BorderPane
        private MenuBar menuBar;

        // Center of the main BorderPane
        private SplitPane leftRightDividerPane;

            // Left of the divider pane
            private VBox leftOfDivider;
                private BorderPane        whereButtonsGo;
                private TreeView<String>  treeView;
                private BorderPane        propertiesGoHere;

            // Right of divider pane
            private BorderPane rightOfDivider;
                private EditorPanel editorPanel; // This will be the center of the BorderPane on the right of the div
                private HBox editorInfoBox;



    private void initGUI() {

        // Init mainBorderPane
        mainBorderPane = new BorderPane();

        // Init MenuBar() -> Will be the top of the main pane, with menus: File, Edit, etc
        initMenuBar();

        // Init dividerPane. Will be he center of the main pane. Will contain a left and right, each with multiple other nodes
        leftRightDividerPane = new SplitPane();
        leftRightDividerPane.setOrientation(Orientation.HORIZONTAL);
        mainBorderPane.setCenter(leftRightDividerPane);

        // Init Left Of Divider() -> VBox from top to bottom: whereButtonsGo, treeView, propertiesGoHere
        initLeftOfDivider();

        // Init Right Of Divider() -> <editorPanel>, editorInfoBox. The editorPanel isn't technically set until a project is set
        initRightOfDivider();
    }

    private void initMenuBar() {
        menuBar = new MenuBar();
        mainBorderPane.setTop(menuBar);
        initFileMenu();
        initEditMenu();;
    }

    private Menu fileMenu;
    private MenuItem openMenuItem;

    private void initFileMenu() {
        fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open...     ");
        openMenuItem.setOnAction((e) -> {
            Platform.runLater(this::onMenuOpenPress);
        });
        fileMenu.getItems().add(openMenuItem);
        menuBar.getMenus().add(fileMenu);
    }

    private Menu editMenu;
    private MenuItem undoMenuItem;
    private MenuItem megaUndoMenuItem;
    private MenuItem redoMenuItem;
    private MenuItem megaRedoMenuItem;

    public Menu getEditMenu() {
        return editMenu;
    }

    public MenuItem getUndoMenuItem() {
        return undoMenuItem;
    }

    public MenuItem getMegaUndoMenuItem() {
        return megaUndoMenuItem;
    }

    public MenuItem getRedoMenuItem() {
        return redoMenuItem;
    }

    public MenuItem getMegaRedoMenuItem() {
        return megaRedoMenuItem;
    }

    private void initEditMenu() {
        editMenu = new Menu("Edit");
        menuBar.getMenus().add(editMenu);

        // Section 1: Undo, Mega Undo, Redo, Mega Redo

        undoMenuItem = new MenuItem("Undo  ");
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(undoMenuItem);
        undoMenuItem.setOnAction((e) ->  {
            editorPanel.undo(true);
        });
            // TODO CALL UNDO FUNCTION

        megaUndoMenuItem = new MenuItem("Mega Undo  ");
        megaUndoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().add(megaUndoMenuItem);
        megaUndoMenuItem.setOnAction((e) -> {
            editorPanel.megaUndo(true);
        });

        redoMenuItem = new MenuItem("Redo  ");
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(redoMenuItem);
        redoMenuItem.setOnAction((e) -> {
            editorPanel.redo(true);
        });

        megaRedoMenuItem = new MenuItem("Mega Redo  ");
        megaRedoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().add(megaRedoMenuItem);
        megaRedoMenuItem.setOnAction((e) -> {
            editorPanel.megaRedo(true);
        });


        // Section 2: Copy/Paste/Cut. Going to be a while for me to implement this

    }

    private static final int LEFT_MIN_WIDTH = 275;
    private static final int LEFT_MAX_WIDTH = 375;

    private void initLeftOfDivider() {
        // The left of the divider will be a VBox. From top to bottom -> whereButtonsGo, treeView, property table
        leftOfDivider = new VBox();
        leftOfDivider.setMinWidth(LEFT_MIN_WIDTH);
        leftOfDivider.setMaxWidth(LEFT_MAX_WIDTH);
        leftRightDividerPane.getItems().add(leftOfDivider);

        // The top of the VBox is the area where buttons will go. Idk what these buttons will do yet
        whereButtonsGo = new BorderPane();
        whereButtonsGo.setCenter(new Label("Buttons and stuff go here"));
        whereButtonsGo.setMinHeight(100);
        leftOfDivider.getChildren().add(whereButtonsGo);

        // The middle of the VBox is the treeView to choose entities to add to the Circuit
        treeView = new TreeView<>();
        treeView.setMinHeight(150); // The listener below makes it so the treeView is always as big as it can get
        stage.heightProperty().addListener((observableValue, number, t1) -> treeView.setPrefHeight(Integer.MAX_VALUE));
        leftOfDivider.getChildren().add(treeView);
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
        treeView.setRoot(treeRoot);

        // Property table
        propertiesGoHere = new BorderPane();
        propertiesGoHere.setCenter(new Label("Item Properties and shit go here"));
        leftOfDivider.getChildren().add(propertiesGoHere);
    }

    private void initRightOfDivider() {
        rightOfDivider = new BorderPane();
        leftRightDividerPane.getItems().add(rightOfDivider);
        editorInfoBox = new HBox();
        editorInfoBox.setBorder(new Border(new BorderStroke(Color.BLACK, Color.GREEN, Color.BLUE, Color.YELLOW,
                BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
                CornerRadii.EMPTY, new BorderWidths(1), Insets.EMPTY)));
        editorInfoBox.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255, 1), null, null)));
        editorInfoBox.setMaxHeight(25);
        editorInfoBox.setMinHeight(25);
        rightOfDivider.setBottom(editorInfoBox);
    }


    public void setCurrentProject(Project p) {
        p.setGUI(this); // TODO probs set this upon project construction
        this.currProject = p;
        if (!currProject.hasCircuits()) {
            new Circuit(currProject, "main");
        }

        final int EDITOR_MIN_SIZE = 500;
        editorPanel = new EditorPanel(currProject);
        editorPanel.setPrefSize(700, 700);
        editorPanel.setMinSize(EDITOR_MIN_SIZE, EDITOR_MIN_SIZE);
        // Bind canvas size to stack pane size.

        rightOfDivider.setCenter(editorPanel);

        Scene scene = new Scene(mainBorderPane, 1250, 750, true, SceneAntialiasing.DISABLED);
        stage.setScene(scene);
        stage.setTitle("Logic Gates");
        stage.show();

        setPropertyTable(editorPanel.c());

        stage.setMinHeight(menuBar.getMinHeight() + EDITOR_MIN_SIZE + 125);
        stage.setMinWidth(EDITOR_MIN_SIZE + LEFT_MIN_WIDTH + 75);
        mainBorderPane.setPrefSize(1250, 750);

        editorPanel.repaint(currProject.getCurrentCircuit());

        postSetProj();
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public Project getCurrProject() {
        return currProject;
    }

    public void postSetProj() {
        editorPanel.repaint(currProject.getCurrentCircuit());
        Circuit c = currProject.getCurrentCircuit();

        c.addEntity(new InputBlock(new CircuitPoint(13, -5, c), 0));
        c.addEntity(new InputBlock(new CircuitPoint(17, -5, c), 0));

        c.addEntity(new InputBlock(new CircuitPoint(23, -5, c), 0));
        c.addEntity(new InputBlock(new CircuitPoint(27, -5, c), 0));

        c.addEntity(new InputBlock(new CircuitPoint(33, -5, c), 0));
        c.addEntity(new InputBlock(new CircuitPoint(37, -5, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(-5, 5, c), 0));
        c.addEntity(new SimpleGateAND(new CircuitPoint(5, 5, c), 0));
        c.addEntity(new SimpleGateAND(new CircuitPoint(15, 5, c), 0));
        c.addEntity(new SimpleGateAND(new CircuitPoint(25, 5, c), 0));
        c.addEntity(new SimpleGateAND(new CircuitPoint(35, 5, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(20, 15, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(30, 15, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(20, 25, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(30, 25, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(20, 35, c), 0));

        c.addEntity(new SimpleGateAND(new CircuitPoint(30, 35, c), 0));


// 14, -9
        // 16, -9


        c.recalculateTransmissions();
        editorPanel.repaint(currProject.getCurrentCircuit());

    }

    private static final int TABLE_HEIGHT = 210;

    public void setPropertyTable(PropertyMutable dynamic) {
        TableView<Property> table = dynamic.getPropertyList().toTableView();
        table.setMaxHeight(210);
        table.setMinHeight(210);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        propertiesGoHere.setCenter(table);
        propertiesGoHere.setMinHeight(TABLE_HEIGHT);
    }
}
