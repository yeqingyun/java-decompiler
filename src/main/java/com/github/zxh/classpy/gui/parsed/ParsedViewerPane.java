package com.github.zxh.classpy.gui.parsed;

import com.github.zxh.classpy.classfile.ClassFile;
import com.github.zxh.classpy.common.FileComponent;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

/**
 * Container of TreeView, HexPane, StatusBar and BytesBar.
 * <p>
 * |------------------------------|
 * | TreeView      |      HexPane |
 * |               |              |
 * |------------------------------|
 * | StatusLabel          BytesBar|
 * |------------------------------|
 */
public class ParsedViewerPane extends BorderPane {

    private final TreeView<FileComponent> tree;
    private HexPane hexPane;
    private final Label statusLabel;
    private final BytesBar bytesBar;

    public ParsedViewerPane(FileComponent file, HexText hex) {
        tree = buildClassTree(file, hex != null);
        if (hex == null) {
            try {
                hexPane = new HexPane(((ClassFile) file).generateJava());
            } catch (Exception e) {
                hexPane = new HexPane("很抱歉，目前暂不支持该class所包含的指令。");
            }
        } else {
            hexPane = new HexPane(hex);
        }
        statusLabel = new Label(" ");
        bytesBar = new BytesBar(file.getLength());
        bytesBar.setMaxHeight(statusLabel.getPrefHeight());
        bytesBar.setPrefWidth(100);

        super.setCenter(buildSplitPane());
        super.setBottom(buildStatusBar());
        listenTreeItemSelection();
    }

    private static TreeView<FileComponent> buildClassTree(FileComponent file, boolean flag) {
        if (!flag) {
            ParsedTreeItem root = new ParsedTreeItem(file);
            root.setExpanded(true);
            TreeView<FileComponent> tree = new TreeView<>(null);
            tree.setMaxWidth(0);
            return tree;
        } else {
            ParsedTreeItem root = new ParsedTreeItem(file);
            root.setExpanded(true);
            TreeView<FileComponent> tree = new TreeView<>(root);
            tree.setMinWidth(200);
            return tree;
        }
    }

    private SplitPane buildSplitPane() {
        SplitPane sp = new SplitPane();
        sp.getItems().add(tree);
        sp.getItems().add(hexPane);
        sp.setDividerPositions(0.3, 0.7);
        return sp;
    }

    private BorderPane buildStatusBar() {
        BorderPane statusBar = new BorderPane();
        statusBar.setLeft(statusLabel);
        statusBar.setRight(bytesBar);
        return statusBar;
    }

    private void listenTreeItemSelection() {
        tree.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener.Change<? extends TreeItem<FileComponent>> c) -> {
                    if (c.next() && c.wasAdded()) {
                        TreeItem<FileComponent> node = c.getList().get(c.getFrom());
                        if (node != null && node.getParent() != null) {
                            FileComponent cc = node.getValue();
                            statusLabel.setText(" " + cc.getClass().getSimpleName());
                            if (cc.getLength() > 0) {
                                hexPane.select(cc);
                                bytesBar.select(cc);
                            }
                        }
                    }
                }
        );
    }

}
