package edu.wit.yeatesg.logicgates.gui;

import edu.wit.yeatesg.logicgates.circuit.entity.Entity;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyList;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.OutputBlock;
import edu.wit.yeatesg.logicgates.circuit.entity.PropertyMutable;
import edu.wit.yeatesg.logicgates.circuit.entity.Property;
import edu.wit.yeatesg.logicgates.circuit.Circuit;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.InputBlock;
import edu.wit.yeatesg.logicgates.datatypes.CircuitPoint;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

    private boolean addDefaultEntities;


    public static void main(String[] args) {
        Platform.runLater(() -> {
            new MainGUI(new Stage(), null, true);
        });
    }

    public MainGUI(Stage stage, Project project, boolean addDefaultEntities) {
        this.addDefaultEntities = addDefaultEntities;
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
        return open ? fileChooser.showOpenDialog(stage) : fileChooser.showSaveDialog(stage);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
    }

    private void onMenuSavePress(ActionEvent e, boolean saveAs) {
        if (saveAs)
            currProject.toFile(getFileFromFileDialog(stage, false));

    }

    public void onMenuOpenPress(ActionEvent ev) {
        File f = getFileFromFileDialog(stage, true);
        Project proj;
        try {
            proj = Project.fromFile(f);
            YesNoGUI yesNoGUI = new YesNoGUI(new Stage(), "Option", "Open Project In New Window?");
            yesNoGUI.setNoAction((e) -> {
                // TODO ask if they want to save
                setCurrentProject(proj);
            });
            yesNoGUI.setYesAction((e) -> Platform.runLater(() -> new MainGUI(new Stage(), proj, false)));
            yesNoGUI.showAndWait();
        } catch (Project.LoadFailedException e) {
            // TODO show "Load Failed" dialog bs
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public MenuBar getMenuBar() {
        return menuBar;
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

        Scene scene = new Scene(mainBorderPane, 1250, 750, true, SceneAntialiasing.DISABLED);
        stage.setScene(scene);
        stage.setTitle("Logic Gates");
        stage.show();
    }

    private void initMenuBar() {
        menuBar = new MenuBar();
        mainBorderPane.setTop(menuBar);
        initFileMenu();
        initEditMenu();
    }

    private Menu fileMenu;
    private MenuItem openMenuItem;

    private void initFileMenu() {
        fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open Project\n");
        openMenuItem.setOnAction(this::onMenuOpenPress);
        fileMenu.getItems().add(openMenuItem);
        MenuItem saveMenuItem = new MenuItem("Save Project As...");
        saveMenuItem.setOnAction((e) -> onMenuSavePress(e, true));
        fileMenu.getItems().add(saveMenuItem);
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
            editorPanel.userCTRLZ(false);
        });
            // TODO CALL UNDO FUNCTION

        megaUndoMenuItem = new MenuItem("Mega Undo  ");
        megaUndoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().add(megaUndoMenuItem);
        megaUndoMenuItem.setOnAction((e) -> {
            editorPanel.userCTRLZ(true);
        });

        redoMenuItem = new MenuItem("Redo  ");
        redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(redoMenuItem);
        redoMenuItem.setOnAction((e) -> {
            editorPanel.userCTRLY(false);
        });

        megaRedoMenuItem = new MenuItem("Mega Redo  ");
        megaRedoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        editMenu.getItems().add(megaRedoMenuItem);
        megaRedoMenuItem.setOnAction((e) -> {
            editorPanel.userCTRLY(true);
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
        TreeItem<String> treeRoot = new TreeItem<>("untitled");
        treeRoot.setExpanded(true);

        TreeItem<String> circuitsItem = new TreeItem<>("Circuits");
        circuitsItem.setExpanded(true);
        circuitsItem.getChildren().add(new TreeItem<>("CircuitA"));
        circuitsItem.getChildren().add(new TreeItem<>("CircuitB"));
        circuitsItem.getChildren().add(new TreeItem<>("CircuitC"));

        TreeItem<String> objectsItem = new TreeItem<>("Objects");
        objectsItem.setExpanded(true);

        TreeItem<String> logicGatesItem = new TreeItem<>("Logic Gates");

        TreeItem<String> andItem = new TreeItem<>("AND Gate");
        andItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/ANDIcon.png")));
        logicGatesItem.getChildren().add(andItem);

        TreeItem<String> nandItem = new TreeItem<>("NAND Gate");
        nandItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/NANDIcon.png")));
        logicGatesItem.getChildren().add(nandItem);

        TreeItem<String> orItem = new TreeItem<>("OR Gate");
        orItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/ORIcon.png")));
        logicGatesItem.getChildren().add(orItem);

        TreeItem<String> norItem = new TreeItem<>("NOR Gate");
        norItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/NORIcon.png")));
        logicGatesItem.getChildren().add(norItem);

        TreeItem<String> xorItem = new TreeItem<>("XOR Gate");
        xorItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/XORIcon.png")));
        logicGatesItem.getChildren().add(xorItem);

        TreeItem<String> xnorItem = new TreeItem<>("XNOR Gate");
        xnorItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/XNORIcon.png")));
        logicGatesItem.getChildren().add(xnorItem);
        logicGatesItem.setExpanded(true);

        objectsItem.getChildren().add(logicGatesItem);

        TreeItem<String> item3 = new TreeItem<>("Wiring");
        treeRoot.getChildren().add(circuitsItem);
        treeRoot.getChildren().add(objectsItem);
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

        setPropertyTable(editorPanel.c());

        stage.setMinHeight(menuBar.getMinHeight() + EDITOR_MIN_SIZE + 125);
        stage.setMinWidth(EDITOR_MIN_SIZE + LEFT_MIN_WIDTH + 75);
        mainBorderPane.setPrefSize(1250, 750);

        editorPanel.repaint();

        postSetProj();
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public Project getCurrProject() {
        return currProject;
    }

    public void postSetProj() {
        editorPanel.repaint();
        Circuit c = currProject.getCurrentCircuit();
        if (addDefaultEntities) {
            addDefaultEntities = false;

            c.addEntity(new GateAND(new CircuitPoint(0, 5, c), 0, Entity.Size.NORMAL, 5));

            c.addEntity(new GateXOR(new CircuitPoint(17, 5, c), 0, Entity.Size.NORMAL, 5));

            c.addEntity(new GateOR(new CircuitPoint(34, 5, c), 0, Entity.Size.NORMAL, 5));
            c.addEntity(new GateOR(new CircuitPoint(40, 5, c), 0, Entity.Size.SMALL, 3));
            c.addEntity(new GateXOR(new CircuitPoint(44, 5, c), 0, Entity.Size.SMALL, 3));
            c.addEntity(new GateAND(new CircuitPoint(48, 5, c), 0, Entity.Size.SMALL, 3));

            c.addEntity(new OutputBlock(new CircuitPoint(-2, -5, c), 0));
            c.addEntity(new InputBlock(new CircuitPoint(2, -5, c), 0));

            c.addEntity(new GateNOT(new CircuitPoint(2, -10, c), 0));
            c.addEntity(new Transistor(new CircuitPoint(2, -15, c), 0));
            c.addEntity(new Transistor(new CircuitPoint(-2, -15, c), 0, true));

            c.addEntity(new PullResistor(new CircuitPoint(-2, -20, c), 0));

            c.addEntity(new PowerEmitter(new CircuitPoint(-2, -25, c), 0));
            c.addEntity(new GroundEmitter(new CircuitPoint(2, -25, c), 0));



        }

        









    /*    c.addEntity(new SimpleGateAND(new CircuitPoint(-1, 10, c), 0));
        c.addEntity(new SimpleGateAND(new CircuitPoint(-2, 8, c), 0));
        c.addEntity(new SimpleGateAND(new CircuitPoint(-4, 6, c), 0));
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

        c.addEntity(new SimpleGateAND(new CircuitPoint(30, 35, c), 0));*/


// 14, -9
        // 16, -9


        c.recalculateTransmissions();
        editorPanel.repaint();

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

    public void setPropertyTable(PropertyList properties) {
        TableView<Property> table = properties.toTableView();
        table.setMaxHeight(210);
        table.setMinHeight(210);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        propertiesGoHere.setCenter(table);
        propertiesGoHere.setMinHeight(TABLE_HEIGHT);
    }

}
