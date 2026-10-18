package com.pranish.app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) {


        CodeArea editor = new CodeArea();

        editor.setParagraphGraphicFactory(
                LineNumberFactory.get(editor)
        );

        editor.replaceText("""
                 for (int i = 0; i < 5; i++)
                 {
                    print(i);
                 }
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

        MenuItem newFile = new MenuItem("New");
        newFile.setOnAction(e -> editor.clear());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newFile,
                new MenuItem("Open"),
                new MenuItem("Save"),
                exit
        );

        MenuBar menuBar = new MenuBar(fileMenu);

        Button runButton = new Button("▶ Run");
        Label title = new Label("PPCE");
        title.setStyle("-fx-font-weight: bold;");

        ToolBar toolbar = new ToolBar(
                title,
                new Separator(),
                runButton
        );

        Label status = new Label("Ready");
        status.getStyleClass().add("status-label");
        status.setPadding(new Insets(6, 10, 6, 10));

        Label languages = new Label("PPC & PPI");
        languages.getStyleClass().add("status-label");
        languages.setPadding(new Insets(6, 10, 6, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(status, spacer, languages);
        statusBar.getStyleClass().add("status-bar");

        VBox top = new VBox(menuBar, toolbar);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(new VirtualizedScrollPane<>(editor));
        root.setBottom(statusBar);

        root.setStyle("-fx-background-color: #181825;");

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/editor.css")).toExternalForm()
        );
        stage.setTitle("PPCE");
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
