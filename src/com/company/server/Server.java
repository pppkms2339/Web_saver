package com.company.server;

import javax.xml.bind.SchemaOutputResolver;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class Server {
    private final int PORT = 8189;
    private Vector<ClientHandler> clients;
    private String DB_PATH, WORK_PATH;

    public Server() {
        ServerSocket server = null;
        Socket socket = null;
        clients = new Vector<>();
        try {
            server = new ServerSocket(PORT);
            //Сервер запущен: запрашиваем рабочие пути
            DB_PATH = new File("").getAbsolutePath().toString() + "\\";
            System.out.println("Now the database's path is: " + DB_PATH);
            Scanner scanner = new Scanner(System.in);
            while(true) {
                System.out.print("Do you want to change it (y/n)? ");
                String s = scanner.nextLine();
                if(s.equalsIgnoreCase("n")) {
                    break;
                }
                if(s.equalsIgnoreCase("y")) {
                    System.out.print("Enter new database's path: ");
                    DB_PATH = scanner.nextLine();
                    break;
                }
            }
            if(!DB_PATH.endsWith("\\")) {
                DB_PATH += "\\";
            }
            WORK_PATH = new File("").getAbsolutePath().toString() + "\\";
            System.out.println("Now the work path is: " + WORK_PATH);
            while(true) {
                System.out.print("Do you want to change it (y/n)? ");
                String s = scanner.nextLine();
                if(s.equalsIgnoreCase("n")) {
                    break;
                }
                if(s.equalsIgnoreCase("y")) {
                    System.out.print("Enter new work path: ");
                    WORK_PATH = scanner.nextLine();
                    break;
                }
            }
            if(!WORK_PATH.endsWith("\\")) {
                WORK_PATH += "\\";
            }
            System.out.println("Server is running, waiting for clients");
            while (true) {
                socket = server.accept();
                subscribeMe(new ClientHandler(socket, this));
                System.out.println("Client is connected");
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Failed to start sever");
        } finally {
            try {
                socket.close();
                server.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }

    public void subscribeMe(ClientHandler c) {
        clients.add(c);
    }

    public void unsubscribeMe(ClientHandler c) {
        clients.remove(c);
        System.out.println("Client is disconnected");
    }

    public boolean isLoginRegister(String login) {
        for (ClientHandler c : clients) {
            if (c.getName().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public String getDBPath() {
        return DB_PATH;
    }

    public String getWorkPath() {
        return WORK_PATH;
    }
}
