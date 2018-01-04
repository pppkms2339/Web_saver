package com.company.database;

import java.sql.*;

public class SQLHandler {
    private static Connection conn;
    private static PreparedStatement stmt;
    private static final String DB_NAME = "ClientsDB.db";

    public static void connect(String dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath + "/" + DB_NAME);
            PreparedStatement statement = conn.prepareStatement("CREATE TABLE IF NOT EXISTS main (Id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                    "   Login TEXT NOT NULL,\n" +
                    "   Password TEXT NOT NULL\n" + ")");
            statement.execute();
            statement.close();
        } catch (Exception c) {
            c.printStackTrace();
            System.out.println("Database connection error");
        }
    }

    public static void disconnect() {
        try {
            conn.close();
        } catch (Exception c) {
            c.printStackTrace();
            System.out.println("Database connection error");
        }
    }

    public static boolean isWrongPassword(String login, String password) {
        try {
            stmt = conn.prepareStatement("SELECT Login FROM main WHERE Login = ? AND Password <> ?;");
            stmt.setString(1, login);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL Query Error");
        }
        return false;
    }

    public static boolean isLoginExist(String login) {
        try {
            stmt = conn.prepareStatement("SELECT Login FROM main WHERE Login = ?");
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL Query Error");
        }

        return false;
    }

    public static void addUser(String login, String password) {
        try {
            stmt = conn.prepareStatement("INSERT INTO main (Login, Password) VALUES (?, ?)");
            stmt.setString(1, login);
            stmt.setString(2, password);
            stmt.execute();
        } catch (SQLException e) {
            System.out.println("SQL Query Error");
            e.printStackTrace();
        }
    }
}
