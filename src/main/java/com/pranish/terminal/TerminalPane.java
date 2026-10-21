package com.pranish.terminal;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TerminalPane extends BorderPane {
    private final TextArea output = new TextArea();
    private final Label shellLabel = new Label("Terminal not started");
    private final List<String> history = new ArrayList<>();
    private int historyIndex = 0;
    private boolean promptReady = true;
    private String shellPrompt = "PS> ";
    private final StringBuilder transcript = new StringBuilder();
    private final StringBuilder currentInput = new StringBuilder();
    private static final String PROMPT_MARKER = "\u001ePPCE_PROMPT\u001f";

    private Process process;
    private BufferedWriter writer;
    private boolean started;
    private Runnable closeAction = () -> {
    };

    public TerminalPane() {
        // Keep command entry in the terminal itself, like an integrated terminal.
        output.setEditable(false);
        output.setWrapText(true);
        output.getStyleClass().add("output");

        shellLabel.getStyleClass().add("terminal-shell-label");
        setTop(shellLabel);
        Button closeButton = new Button("×");
        closeButton.getStyleClass().add("terminal-close-button");
        closeButton.setFocusTraversable(false);
        closeButton.setOnAction(e -> closeAction.run());
        HBox header = new HBox(shellLabel, closeButton);
        header.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(shellLabel, javafx.scene.layout.Priority.ALWAYS);
        setTop(header);
        setCenter(output);
        renderTerminal();
        output.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String cmd = currentInput.toString();
                currentInput.setLength(0);
                transcript.append("> ").append(cmd).append('\n');
                boolean shellIsWaiting = promptReady;
                promptReady = false;
                if (!cmd.isBlank()) {
                    history.add(cmd);
                    historyIndex = history.size();
                }
                if (shellIsWaiting && (cmd.trim().equalsIgnoreCase("cls")
                        || cmd.trim().equalsIgnoreCase("clear"))) {
                    transcript.setLength(0);
                    promptReady = true;
                } else {
                    send(cmd);
                }
                renderTerminal();
                e.consume();
            } else if (e.getCode() == KeyCode.UP && historyIndex > 0) {
                currentInput.setLength(0);
                currentInput.append(history.get(--historyIndex));
                renderTerminal();
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN) {
                if (historyIndex < history.size() - 1) {
                    currentInput.setLength(0);
                    currentInput.append(history.get(++historyIndex));
                } else {
                    historyIndex = history.size();
                    currentInput.setLength(0);
                }
                renderTerminal();
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.L) {
                transcript.setLength(0);
                renderTerminal();
                e.consume();
            } else if (e.getCode() == KeyCode.BACK_SPACE) {
                if (!currentInput.isEmpty())
                    currentInput.deleteCharAt(currentInput.length() - 1);
                renderTerminal();
                e.consume();
            } else if (e.getCode() == KeyCode.LEFT || e.getCode() == KeyCode.HOME
                    || e.getCode() == KeyCode.END || e.getCode() == KeyCode.RIGHT
                    || e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.TAB) {
                e.consume();
            }
        });
        output.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            String ch = e.getCharacter();
            if (ch != null && ch.length() == 1 && ch.charAt(0) >= ' ' && ch.charAt(0) != '\u007f') {
                currentInput.append(ch);
                renderTerminal();
            }
            e.consume();
        });
    }

    public void start(File workingDir) {
        stop();
        File directory = workingDir != null && workingDir.isDirectory()
                ? workingDir
                : new File(System.getProperty("user.home"));
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        String shell = windows ? "PowerShell" : "bash";
        shellPrompt = windows ? "PS> " : "$ ";
        shellLabel.setText(shell + " " + directory.getAbsolutePath());
        try {
            ProcessBuilder builder;
            if (windows) {
                // Keep the interactive shell, but suppress its prompt so it is not
                // mixed into the command output shown in the terminal pane.
                builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile",
                        "-NoExit", "-Command", "function prompt { '" + PROMPT_MARKER + "' }");
            } else {
                builder = new ProcessBuilder("bash", "--noprofile", "--norc", "-i");
                builder.environment().put("PS1", PROMPT_MARKER);
            }
            builder.directory(directory);
            builder.redirectErrorStream(true);
            process = builder.start();
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            started = true;
            transcript.setLength(0);
            currentInput.setLength(0);
            promptReady = true;
            readOutput(process);
        } catch (IOException e) {
            process = null;
            writer = null;
            started = false;
            transcript.append("[Could not start ").append(shell).append(": ").append(e.getMessage()).append("]\n");
            renderTerminal();
        }
    }

    public boolean isStarted() {
        return started && process != null && process.isAlive();
    }

    public void send(String cmd) {
        if (writer == null || process == null || !process.isAlive()) {
            transcript.append("[The shell is not running]\n");
            renderTerminal();
            return;
        }
        try {
            promptReady = false;
            writer.write(cmd);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            transcript.append("[Write failed: ").append(e.getMessage()).append("]\n");
            renderTerminal();
        }
    }

    public void stop() {
        if (process != null) {
            process.destroyForcibly();
            process = null;
            writer = null;
        }
        started = false;
    }

    private void readOutput(Process process) {
        Thread thread = new Thread(() -> {
            try (Reader reader = new InputStreamReader(process.getInputStream())) {
                char[] buffer = new char[1024];
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    String text = new String(buffer, 0, n);
                    Platform.runLater(() -> {
                        appendShellOutput(text);
                    });
                }
            } catch (IOException ignored) {
            }
        }, "terminalReader");
        thread.setDaemon(true);
        thread.start();
    }

    public void focusInput() {
        output.requestFocus();
    }

    public void setOnClose(Runnable action) {
        closeAction = action == null ? () -> {
        } : action;
    }

    private void appendShellOutput(String text) {
        String combined = transcript + text;
        int marker;
        while ((marker = combined.indexOf(PROMPT_MARKER)) >= 0) {
            combined = combined.substring(0, marker) + combined.substring(marker + PROMPT_MARKER.length());
            promptReady = true;
        }
        transcript.setLength(0);
        transcript.append(combined);
        renderTerminal();
    }

    private void renderTerminal() {
        output.setText(transcript + (promptReady ? shellPrompt : "") + currentInput);
        output.positionCaret(output.getLength());
    }

    public void clear() {
        transcript.setLength(0);
        renderTerminal();
    }

    public void run(String displayText, String command) {
        transcript.setLength(0);
        transcript.append(displayText);
        renderTerminal();
        send(command);
    }

    public void println(String text) {
        transcript.append(text).append('\n');
        renderTerminal();
    }

}
