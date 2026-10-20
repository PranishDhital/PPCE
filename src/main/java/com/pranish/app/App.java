package com.pranish.app;
import com.pranish.highlighter.SyntaxHighlighter;
import com.pranish.filetree.FileTree;
import com.pranish.terminal.TerminalPane;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.IOException;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

public class App extends Application {
    private File currentFile = null;
    private TerminalPane terminal;
    @Override
    public void start(Stage stage) {


        CodeArea editor = new CodeArea();
        File rootFile = new File(System.getProperty("user.home"));
        FileTree rootItem = new FileTree(rootFile);
        rootItem.setExpanded(true);
        TreeView<File> treeView = new TreeView<>(rootItem);

        Label status = new Label("Ready");
        editor.setParagraphGraphicFactory(
                LineNumberFactory.get(editor)
        );

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

        Menu fileMenu = new Menu("File");

        MenuItem newFile = new MenuItem("New File");
        newFile.setOnAction(event -> editor.clear());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        MenuItem openFile = new MenuItem("Open File");
        openFile.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open File");

            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("Text Files", "*.pd"),
                    new FileChooser.ExtensionFilter("Text Files", "*.*")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                openInEditor(file,editor,status);
            }
        });

        MenuItem openFolder = new MenuItem("Open Folder");
        openFolder.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Open Folder");

            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File dir = directoryChooser.showDialog(stage);
            if (dir != null) {
                FileTree newRoot = new FileTree(dir);
                newRoot.setExpanded(true);
                treeView.setRoot(newRoot);
                status.setText("Folder: " + dir.getAbsolutePath());
                terminal.start(dir);
            }
        });

        MenuItem save = new MenuItem("Save");
        save.setOnAction(e -> saveFile(stage, editor, status));
        save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

        fileMenu.getItems().addAll(
                newFile,
                openFile,
                openFolder,
                save,
                exit
        );

        Button runButton = new Button("▶ Run");
        Label title = new Label("PPCE");
        title.setStyle("-fx-font-weight: bold;");

        ToolBar toolbar = new ToolBar(
                title,
                new Separator(),
                runButton
        );


        status.getStyleClass().add("status-label");
        status.setPadding(new Insets(6, 10, 6, 10));

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("version-label");
        version.setPadding(new Insets(6, 10, 6, 10));

        Label languages = new Label("PPC & PPI");
        languages.getStyleClass().add("status-label");
        languages.setPadding(new Insets(6, 10, 6, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(status, spacer, languages,version);
        statusBar.getStyleClass().add("status-bar");

        Menu viewMenu = new Menu("View");
        MenuItem toggleTerminal = new MenuItem("Terminal");
        toggleTerminal.setAccelerator(new KeyCodeCombination(
                KeyCode.BACK_QUOTE, KeyCombination.CONTROL_DOWN));
        viewMenu.getItems().add(toggleTerminal);
        MenuBar menuBar = new MenuBar(fileMenu, viewMenu);
        VBox top = new VBox(menuBar, toolbar);

        terminal = new TerminalPane();
        terminal.setVisible(false);
        terminal.setManaged(false);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setBottom(statusBar);

        root.setStyle("-fx-background-color: #181825;");

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/editor.css")).toExternalForm()
        );


        editor.textProperty().addListener((obs, oldText, newText) ->
                editor.setStyleSpans(0, SyntaxHighlighter.highlight(newText))
        );

        // highlight any initial text too
        editor.setStyleSpans(0, SyntaxHighlighter.highlight(editor.getText()));


        treeView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                TreeItem<File> selected = treeView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue().isFile()) {
                    openInEditor(selected.getValue(), editor, status);
                }
            }
        });

        // show the file name only insyead of the entire path
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

        VirtualizedScrollPane<CodeArea> editorPane = new VirtualizedScrollPane<>(editor);

        VBox editorColumn = new VBox(editorPane, terminal);
        VBox.setVgrow(editorPane, Priority.ALWAYS);
        VBox.setVgrow(terminal, Priority.SOMETIMES);

        SplitPane splitPane = new SplitPane(editorColumn, treeView);
        splitPane.setDividerPositions(0.5);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        root.setCenter(splitPane);

        Runnable toggleTerminalAction = () -> {
            boolean opening = !terminal.isVisible();
            terminal.setManaged(opening);
            terminal.setVisible(opening);
            if (opening) {
                if (!terminal.isStarted()) {
                    File workingDir = currentFile != null && currentFile.getParentFile() != null
                            ? currentFile.getParentFile()
                            : new File(System.getProperty("user.home"));
                    terminal.start(workingDir);
                }
                terminal.focusInput();
            } else {
                editor.requestFocus();
            }
        };
        toggleTerminal.setOnAction(event -> toggleTerminalAction.run());

        runButton.setOnAction(event -> {
            if (!terminal.isVisible()) {
                toggleTerminal.fire();
            }
            if (currentFile != null) {
                terminal.send(currentFile.getAbsolutePath());
            }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_QUOTE && event.isControlDown()
                    && !event.isAltDown() && !event.isMetaDown()) {
                event.consume();
                toggleTerminal.fire();
            }
        });



        stage.setTitle("PPCE");
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }

    // helps to open in editor
    private void openInEditor(File file, CodeArea editor, Label status) {
        try {
            editor.replaceText(Files.readString(file.toPath()));
            currentFile = file;
            status.setText("Opened: " + file.getName());
        } catch (java.io.IOException ex) {
            status.setText("Could not open: " + ex.getMessage());
        }
    }

    // save the file
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
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        if (currentFile != null && currentFile.getParentFile() != null) {
            chooser.setInitialDirectory(currentFile.getParentFile());
            chooser.setInitialFileName(currentFile.getName());
        }

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            writeToFile(file, editor, status, stage);
        }

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
    @Override
    public void stop() {
        if (terminal != null) terminal.stop();
    }
}
