package com.company.client;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.System.exit;
import static com.company.client.Constant.*;

public class JWindow extends JFrame {
    private final int PORT = 8189;
    private JTextArea textArea;
    private JTextField login;
    private JButton authButton, addButton, deleteButton, sendButton, downloadButton;
    private JMenuItem miFileExit;
    private JMenuItem miHelpAbout;
    private JPanel authPanel, centralPanel, buttonPanel, treePanel, leftPanel;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private JPasswordField password;
    private DynamicTree tree;
    private String name;
    private File selectedFile;
    private JProgressBar progressBar;
    private SimpleDateFormat dateFormat;

    public JWindow() {
        windowInitialize();
        addListeners();
        dateFormat = new SimpleDateFormat("HH:mm");

        try {
            //Запрос на ввод IP-адреса сервера
            String answer = (String) JOptionPane.showInputDialog(null, MESSAGE_INPUT_IP, WINDOW_TITLE, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (answer == null || answer.equals("")) {
                socket = new Socket(InetAddress.getByName(null), PORT);
            }
            else {
                socket = new Socket(InetAddress.getByName(answer), PORT);
            }

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/authok")) {
                            //Авторизация прошла успешно
                            authPanel.setVisible(false);
                            name = msg.split("\\*")[1];
                            setTitle(WINDOW_TITLE + " - " + name);
                            out.writeUTF("/dir" + "*" + name);  //запрашиваем структуру папок
                        } else if (msg.startsWith("/dirok")) {
                            //Структура папок получена
                            tree.showTree(msg.split("\\*")[1]);
                            textArea.append(dateFormat.format(new Date()) + " Вы подключились к сетевой папке '" + name + "'" + System.lineSeparator());
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                            addButton.setEnabled(true);
                            deleteButton.setEnabled(true);
                            sendButton.setEnabled(true);
                            downloadButton.setEnabled(true);
                            progressBar.setValue(0);
                            progressBar.setVisible(true);
                        } else if (msg.startsWith("/new")) {
                            //Запрос на добавление нового пользователя
                            int result = JOptionPane.showConfirmDialog(null, MESSAGE_NEW_USER, WINDOW_TITLE, JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.YES_OPTION) {
                                out.writeUTF("/newok" + "*" + login.getText() + "*" + new String(password.getPassword()));
                            } else {
                                out.writeUTF("/end");
                                break;
                            }
                        } else if (msg.startsWith("/delok")) {
                            String[] dir = msg.split("\\*")[1].split("/");
                            textArea.append(dateFormat.format(new Date()) + " С сетевого диска успешно удален(а) файл(папка) '" + dir[dir.length - 1] + "'" + System.lineSeparator());
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        } else if (msg.startsWith("/addok")) {
                            String[] dir = msg.split("\\*")[1].split("/");
                            tree.addNewNode(dir[dir.length - 1]);   //Добавляем в дерево
                            textArea.append(dateFormat.format(new Date()) + " На сетевой диск успешно добавлена новая папка '" + dir[dir.length - 1] + "'" + System.lineSeparator());
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        } else if (msg.startsWith("/addno")) {
                            JOptionPane.showMessageDialog(null, ERROR_MESSAGE, WINDOW_TITLE, JOptionPane.ERROR_MESSAGE);
                        } else if (msg.startsWith("/ready")) {
                            //Можно запускать передачу файла по сети - сервер готов
                            FileInputStream fileIn = new FileInputStream(selectedFile);
                            byte b;
                            for (long i = 0; i < selectedFile.length(); i++) {
                                b = (byte) fileIn.read();
                                out.write(b);
                                progressBar.setValue((int) ((double) i / selectedFile.length() * 100));
                            }
                            fileIn.close();
                            progressBar.setValue(100);
                            String[] dir = msg.split("\\*")[1].split("/");
                            tree.addNewFile(dir[dir.length - 1]);   //Добавляем в дерево
                            textArea.append(dateFormat.format(new Date()) + " На сетевой диск успешно загружен новый файл '" + dir[dir.length - 1] + "'" + System.lineSeparator());
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        } else if (msg.startsWith("/sendno")) {
                            //Файл с таким именем уже существует в хранилище
                            String path = msg.split("\\*")[1];
                            String[] dir = path.split("/");
                            String newName = dir[dir.length - 1];
                            int result = JOptionPane.showConfirmDialog(null, FILE_ALREADY_EXIST + newName + "'?", WINDOW_TITLE, JOptionPane.YES_NO_OPTION);
                            if (result == JOptionPane.YES_OPTION) {
                                out.writeUTF("/oksend" + "*" + path);
                            }
                        } else if (msg.startsWith("/size")) {
                            //Сервер прислал размер файла (можно начинать скачивание файла с сервера)
                            String[] elements = msg.split("\\*");
                            long fileSize = Long.parseLong(elements[1]);    //выделяем длину файла из строки
                            FileOutputStream fileOut = new FileOutputStream(selectedFile);
                            out.writeUTF("/okdownload");    //сообщаем серверу, что готовы принимать файл
                            byte b;
                            for (long i = 0; i < fileSize; i++) {
                                b = (byte) in.read();
                                fileOut.write(b);
                                progressBar.setValue((int) ((double) i / fileSize * 100));
                            }
                            fileOut.flush();
                            fileOut.close();
                            progressBar.setValue(100);
                            String[] dir = elements[2].split("/");
                            textArea.append(dateFormat.format(new Date()) + " С сетевого диска успешно скачан файл '" + dir[dir.length - 1] + "'" + System.lineSeparator());
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        } else {
                            textArea.append(msg + System.lineSeparator());
                            textArea.setCaretPosition(textArea.getDocument().getLength());
                        }
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                    JOptionPane.showMessageDialog(null, UNKNOWN_ERROR_MESSAGE, WINDOW_TITLE, JOptionPane.ERROR_MESSAGE);
                } finally {
                    try {
                        in.close();
                        out.close();
                        socket.close();
                        setVisible(false);
                        dispose();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }
        }).start();

        setVisible(true);
    }

    private void windowInitialize() {
        setTitle(WINDOW_TITLE);
        setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource(ICON_PATH)));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width - FORM_WIDTH) / 2, (screenSize.height - FORM_HEIGHT) / 2, FORM_WIDTH, FORM_HEIGHT);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setVisible(false);
        leftPanel.add(progressBar, BorderLayout.SOUTH);

        treePanel = new JPanel();
        treePanel.setLayout(new BorderLayout());
        treePanel.setBackground(Color.WHITE);
        tree = new DynamicTree();
        treePanel.add(tree, BorderLayout.CENTER);

        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4));
        addButton = new JButton(ADD_BUTTON_TEXT);
        addButton.setToolTipText(ADD_BUTTON_LABEL);
        addButton.setEnabled(false);
        buttonPanel.add(addButton);
        deleteButton = new JButton(DELETE_BUTTON_TEXT);
        deleteButton.setToolTipText(DELETE_BUTTON_LABEL);
        deleteButton.setEnabled(false);
        buttonPanel.add(deleteButton);
        sendButton = new JButton(SEND_BUTTON_TEXT);
        sendButton.setToolTipText(SEND_BUTTON_LABEL);
        sendButton.setEnabled(false);
        buttonPanel.add(sendButton);
        downloadButton = new JButton(DOWNLOAD_BUTTON_TEXT);
        downloadButton.setToolTipText(DOWNLOAD_BUTTON_LABEL);
        downloadButton.setEnabled(false);
        buttonPanel.add(downloadButton);

        treePanel.add(buttonPanel, BorderLayout.SOUTH);

        centralPanel = new JPanel();
        centralPanel.setLayout(new GridLayout(1, 2));
        centralPanel.add(leftPanel);
        centralPanel.add(treePanel);

        setLayout(new BorderLayout());
        add(centralPanel, BorderLayout.CENTER);

        login = new JTextField();
        login.setToolTipText(LOGIN_TEXT_LABEL);
        password = new JPasswordField();
        password.setToolTipText(PASSWORD_TEXT_LABEL);
        authButton = new JButton(AUTH_BUTTON_TEXT);
        authPanel = new JPanel();
        authPanel.setLayout(new GridLayout(1, 3));
        authPanel.add(login);
        authPanel.add(password);
        authPanel.add(authButton);
        add(authPanel, BorderLayout.NORTH);

        JMenuBar mainMenu = new JMenuBar();
        JMenu mFile = new JMenu(MENU_FILE);
        JMenu mHelp = new JMenu(MENU_HELP);
        miFileExit = new JMenuItem(MENU_ITEM_EXIT);
        miHelpAbout = new JMenuItem(MENU_ITEM_ABOUT);
        setJMenuBar(mainMenu);
        mainMenu.add(mFile);
        mainMenu.add(mHelp);
        mFile.add(miFileExit);
        mHelp.add(miHelpAbout);
    }

    private void close() {
        int result = JOptionPane.showConfirmDialog(null, CLOSE_MESSAGE, WINDOW_TITLE, JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            try {
                out.writeUTF("/end");
                out.flush();
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                //e.printStackTrace();
            } finally {
                setVisible(false);
                dispose();
                exit(0);
            }
        }
    }

    private void addListeners() {
        authButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    out.writeUTF("/auth" + "*" + login.getText() + "*" + new String(password.getPassword()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setValue(0);
                String answer = tree.removeCurrentNode();
                if (answer != null) {
                    try {
                        out.writeUTF("/del" + "*" + answer);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setValue(0);
                String answer = tree.addNewNode();
                if (answer != null) {
                    try {
                        out.writeUTF("/add" + "*" + answer);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setValue(0);
                String answer = tree.sendFile();
                if (answer != null) {
                    //На дереве выделен элемент, куда будет копироваться файл
                    JFileChooser chooser = new JFileChooser();
                    chooser.setMultiSelectionEnabled(false);
                    chooser.setDialogTitle(WINDOW_TITLE);
                    int returnValue = chooser.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        selectedFile = chooser.getSelectedFile();
                        try {
                            out.writeUTF("/send" + "*" + selectedFile.length() + "*" + answer + selectedFile.getName());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    //На дереве не выделен элемент, куда добавлять файл
                    JOptionPane.showMessageDialog(null, NEED_SELECT_MESSAGE, WINDOW_TITLE, JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                progressBar.setValue(0);
                String answer = tree.downloadFile();
                if (answer != null) {
                    //На дереве выделен элемент, который нужно загрузить
                    String[] nodes = answer.split("\\.");
                    String fileExtension = nodes[nodes.length - 1];
                    JFileChooser chooser = new JFileChooser();
                    chooser.setMultiSelectionEnabled(false);
                    chooser.setDialogTitle(WINDOW_TITLE);
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("Файлы " + fileExtension + " (*." + fileExtension + ")", fileExtension);
                    chooser.setFileFilter(filter);
                    nodes = answer.split("/");
                    chooser.setSelectedFile(new File(nodes[nodes.length - 1]));
                    int returnValue = chooser.showSaveDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        selectedFile = chooser.getSelectedFile();
                        if (selectedFile.exists()) {
                            //Такой файл уже существует - вопрос о перезаписывании
                            int result = JOptionPane.showConfirmDialog(null, MESSAGE_FILE_EXISTS, WINDOW_TITLE, JOptionPane.YES_NO_OPTION);
                            if (result != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }
                        try {
                            out.writeUTF("/download" + "*" + answer);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } else {
                    //На дереве не выделен элемент, который нужно скачать
                    JOptionPane.showMessageDialog(null, NEED_SELECT_FILE, WINDOW_TITLE, JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        miFileExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        miHelpAbout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, ABOUT_MESSAGE, WINDOW_TITLE, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}
