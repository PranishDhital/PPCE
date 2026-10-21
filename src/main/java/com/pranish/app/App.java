package com.pranish.app;

import com.pranish.filetree.FileTree;
import com.pranish.highlighter.SyntaxHighlighter;
import com.pranish.toolmanager.ToolManager;
import com.pranish.terminal.TerminalPane;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class App extends Application {

    private enum RunMode {
        INTERPRETER("Interpreter (PPI)"),
        COMPILER("Compiler (PPC)");

        private final String label;

        RunMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private File currentFile = null;
    private TerminalPane terminal;

    @Override
    public void start(Stage stage) {

        // ---------- Editor ----------
        CodeArea editor = new CodeArea();
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
        editor.replaceText("""
                // for loop
                for (int i = 0; i < 5; i++)
                {
                // This prints the value of i
                    print(i);
                }
                printnl ("Hello, There!");
                """);
        editor.setStyle("""
                -fx-font-family: 'JetBrains Mono';
                -fx-font-size: 16px;
                -fx-background-color: #1e1e2e;
                -fx-control-inner-background: #1e1e2e;
                -fx-text-fill: #cdd6f4;
                -fx-highlight-fill: #45475a;
                -fx-highlight-text-fill: #cdd6f4;
                -fx-focus-color: transparent;
                -fx-faint-focus-color: transparent;
                """);
        editor.textProperty().addListener((obs, oldText, newText) ->
                editor.setStyleSpans(0, SyntaxHighlighter.highlight(newText)));
        editor.setStyleSpans(0, SyntaxHighlighter.highlight(editor.getText()));

        // ---------- Status label ----------
        Label status = new Label("Ready");
        status.getStyleClass().add("status-label");
        status.setPadding(new Insets(6, 10, 6, 10));

        // ---------- File tree ----------
        FileTree rootItem = new FileTree(new File(System.getProperty("user.home")));
        rootItem.setExpanded(true);

        TreeView<File> treeView = new TreeView<>(rootItem);
        treeView.setMinWidth(180);
        treeView.setCellFactory(tv -> new TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName().isEmpty() ? item.getPath() : item.getName());
                }
            }
        });
        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue().isFile()) {
                    openInEditor(selected.getValue(), editor, status, stage);
                }
            }
        });

        // ---------- Layout: editor + (hidden) terminal, tree on the right ----------
        VirtualizedScrollPane<CodeArea> editorPane = new VirtualizedScrollPane<>(editor);
        terminal = new TerminalPane();
        SplitPane editorColumn = new SplitPane(editorPane);   // terminal is added when toggled
        editorColumn.setOrientation(Orientation.VERTICAL);
        terminal.setOnClose(() -> {
            editorColumn.getItems().remove(terminal);
            terminal.stop();
            editor.requestFocus();
        });

        SplitPane splitPane = new SplitPane(editorColumn, treeView);
        splitPane.setDividerPositions(0.78);
        SplitPane.setResizableWithParent(treeView, false);

        // ---------- Terminal toggle ----------
        Runnable toggleTerminal = () -> {
            if (editorColumn.getItems().contains(terminal)) {
                editorColumn.getItems().remove(terminal);
                editor.requestFocus();
            } else {
                if (!terminal.isStarted()) {
                    File dir = currentFile != null && currentFile.getParentFile() != null
                            ? currentFile.getParentFile()
                            : new File(System.getProperty("user.home"));
                    terminal.start(dir);
                }
                editorColumn.getItems().add(terminal);
                editorColumn.setDividerPositions(0.7);
                terminal.focusInput();
            }
        };

        // ---------- Run action ----------
        Consumer<RunMode> runFile = mode -> {
            saveFile(stage, editor, status);
            if (currentFile == null) {
                status.setText("Save the file first");
                return;
            }
            if (!editorColumn.getItems().contains(terminal)) toggleTerminal.run();
            if (!terminal.isStarted()) terminal.start(currentFile.getParentFile());

            try {
                String label = mode == RunMode.INTERPRETER ? "interpreter" : "compiler";
                List<String> commands = buildRunCommands(currentFile, mode);
                terminal.clear();
                terminal.println("> run " + currentFile.getName() + " (" + label + ")");
                for (String cmd : commands) terminal.send(cmd);
                status.setText("Running with " + label + ": " + currentFile.getName());
            } catch (IOException ex) {
                status.setText("Could not run: " + ex.getMessage());
                terminal.println("[" + ex.getMessage() + "]");
            }
        };

        // ---------- Run tool chooser ----------
        Runnable openRunChooser = () -> {
            Stage chooser = new Stage(StageStyle.TRANSPARENT);
            chooser.initOwner(stage);
            chooser.initModality(javafx.stage.Modality.WINDOW_MODAL);

            Label heading = new Label("Run file");
            heading.getStyleClass().add("run-dialog-heading");
            Button closeButton = new Button("×");
            closeButton.getStyleClass().add("run-dialog-close");
            closeButton.setOnAction(event -> chooser.close());
            Region headerSpacer = new Region();
            HBox.setHgrow(headerSpacer, Priority.ALWAYS);
            HBox header = new HBox(heading, headerSpacer, closeButton);
            header.setAlignment(Pos.CENTER_LEFT);

            Label description = new Label("Choose which tool should run this file.");
            description.getStyleClass().add("run-dialog-description");

            ToggleGroup toolChoices = new ToggleGroup();
            RadioButton interpreterChoice = createRunChoice(RunMode.INTERPRETER, toolChoices);
            RadioButton compilerChoice = createRunChoice(RunMode.COMPILER, toolChoices);
            interpreterChoice.setSelected(true);
            VBox choices = new VBox(8, interpreterChoice, compilerChoice);

            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("run-dialog-cancel");
            cancelButton.setOnAction(event -> chooser.close());
            Button runButton = new Button("Run");
            runButton.getStyleClass().add("run-dialog-run");
            runButton.setDefaultButton(true);
            runButton.setOnAction(event -> {
                RadioButton selected = (RadioButton) toolChoices.getSelectedToggle();
                RunMode mode = (RunMode) selected.getUserData();
                chooser.close();
                runFile.accept(mode);
            });
            HBox actions = new HBox(8, cancelButton, runButton);
            actions.setAlignment(Pos.CENTER_RIGHT);

            VBox dialogContent = new VBox(16, header, description, choices, actions);
            dialogContent.getStyleClass().add("run-dialog");
            dialogContent.setPadding(new Insets(20));
            Scene chooserScene = new Scene(dialogContent);
            chooserScene.setFill(Color.TRANSPARENT);
            chooserScene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/Editor.css")).toExternalForm());
            chooser.setScene(chooserScene);
            chooser.setOnShown(event -> {
                chooser.setX(stage.getX() + (stage.getWidth() - chooser.getWidth()) / 2);
                chooser.setY(stage.getY() + (stage.getHeight() - chooser.getHeight()) / 2);
            });
            chooserScene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE) chooser.close();
            });
            chooser.showAndWait();
        };

        // ---------- File menu ----------
        Menu fileMenu = new Menu("File");

        MenuItem newFile = new MenuItem("New File");
        newFile.setOnAction(e -> {
            editor.clear();
            currentFile = null;
            stage.setTitle("PPCE");
            status.setText("New file");
        });

        MenuItem openFile = new MenuItem("Open File");
        openFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open File");
            fc.setInitialDirectory(new File(System.getProperty("user.home")));
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("PD Files", "*.pd"));
            File file = fc.showOpenDialog(stage);
            if (file != null) openInEditor(file, editor, status, stage);
        });

        MenuItem openFolder = new MenuItem("Open Folder");
        openFolder.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Open Folder");
            dc.setInitialDirectory(new File(System.getProperty("user.home")));
            File dir = dc.showDialog(stage);
            if (dir != null) {
                FileTree newRoot = new FileTree(dir);
                newRoot.setExpanded(true);
                treeView.setRoot(newRoot);
                terminal.start(dir);
                status.setText("Folder: " + dir.getAbsolutePath());
            }
        });

        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> saveFile(stage, editor, status));
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

        MenuItem saveAs = new MenuItem("Save As...");
        saveAs.setOnAction(e -> saveFileAs(stage, editor, status));
        saveAs.setAccelerator(new KeyCodeCombination(KeyCode.S,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(newFile, openFile, openFolder, save, saveAs, exit);

        // ---------- View menu ----------
        Menu viewMenu = new Menu("View");
        MenuItem toggleTerminalItem = new MenuItem("Toggle Terminal (Ctrl+`)");
        toggleTerminalItem.setOnAction(e -> toggleTerminal.run());
        viewMenu.getItems().add(toggleTerminalItem);

        MenuItem aboutItem = new MenuItem("About Us");
        aboutItem.setOnAction(e -> aboutsec(stage));
        viewMenu.getItems().add(aboutItem);

        Menu runMenu = new Menu("Run");
        MenuItem runItem = new MenuItem("Run");
        runItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        runItem.setOnAction(e -> openRunChooser.run());
        runMenu.getItems().add(runItem);

        MenuBar menuBar = new MenuBar(fileMenu, viewMenu, runMenu);

        Label appName = new Label("PPCE");
        appName.getStyleClass().add("app-title-name");
        HBox brandGroup = new HBox(appName);
        brandGroup.setAlignment(Pos.CENTER_LEFT);

        Label documentTitle = new Label("Untitled");
        documentTitle.getStyleClass().add("document-title");
        stage.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            String prefix = "PPCE - ";
            documentTitle.setText(newTitle != null && newTitle.startsWith(prefix)
                    ? newTitle.substring(prefix.length()) : "Untitled");
        });

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        Button minimizeButton = new Button("—");
        Button maximizeButton = new Button("□");
        Button closeWindowButton = new Button("×");
        minimizeButton.getStyleClass().add("window-control-button");
        maximizeButton.getStyleClass().add("window-control-button");
        closeWindowButton.getStyleClass().addAll("window-control-button", "window-close-button");
        minimizeButton.setOnAction(e -> stage.setIconified(true));
        maximizeButton.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        closeWindowButton.setOnAction(e -> stage.close());
        stage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) ->
                maximizeButton.setText(isMaximized ? "❐" : "□"));
        HBox windowControls = new HBox(minimizeButton, maximizeButton, closeWindowButton);
        windowControls.getStyleClass().add("window-controls");
        HBox windowTitleBar = new HBox(14, brandGroup, documentTitle, titleSpacer, windowControls);
        windowTitleBar.setAlignment(Pos.CENTER_LEFT);
        windowTitleBar.getStyleClass().add("window-title-bar");
        windowTitleBar.setOnMousePressed(event -> {
            if (event.getClickCount() == 2) {
                stage.setMaximized(!stage.isMaximized());
            } else if (!(event.getTarget() instanceof Button) && !stage.isMaximized()) {
                windowTitleBar.setUserData(new double[]{event.getScreenX() - stage.getX(),
                        event.getScreenY() - stage.getY()});
            }
        });
        windowTitleBar.setOnMouseDragged(event -> {
            if (!(event.getTarget() instanceof Button) && !stage.isMaximized()
                    && windowTitleBar.getUserData() instanceof double[] dragOffset) {
                stage.setX(event.getScreenX() - dragOffset[0]);
                stage.setY(event.getScreenY() - dragOffset[1]);
            }
        });

        // ---------- Status bar ----------
        Label version = new Label("v1.0.0");
        version.getStyleClass().add("version-label");
        version.setPadding(new Insets(6, 10, 6, 10));

        Label languages = new Label("PPC & PPI");
        languages.getStyleClass().add("status-label");
        languages.setPadding(new Insets(6, 10, 6, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(status, spacer, languages, version);
        statusBar.getStyleClass().add("status-bar");

        // ---------- Root, scene, stage ----------
        BorderPane root = new BorderPane();
        root.setTop(new VBox(windowTitleBar, menuBar));
        root.setCenter(splitPane);
        root.setBottom(statusBar);
        root.setStyle("-fx-background-color: #181825;");

        String[] resizeEdge = {""};
        double[] resizeStart = new double[4]; // screen X, screen Y, width, height
        root.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (stage.isMaximized()) {
                resizeEdge[0] = "";
                root.setCursor(Cursor.DEFAULT);
                return;
            }
            double x = event.getX(), y = event.getY();
            boolean left = x <= 6, right = x >= root.getWidth() - 6;
            boolean top = y <= 6, bottom = y >= root.getHeight() - 6;
            resizeEdge[0] = (top ? "N" : bottom ? "S" : "") + (left ? "W" : right ? "E" : "");
            Cursor cursor = switch (resizeEdge[0]) {
                case "N", "S" -> Cursor.V_RESIZE;
                case "E", "W" -> Cursor.H_RESIZE;
                case "NE" -> Cursor.NE_RESIZE;
                case "SW" -> Cursor.SW_RESIZE;
                case "NW" -> Cursor.NW_RESIZE;
                case "SE" -> Cursor.SE_RESIZE;
                default -> Cursor.DEFAULT;
            };
            root.setCursor(cursor);
        });
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!resizeEdge[0].isEmpty()) {
                resizeStart[0] = event.getScreenX();
                resizeStart[1] = event.getScreenY();
                resizeStart[2] = stage.getWidth();
                resizeStart[3] = stage.getHeight();
                event.consume();
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (resizeEdge[0].isEmpty() || stage.isMaximized()) return;
            double dx = event.getScreenX() - resizeStart[0];
            double dy = event.getScreenY() - resizeStart[1];
            if (resizeEdge[0].contains("E")) stage.setWidth(Math.max(stage.getMinWidth(), resizeStart[2] + dx));
            if (resizeEdge[0].contains("S")) stage.setHeight(Math.max(stage.getMinHeight(), resizeStart[3] + dy));
            if (resizeEdge[0].contains("W")) {
                double width = Math.max(stage.getMinWidth(), resizeStart[2] - dx);
                stage.setX(resizeStart[0] + resizeStart[2] - width);
                stage.setWidth(width);
            }
            if (resizeEdge[0].contains("N")) {
                double height = Math.max(stage.getMinHeight(), resizeStart[3] - dy);
                stage.setY(resizeStart[1] + resizeStart[3] - height);
                stage.setHeight(height);
            }
            event.consume();
        });
        root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> resizeEdge[0] = "");

        Scene scene = new Scene(root, 1100, 700, Color.web("#181825"));
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/Editor.css")).toExternalForm());

        // Ctrl + ` toggles the terminal (works even when the editor has focus)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_QUOTE && event.isControlDown()
                    && !event.isAltDown() && !event.isMetaDown()) {
                event.consume();
                toggleTerminal.run();
            }
        });

        stage.setTitle("PPCE");
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() {
        if (terminal != null) terminal.stop();
    }

    // ================= Run helpers =================

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static String quote(String s) {
        return isWindows()
                ? "'" + s.replace("'", "''") + "'"
                : "'" + s.replace("'", "'\\''") + "'";
    }

    private static RadioButton createRunChoice(RunMode mode, ToggleGroup group) {
        RadioButton choice = new RadioButton(mode.toString());
        choice.setToggleGroup(group);
        choice.setUserData(mode);
        choice.getStyleClass().add("run-dialog-choice");
        return choice;
    }

    private List<String> buildRunCommands(File file, RunMode mode) throws IOException {
        String p = isWindows() ? "& " : "";
        String src = file.getAbsolutePath();

        if (mode == RunMode.INTERPRETER) {
            Path ppi = ToolManager.get("ppi");
            return List.of(p + quote(ppi.toString()) + " " + quote(src));
        }

        Path ppc = ToolManager.get("ppc");
        String out = src.replaceAll("\\.[^.\\\\/]+$", "") + (isWindows() ? ".exe" : ".out");
        String check = isWindows()
                ? "if (Test-Path " + quote(out) + ") { 'Compiled: " + out.replace("'", "''")
                  + "' } else { 'Compilation failed.' }"
                : "if [ -f " + quote(out) + " ]; then echo 'Compiled: " + out.replace("'", "'\\''")
                  + "'; else echo 'Compilation failed.'; fi";
        return List.of(
                p + quote(ppc.toString()) + " " + quote(src) + " -o " + quote(out), // adjust flags to your compiler
                check
        );
    }

    // ================= File helpers =================

    private void openInEditor(File file, CodeArea editor, Label status, Stage stage) {
        try {
            editor.replaceText(Files.readString(file.toPath()));
            currentFile = file;
            stage.setTitle("PPCE - " + file.getName());
            status.setText("Opened: " + file.getName());
        } catch (IOException ex) {
            status.setText("Could not open: " + ex.getMessage());
        }
    }

    private void saveFile(Stage stage, CodeArea editor, Label status) {
        if (currentFile == null) {
            saveFileAs(stage, editor, status);
            return;
        }
        writeToFile(currentFile, editor, status, stage);
    }

    private void saveFileAs(Stage stage, CodeArea editor, Label status) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PPC Files", "*.ppc", "*.ppi"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        if (currentFile != null && currentFile.getParentFile() != null) {
            chooser.setInitialDirectory(currentFile.getParentFile());
            chooser.setInitialFileName(currentFile.getName());
        }
        File file = chooser.showSaveDialog(stage);
        if (file != null) writeToFile(file, editor, status, stage);
    }

    private void writeToFile(File file, CodeArea editor, Label status, Stage stage) {
        try {
            Files.writeString(file.toPath(), editor.getText());
            currentFile = file;
            status.setText("Saved: " + file.getName());
            stage.setTitle("PPCE - " + file.getName());
        } catch (IOException ex) {
            status.setText("Could not save: " + ex.getMessage());
        }
    }

    private void aboutsec(Stage owner) {
        Stage about = new Stage(StageStyle.TRANSPARENT);
        about.initOwner(owner);
        about.initModality(javafx.stage.Modality.WINDOW_MODAL);

        Label heading = new Label("About PPCE");
        heading.getStyleClass().add("run-dialog-heading");
        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("run-dialog-close");
        closeButton.setOnAction(event -> about.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(heading, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        Label description = new Label("PPCE is an editor for the .pd programming language.");
        description.getStyleClass().add("run-dialog-description");
        Label componentsHeading = new Label("The .pd toolchain");
        componentsHeading.getStyleClass().add("about-section-heading");

        VBox projects = new VBox(10,
                createProjectLink("PPCE", "The .pd language editor", "https://github.com/PranishDhital/PPCE"),
                createProjectLink("PPC", "The .pd compiler", "https://github.com/PranishDhital/PPC"),
                createProjectLink("PPI", "The .pd interpreter", "https://github.com/PranishDhital/PDLex"));

        VBox content = new VBox(16, header, description, componentsHeading, projects);
        content.getStyleClass().addAll("run-dialog", "about-dialog");
        content.setPadding(new Insets(20));
        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/Editor.css")).toExternalForm());
        about.setScene(scene);
        about.setTitle("About PPCE");
        about.setOnShown(event -> {
            about.setX(owner.getX() + (owner.getWidth() - about.getWidth()) / 2);
            about.setY(owner.getY() + (owner.getHeight() - about.getHeight()) / 2);
        });
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) about.close();
        });
        about.showAndWait();
    }

    private Hyperlink createProjectLink(String name, String description, String url) {
        Hyperlink link = new Hyperlink(name + "  —  " + description);
        link.getStyleClass().add("about-project-link");
        link.setOnAction(event -> getHostServices().showDocument(url));
        return link;
    }
}
