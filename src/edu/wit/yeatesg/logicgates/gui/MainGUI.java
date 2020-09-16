package edu.wit.yeatesg.logicgates.gui;

import com.sun.javafx.application.PlatformImpl;
import edu.wit.yeatesg.logicgates.circuit.entity.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.logicgate.*;
import edu.wit.yeatesg.logicgates.circuit.entity.connectible.peripheral.OutputBlock;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;


public class MainGUI extends Application {

    private Stage stage;
    private Project currProject;

    private boolean addDefaultEntities;

    public static void main(String[] args) {
        PlatformImpl.startup(() -> {});
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
        fileChooser.setTitle(open ? "Open File" : "Save To File");

        File f = new File(System.getProperty("user.home")
                + File.separator + "Documents"
                + File.separator + "Logic Gates"
                + File.separator + "Projects");
        f.mkdirs();
        fileChooser.setInitialDirectory(f);
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Circuit", "*.cxml"));
        return open ? fileChooser.showOpenDialog(stage) : fileChooser.showSaveDialog(stage);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
    }


    public void saveAs() {
        File f = getFileFromFileDialog(stage, false);
        if (f != null) {
            currProject.setPath(f.getPath());
            currProject.writeToFile(f);
        }
    }

    public void save() {
        if (currProject.getPath() == null) {
            saveAs();
            return;
        }
        File f = new File(currProject.getPath());
        if (f.exists() && !f.isDirectory()) {
            if (f.delete()) {
                try {
                    if (!f.createNewFile())
                        f = new File(currProject.getPath());
                } catch (IOException e) {
                    f = new File(currProject.getPath());
                }
            }
        }
        currProject.writeToFile(f);
    }

    public void onMenuOpenPress(ActionEvent ev) {
        File f = getFileFromFileDialog(stage, true);
        if (f != null) {
            Project proj;
            try {
                proj = Project.fromFile(f);
                YesNoGUI yesNoGUI = new YesNoGUI(new Stage(), "Option", "Open Project In New Window?");
                yesNoGUI.setNoAction((e) -> {
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
                private BorderPane buttonBorderPane;
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
    private MenuItem saveMenuItem;

    public MenuItem getOpenMenuItem() {
        return openMenuItem;
    }

    public MenuItem getSaveMenuItem() {
        return saveMenuItem;
    }

    private void initFileMenu() {
        fileMenu = new Menu("File");
        MenuItem openMenuItem = new MenuItem("Open Project\n");
        openMenuItem.setOnAction(this::onMenuOpenPress);
        fileMenu.getItems().add(openMenuItem);
        MenuItem saveAsMenuItem = new MenuItem("Save As");
        saveAsMenuItem.setOnAction((e) -> saveAs());
        fileMenu.getItems().add(saveAsMenuItem);

        saveMenuItem = new MenuItem("Save");
        saveMenuItem.setOnAction((e) -> save());
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

        megaUndoMenuItem = new MenuItem("Mega Unldo  ");
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
        buttonBorderPane = new BorderPane();
        buttonBorderPane.setCenter(new Label("Buttons and stuff go here"));
        buttonBorderPane.setMinHeight(100);
        leftOfDivider.getChildren().add(buttonBorderPane);

        // The middle of the VBox is the treeView to choose entities to add to the Circuit
        treeView = new TreeView<>();
        treeView.setMinHeight(150); // The listener below makes it so the treeView is always as big as it can get
        stage.heightProperty().addListener((observableValue, number, t1) -> treeView.setPrefHeight(Integer.MAX_VALUE));
        leftOfDivider.getChildren().add(treeView);
        TreeItem<String> treeRoot = new TreeItem<>("untitled");
        treeRoot.setExpanded(true);

        TreeItem<String> circuitAItem = new TreeItem<>("Circuit A");
        circuitAItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/CircuitIcon.png")));
        treeRoot.getChildren().add(circuitAItem);

        TreeItem<String> circuitBItem = new TreeItem<>("Circuit B");
        circuitBItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/CircuitIcon.png")));
        treeRoot.getChildren().add(circuitBItem);

        TreeItem<String> circuitCItem = new TreeItem<>("Circuit C");
        circuitCItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/CircuitIcon.png")));
        treeRoot.getChildren().add(circuitCItem);

        TreeItem<String> wiringItem = new TreeItem<>("Wiring");
        treeRoot.getChildren().add(wiringItem);
        wiringItem.setExpanded(true);

        Circuit t = new Circuit();
        CircuitPoint o = new CircuitPoint(0, 0, t);

        EntityTreeItem inputBlockItem = new EntityTreeItem(new InputBlock(o, 270), "Input Block");

        inputBlockItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/InputBlockIcon.png")));
        wiringItem.getChildren().add(inputBlockItem);

        EntityTreeItem outputBlockItem = new EntityTreeItem(new OutputBlock(o, 90), "Output Block");
        outputBlockItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/OutputBlockIcon.png")));
        wiringItem.getChildren().add(outputBlockItem);

        EntityTreeItem pullUpResistorItem = new EntityTreeItem(new PullResistor(o, 270, 1), "Pull Up Resistor");
        pullUpResistorItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/PullUpResistorIcon.png")));
        wiringItem.getChildren().add(pullUpResistorItem);

        EntityTreeItem pullDownResistorItem = new EntityTreeItem(new PullResistor(o, 270, 0), "Pull Down Resistor");
        pullDownResistorItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/PullDownResistorIcon.png")));
        wiringItem.getChildren().add(pullDownResistorItem);

        EntityTreeItem powerIcon = new EntityTreeItem(new PowerEmitter(o), "Power");
        powerIcon.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/PowerIcon.png")));
        wiringItem.getChildren().add(powerIcon);

        EntityTreeItem groundIcon = new EntityTreeItem(new GroundEmitter(o), "Ground");
        groundIcon.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/GroundIcon.png")));
        wiringItem.getChildren().add(groundIcon);

        EntityTreeItem pTypeTransistorIcon = new EntityTreeItem(new Transistor(o, 0, false), "P Type Transistor");
        pTypeTransistorIcon.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/pTypeTransistorIcon.png")));
        wiringItem.getChildren().add(pTypeTransistorIcon);

        EntityTreeItem nTypeTransistorIcon = new EntityTreeItem(new Transistor(o, 0, true), "N Type Transistor");
        nTypeTransistorIcon.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/nTypeTransistorIcon.png")));
        wiringItem.getChildren().add(nTypeTransistorIcon);

        TreeItem<String> logicGatesItem = new TreeItem<>("Logic Gates");
        logicGatesItem.setExpanded(true);
        treeRoot.getChildren().add(logicGatesItem);

        EntityTreeItem andItem = new EntityTreeItem(new GateAND(o, 270, Entity.Size.NORMAL, 2, false), "AND Gate");
        andItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/ANDIcon.png")));
        logicGatesItem.getChildren().add(andItem);

        EntityTreeItem nandItem = new EntityTreeItem(new GateAND(o, 270, Entity.Size.NORMAL, 2, true), "NAND Gate");
        nandItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/NANDIcon.png")));
        logicGatesItem.getChildren().add(nandItem);

        EntityTreeItem orItem = new EntityTreeItem(new GateOR(o, 270, Entity.Size.NORMAL, 2, false), "OR Gate");
        orItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/ORIcon.png")));
        logicGatesItem.getChildren().add(orItem);

        EntityTreeItem norItem = new EntityTreeItem(new GateOR(o, 270, Entity.Size.NORMAL, 2, true), "NOR Gate");
        norItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/NORIcon.png")));
        logicGatesItem.getChildren().add(norItem);

        EntityTreeItem xorItem = new EntityTreeItem(new GateXOR(o, 270, Entity.Size.NORMAL, 2, false), "XOR Gate");
        xorItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/XORIcon.png")));
        logicGatesItem.getChildren().add(xorItem);

        EntityTreeItem xnorItem = new EntityTreeItem(new GateXOR(o, 270, Entity.Size.NORMAL, 2, true), "XNOR Gate");
        xnorItem.setGraphic(new ImageView(new Image("/assets/Logic Gate Icons/XNORIcon.png")));
        logicGatesItem.getChildren().add(xnorItem);

        treeView.setRoot(treeRoot);
        treeView.setEditable(false);
        treeView.setCellFactory(p -> new CustomTreeCell());

        // Property table
        propertiesGoHere = new BorderPane();

        propertiesGoHere.setCenter(new Label("Item Properties and shit go here"));
        leftOfDivider.getChildren().add(propertiesGoHere);
    }

    private final class CustomTreeCell extends TreeCell<String> {

        public CustomTreeCell() {
            addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
                TreeItem<String> myItem = getTreeItem();
                if (myItem instanceof EntityTreeItem)
                    ((EntityTreeItem) myItem).onClick();
            });
        }

        @Override
        protected void updateItem(String s, boolean empty) {
            super.updateItem(s, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            }
            else {
                setText(getTreeItem().getValue());
                setGraphic(getTreeItem().getGraphic());
            }
        }
    }
    
    public Circuit c() {
        return currProject.getCurrentCircuit();
    }

    public class EntityTreeItem extends TreeItem<String> {

        private Entity entity;

        public EntityTreeItem(Entity entity, String string) {
            super(string);
            entity.setTreeItem(this);
            this.entity = entity;
        }

        public void onClick() {
            Circuit c = c();
            c.deselectAllAndTrack();
            c.appendCurrentStateChanges("Deselect All");
            editorPanel.cancelCurrentPlacement();
            editorPanel.previewPlacementNextFocusGain(entity);
            setPropertyTable(entity);
            editorPanel.repaint();
        }
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
        p.setGUI(this);
        this.currProject = p;
        if (!currProject.hasCircuits()) {
            new Circuit(currProject, "main");
        }

        final int EDITOR_MIN_SIZE = 500;
        editorPanel = new EditorPanel(currProject);
        int height = 700;
        int width = 700;
        editorPanel.setPrefSize(width, height);
        editorPanel.setMinSize(EDITOR_MIN_SIZE, EDITOR_MIN_SIZE);
        // Bind canvas size to stack pane size.

        rightOfDivider.setCenter(editorPanel);

        setPropertyTable(editorPanel.c());

        stage.setMinHeight(menuBar.getMinHeight() + EDITOR_MIN_SIZE + 125);
        stage.setMinWidth(EDITOR_MIN_SIZE + LEFT_MIN_WIDTH + 75);
        mainBorderPane.setPrefSize(1250, 750);


        editorPanel.repaint();
        updateSaveMenuBars();
        currProject.getCurrentCircuit().stateController().updateMenuBars();
        currProject.getCurrentCircuit().selectionTableUpdate();
        currProject.getCurrentCircuit().setXOffset(width / 2);
        currProject.getCurrentCircuit().setYOffset(height / 2);
    }

    public EditorPanel getEditorPanel() {
        return editorPanel;
    }

    public Project getCurrProject() {
        return currProject;
    }

    private static final int TABLE_HEIGHT = 210;

    public void setPropertyTable(PropertyMutable dynamic) {
        setPropertyTable(dynamic.getPropertyList());
    }

    public void setPropertyTable(PropertyList properties) {
        TableView<Property> table = properties.toTableView();
        table.setMaxHeight(210);
        table.setMinHeight(210);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        propertiesGoHere.setCenter(table);
        propertiesGoHere.setMinHeight(TABLE_HEIGHT);
    }

    public void updateSaveMenuBars() {
        String path = currProject.getPath();
        if (path == null) {
            saveMenuItem.setDisable(true);
            saveMenuItem.setText("Save");
        } else {
            saveMenuItem.setDisable(false);
            saveMenuItem.setText("Overwrite " + path);
        }
    }
}
