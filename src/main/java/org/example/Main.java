package org.example;

import org.example.db.DatabaseManager;
import org.example.model.Admin;
import org.example.model.Book;
import org.example.model.NormalUser;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    public static void main(String[] args) {
        System.out.println("Library Management System - Initializing...");

        // Initialize the database and create tables if they don't exist
        DatabaseManager.initializeDatabase();
        System.out.println("Database initialization complete.");

        // Example: Test model classes
        Admin admin = new Admin("admin001", "superadmin", "securepass");
        System.out.println("Created Admin: " + admin);

        NormalUser user = new NormalUser("user001", "john.doe", "password123");
        System.out.println("Created Normal User: " + user);

        Book book = new Book("978-0321765723", "Effective Java", "Joshua Bloch", "Programming", 5, 10);
        System.out.println("Created Book: " + book);

        // Example: Test database connection and query (simple test)
        System.out.println("\nTesting database connection and admin user existence...");
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT username, role FROM users WHERE user_id = 'admin'");
            if (rs.next()) {
                System.out.println("Found admin user: " + rs.getString("username") + " with role: " + rs.getString("role"));
            } else {
                System.out.println("Admin user 'admin' not found. (This is okay if it wasn't created by initializeDatabase)");
            }
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\nTo see the database, you can use a SQLite browser to open 'library.db' in the project root.");
    }
}