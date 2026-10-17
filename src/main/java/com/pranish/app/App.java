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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;

public class App extends Application {

    @Override
    public void start(Stage stage) {


        CodeArea editor = new CodeArea();

        editor.setParagraphGraphicFactory(
                LineNumberFactory.get(editor)
        );

        editor.replaceText("""
                // Welcome to PPC Editor!

                // Write your PPC code here.

                """);

        editor.setStyle("""
        -fx-font-family: 'JetBrains Mono';
        -fx-font-size: 15px;
        -fx-background-color: #000000;
        -fx-control-inner-background: #000000;
        -fx-text-fill: #ffffff;
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
        status.setPadding(new Insets(6, 10, 6, 10));

        HBox statusBar = new HBox(status);
        statusBar.setPadding(new Insets(4));

        VBox top = new VBox(menuBar, toolbar);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(new VirtualizedScrollPane<>(editor));
        root.setBottom(statusBar);

        root.setStyle("-fx-background-color: #181825;");

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("/editor.css").toExternalForm()
        );
        stage.setTitle("PPC Editor");
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
