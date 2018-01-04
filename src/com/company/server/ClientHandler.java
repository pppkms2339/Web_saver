package com.company.server;

import com.company.database.SQLHandler;

import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private String ROOT_PATH;
    private static final String MESSAGE_ALREADY_AUTH = "Вы уже подключены к сетевому диску";
    private static final String MESSAGE_UNCORRECT_PASS = "Неверный пароль";
    private static final String MESSAGE_NEED_LOGIN = "Вам необходимо авторизироваться";

    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;
    private String name = "";
    private FileOutputStream fileOut;
    private FileInputStream fileIn;
    private long fileSize;

    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.ROOT_PATH = server.getWorkPath();
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            try {
                //Авторизация
                SQLHandler.connect(server.getDBPath());
                while (true) {
                    String str = in.readUTF();
                    if (str.startsWith("/auth")) {
                        String[] elements = str.split("\\*");
                        if (elements.length == 3) {
                            if (SQLHandler.isLoginExist(elements[1])) {
                                if (!SQLHandler.isWrongPassword(elements[1], elements[2])) {
                                    //Логин и пароль правильные
                                    if (!server.isLoginRegister(elements[1])) {
                                        //Такого пользователя еще нет в чате
                                        this.name = elements[1];
                                        sendMessage("/authok" + "*" + this.name);
                                        break;
                                    } else {
                                        sendMessage(MESSAGE_ALREADY_AUTH);
                                    }
                                } else {
                                    sendMessage(MESSAGE_UNCORRECT_PASS);
                                }
                            } else {
                                //Регистрация нового пользователя (отправляется запрос)
                                sendMessage("/new");
                            }
                        } else {
                            sendMessage(MESSAGE_NEED_LOGIN);
                        }
                    } else if (str.startsWith("/newok")) {
                        //Регистрация нового пользователя (заносим нового юзера в БД, подключаем к системе)
                        String[] elements = str.split("\\*");
                        SQLHandler.addUser(elements[1], elements[2]);
                        this.name = elements[1];
                        new File(ROOT_PATH + this.name).mkdir();    //Создаем папку на сервере для нового юзера
                        sendMessage("/authok" + "*" + this.name);
                        break;
                    } else {
                        sendMessage(MESSAGE_NEED_LOGIN);
                    }
                }
                SQLHandler.disconnect();
                while (true) {
                    String str = in.readUTF();
                    if (str.equalsIgnoreCase("/end")) {
                        break;
                    }
                    if (str.startsWith("/dir")) {
                        //Запрос структуры папок
                        sendMessage("/dirok" + "*" + FileHandler.buildFolderStructure(new File(ROOT_PATH + this.name)));
                    }
                    if (str.startsWith("/del")) {
                        //Удаление файла или папки с диска
                        String dir = str.split("\\*")[1];
                        FileHandler.delete(new File(ROOT_PATH + this.name + "/" + dir));
                        sendMessage("/delok" + "*" + dir);
                    }
                    if (str.startsWith("/add")) {
                        //Добавление папки на диск
                        String dir = str.split("\\*")[1];
                        //Проврека на то, что данная папка уже существует
                        File f = new File(ROOT_PATH + this.name + "/" + dir);
                        if (f.exists()) {
                            sendMessage("/addno");
                        } else {
                            f.mkdir();
                            sendMessage("/addok" + "*" + dir);
                        }
                    }
                    if (str.startsWith("/send")) {
                        //Передача файлов по сети (запрос на передачу)
                        String[] items = str.split("\\*");
                        fileSize = Long.parseLong(items[1]);    //выделяем длину файла из строки    .
                        String dir = items[2];  //выделяем путь к файлу из строки
                        String newName = dir;
                        //Проврека на то, что данный файл уже существует на сервере
                        File f = new File(ROOT_PATH + this.name + "/" + dir);
                        if (f.exists()) {
                            //Такой файл существует. Для нового файла получаем новое имя
                            newName = FileHandler.newFileName(ROOT_PATH + this.name + "/" + dir);
                            sendMessage("/sendno" + "*" + newName);
                            //f = new File(newName);
                        } else {
                            fileOut = new FileOutputStream(f);
                            sendMessage("/ready" + "*" + newName);
                            dataRecieve();
                        }
                    }
                    if (str.startsWith("/oksend")) {
                        //Файл с таким именем существует, но юзер согласен на новое имя
                        String dir = str.split("\\*")[1];
                        File f = new File(dir);
                        fileOut = new FileOutputStream(f);
                        sendMessage("/ready" + "*" + dir);
                        dataRecieve();
                    }
                    if (str.startsWith("/download")) {
                        //Запрос на скачивание с сервера файла
                        String dir = str.split("\\*")[1];
                        File f = new File(ROOT_PATH + this.name + "/" + dir);
                        fileIn = new FileInputStream(f);
                        fileSize = f.length();
                        sendMessage("/size" + "*" + fileSize + "*" + dir);
                    }
                    if (str.startsWith("/okdownload")) {
                        dataSend();
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
            } finally {
                server.unsubscribeMe(this);
                try {
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    private void dataRecieve() {
        try {
            byte b;
            for (long i = 0; i < fileSize; i++) {
                b = (byte) in.read();
                fileOut.write(b);
            }
            fileOut.flush();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dataSend() {
        try {
            byte b;
            for (long i = 0; i < fileSize; i++) {
                b = (byte) fileIn.read();
                out.write(b);
            }
            fileIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
