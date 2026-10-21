# PPCE

PPCE is a desktop code editor for the `.pd` programming language. Edit and syntax-highlight programs, browse files, and run them with the bundled PPI interpreter or PPC compiler.

![PPCE editor](images/Screenshot%202026-10-21%20112035.png)

## Run

Requires Java 26 and Maven. Start the editor with:

```sh
mvn javafx:run
```

Open or save a `.pd` file, then press **F5** to choose the interpreter or compiler. Use **Ctrl+`** to toggle the integrated terminal.

## Built with

- Java 26 and JavaFX 26
- RichTextFX for the code editor and syntax highlighting
- Maven for dependency and build management
- PPC compiler and PPI interpreter for .pd programs

## License

PPCE is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE). You may use, modify, and share it for noncommercial purposes with attribution. Commercial use, including commercial resale, is not permitted without separate permission. This is a source-available license, not an open-source license.
