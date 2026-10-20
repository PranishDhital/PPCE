package com.pranish.filetree;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FileTree extends TreeItem<File> {
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;

    public FileTree(File file) {
        super(file);
    }

    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            File file = getValue();
            isLeaf = file.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> item) {
        File f = item.getValue();
        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                Arrays.sort(files, Comparator
                        .comparing(File::isFile)                       // folders first
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                for (File child : files) {
                    children.add(new FileTree(child));
                }
            }
        }
        return children;
    }
}