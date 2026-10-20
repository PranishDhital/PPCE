package com.pranish.terminal;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TerminalPane extends BorderPane {
    private final TextArea output = new TextArea();
    private final TextField input = new TextField();
    private final Label shellLabel = new Label("Terminal not started");
    private final List<String> history = new ArrayList<>();
    private int historyIndex = 0;

    private Process process;
    private BufferedWriter writer;
    private boolean started;

    public TerminalPane()
    {
        output.setEditable(false);
        output.setWrapText(true);
        output.getStyleClass().add("output");

        input.setPromptText("Type a command and press enter");
        input.getStyleClass().add("input");

        shellLabel.getStyleClass().add("terminal-shell-label");
        setTop(shellLabel);
        setCenter(output);
        setBottom(input);

        input.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String cmd = input.getText();
                input.clear();
                if (!cmd.isBlank()) {
                    history.add(cmd);
                    historyIndex = history.size();
                }
                String trimmed = cmd.trim();
                if (trimmed.equalsIgnoreCase("cls")){
                    output.clear();
                } else {
                    send(cmd);
                }
                e.consume();
            } else if (e.getCode() == KeyCode.UP && historyIndex > 0) {
                input.setText(history.get(--historyIndex));
                input.positionCaret(input.getText().length());
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                if (historyIndex < history.size() - 1) {
                    input.setText(history.get(++historyIndex));
                } else {
                    historyIndex = history.size();
                    input.clear();
                }
                input.positionCaret(input.getText().length());
                e.consume();
            }else if (e.isControlDown() && e.getCode() == KeyCode.L) {
                output.clear();
                e.consume();
            }
        });
    }

    public void start(File workingDir)
    {
        stop();
        File directory = workingDir != null && workingDir.isDirectory()
                ? workingDir : new File(System.getProperty("user.home"));
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        String shell = windows ? "PowerShell" : "bash";
        shellLabel.setText(shell + " " + directory.getAbsolutePath());
        try {
            ProcessBuilder builder;
            if (windows) {
                // Keep the interactive shell, but suppress its prompt so it is not
                // mixed into the command output shown in the terminal pane.
                builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile",
                        "-NoExit", "-Command", "function prompt { '' }");
            } else {
                builder = new ProcessBuilder("bash", "--noprofile", "--norc", "-i");
                builder.environment().put("PS1", "bash \\w $ ");
            }
            builder.directory(directory);
            builder.redirectErrorStream(true);
            process = builder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            started = true;
            readOutput(process);
        } catch (IOException e) {
            process = null;
            writer = null;
            started = false;
            output.appendText("[Could not start " + shell + ": " + e.getMessage() + "]\n");
        }
    }

    public boolean isStarted() {
        return started && process != null && process.isAlive();
    }


    public void send(String cmd)
    {
        if (writer == null || process==null || !process.isAlive())
        {
            output.appendText("[The shell is not running]\n");
            return;
        }
        try{
            writer.write(cmd);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            output.appendText("[Write failed : " + e.getMessage() + "]\n");
        }
    }
    public void stop()
    {
        if(process != null)
        {
            process.destroyForcibly();
            process = null;
            writer = null;
        }
        started = false;
    }

    private void readOutput(Process process)
    {
        Thread thread = new Thread(() -> {
            try(Reader reader = new InputStreamReader(process.getInputStream()))
            {
                char[] buffer = new char[1024];
                int n;
                while ((n = reader.read(buffer)) != -1)
                {
                    String text = new String(buffer, 0, n);
                    Platform.runLater(() -> output.appendText(text));
                }
            } catch (IOException ignored) {
            }
        },"terminalReader");
        thread.setDaemon(true);
        thread.start();
    }
    public void focusInput()
    {
        input.requestFocus();
    }

    public void clear()
    {
        output.clear();
    }

    public void run(String displayText, String command)
    {
        output.clear();
        output.appendText(displayText);
        send(command);
    }
    public void println(String text)
    {
        output.appendText(text + "\n");
    }


}
