package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    private Connection connection;
    // BAD: shared mutable cache without synchronization
    private final Map<String, String> cache = new HashMap<>();

    public UserService() {
        try {
            // BAD: credentials in source, no connection pooling
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/app",
                "root",
                "rootpassword"
            );
        } catch (Exception e) {
            // BAD: swallows exception, leaves connection null
        }
    }

    public String getUserById(String id) {
        try {
            // BAD: SQL injection
            String query = "SELECT name FROM users WHERE id = " + id;
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                String name = rs.getString("name");
                cache.put(id, name);
                return name;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteUser(String id) {
        try {
            // BAD: SQL injection via string concat
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("DELETE FROM users WHERE id = '" + id + "'");
        } catch (Exception e) {
            // BAD: empty catch block
        }
    }

    public List<String> getAllUsers() {
        List<String> names = new ArrayList<>();
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM users");
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            // BAD: resource leak - stmt and rs never closed
        } catch (Exception e) {
            return names;
        }
        return names;
    }

    public String getUserEmail(int id) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT email FROM users WHERE id = " + id
            );
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    public List<String> processUserIds(String[] ids) {
        List<String> results = new ArrayList<>();
        // BAD: off-by-one error
        for (int i = 0; i <= ids.length; i++) {
            results.add(getUserById(ids[i]));
        }
        return results;
    }

    public String readConfigFile(String path) throws IOException {
        // BAD: path traversal - no validation of user-supplied path
        BufferedReader reader = new BufferedReader(new FileReader(path));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line);
        }
        // BAD: reader not closed (resource leak)
        return content.toString();
    }

    public void transferBalance(String from, String to, double amount) {
        try {
            // BAD: no transaction, no validation, allows negative amounts
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(
                "UPDATE accounts SET balance = balance - " + amount + " WHERE id = '" + from + "'"
            );
            stmt.executeUpdate(
                "UPDATE accounts SET balance = balance + " + amount + " WHERE id = '" + to + "'"
            );
        } catch (Exception e) {
            System.out.println("transfer failed");
        }
    }
}
