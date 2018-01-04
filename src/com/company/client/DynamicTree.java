package com.company.client;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Stack;

import static com.company.client.Constant.*;

public class DynamicTree extends JPanel {
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    private JTree tree;
    private final String ICON_PATH = "./images/";
    private Toolkit toolkit = Toolkit.getDefaultToolkit();
    private DefaultMutableTreeNode parentToAdd;

    public DynamicTree() {
        super(new GridLayout(1, 0));

        rootNode = new DefaultMutableTreeNode("Root");
        treeModel = new DefaultTreeModel(rootNode);
        treeModel.setAsksAllowsChildren(true);
        treeModel.addTreeModelListener(new MyTreeModelListener());
        tree = new JTree(treeModel);
        //tree.setEditable(true);
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);

        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane);

        ImageIcon iconClosedFolder = new ImageIcon(DynamicTree.class.getResource(ICON_PATH + "ClosedFolder.png"));
        ImageIcon iconOpenedFolder = new ImageIcon(DynamicTree.class.getResource(ICON_PATH + "OpenedFolder.png"));
        ImageIcon iconFile = new ImageIcon(DynamicTree.class.getResource(ICON_PATH + "File.png"));
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(iconFile);
        renderer.setOpenIcon(iconOpenedFolder);
        renderer.setClosedIcon(iconClosedFolder);
        tree.setCellRenderer(renderer);
    }

    public void showTree(String structure) {
        Stack<DefaultMutableTreeNode> stack = new Stack<>();
        StringBuilder sb = new StringBuilder();
        String s;
        DefaultMutableTreeNode p, t;

        for (int i = 0; i < structure.length(); i++) {
            s = structure.substring(i, i + 1);
            if (s.equals("<")) {
                if (sb.length() == 0) {
                    stack.push(null);
                } else {
                    //в sb хранится имя папкки, добавляем ее в дерево
                    p = addObject(stack.peek(), sb.toString());
                    p.setAllowsChildren(true);
                    stack.push(p);
                    sb.setLength(0);
                }
            } else if (s.equals(">")) {
                p = stack.pop();
                if (sb.length() != 0) {
                    t = addObject(p, sb.toString());
                    t.setAllowsChildren(false);
                    sb.setLength(0);
                }
            } else if (s.equals("|")) {
                if (sb.length() != 0) {
                    t = addObject(stack.peek(), sb.toString());
                    t.setAllowsChildren(false);
                    sb.setLength(0);
                }
            } else {
                sb.append(s);
            }
        }
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child) {
        return addObject(parent, child, false);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child, boolean shouldBeVisible) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    public String removeCurrentNode() {
        TreePath currentSelection = tree.getSelectionPath();
        String answer = null;
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                int result = JOptionPane.showConfirmDialog(null, DELETE_MESSAGE, WINDOW_TITLE, JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    answer = buildPath(currentSelection);   //вернуть путь до удаляемой папки (для удаления с сервера)
                    //Проверка, что удаляется папка и она не пустая
                    if (currentNode.getChildCount() > 0) {
                        int result2 = JOptionPane.showConfirmDialog(null, DELETE_NOT_EMPTY, WINDOW_TITLE, JOptionPane.YES_NO_OPTION);
                        if (result2 == JOptionPane.YES_OPTION) {
                            treeModel.removeNodeFromParent(currentNode);
                        }
                    } else {
                        treeModel.removeNodeFromParent(currentNode);
                    }
                }
                return answer;
            }
        }
        toolkit.beep(); //попытка удалить корень
        return answer;
    }

    private String buildPath(TreePath currentSelection) {
        StringBuilder path = new StringBuilder();
        for (int i = 1; i < currentSelection.getPathCount(); i++) {
            path.append(currentSelection.getPathComponent(i) + "/");
        }
        return path.toString();
    }

    public String addNewNode() {
        TreePath currentSelection = tree.getSelectionPath();
        String answer = null, s = "";
        if (currentSelection != null) {
            s = (String) JOptionPane.showInputDialog(null, INPUT_NEW_FOLDER, WINDOW_TITLE, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (s == null) {
                return answer;
            }
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                if (currentNode.getAllowsChildren()) {
                    answer = buildPath(currentSelection) + s;
                    parentToAdd = currentNode;
                } else {
                    answer = buildPath(currentSelection.getParentPath()) + s;
                    parentToAdd = (DefaultMutableTreeNode) currentNode.getParent();
                }
            } else {
                answer = s;
                parentToAdd = null;
            }
        } else {
            toolkit.beep(); //не выделен родитель (куда добавлять элемент)
        }
        return answer;
    }

    public void addNewNode(String name) {
        DefaultMutableTreeNode p = addObject(parentToAdd, name, true);
        p.setAllowsChildren(true);
    }

    //Определяем куда копируется новый элемент (и на сервере, и в дереве)
    public String sendFile() {
        TreePath currentSelection = tree.getSelectionPath();
        String answer = null;
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                if (currentNode.getAllowsChildren()) {
                    answer = buildPath(currentSelection);
                    parentToAdd = currentNode;
                } else {
                    answer = buildPath(currentSelection.getParentPath());
                    parentToAdd = (DefaultMutableTreeNode) currentNode.getParent();
                }
            } else {
                answer = "";
                parentToAdd = null;
            }
        } else {
            toolkit.beep(); //не выделен родитель (куда добавлять элемент)
        }
        return answer;
    }

    //Определяем какой элемет хотят скачать с диска
    public String downloadFile() {
        TreePath currentSelection = tree.getSelectionPath();
        String answer = null;
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            if (currentNode.getAllowsChildren()) {
                toolkit.beep(); //выделена папка
            } else {
                answer = buildPath(currentSelection.getParentPath()) + currentNode.toString();
            }
        } else {
            toolkit.beep(); //не выделен файл для загрузки
        }
        return answer;
    }

    public void addNewFile(String name) {
        DefaultMutableTreeNode p = addObject(parentToAdd, name, true);
        p.setAllowsChildren(false);
    }
}

class MyTreeModelListener implements TreeModelListener {
    public void treeNodesChanged(TreeModelEvent e) {
    }

    public void treeNodesInserted(TreeModelEvent e) {
    }

    public void treeNodesRemoved(TreeModelEvent e) {
    }

    public void treeStructureChanged(TreeModelEvent e) {
    }
}
