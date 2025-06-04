package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.example.util.PasswordUtil; // Import PasswordUtil
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:library.db";

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        // First, drop tables if they exist to apply schema changes easily during development
        // In production, you'd use migration scripts.
        // For this project, this helps ensure the new schema is applied.
        // Comment out these drops after the first successful run with the new schema if you want to retain data.
        /*
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS borrow_records;");
            stmt.execute("DROP TABLE IF EXISTS books;");
            stmt.execute("DROP TABLE IF EXISTS users;");
            System.out.println("Existing tables dropped for re-initialization.");
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
        */

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create Users Table
            String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password_hash TEXT NOT NULL, " + // Changed from 'password'
                    "role TEXT NOT NULL CHECK(role IN ('ADMIN', 'NORMAL_USER'))," +
                    "is_active INTEGER NOT NULL DEFAULT 1" + // 1 for true, 0 for false
                    ");";
            stmt.execute(createUserTableSql);
            System.out.println("Users table created or already exists.");

            // Create Books Table
            String createBooksTableSql = "CREATE TABLE IF NOT EXISTS books (" +
                    "book_id TEXT PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT NOT NULL, " +
                    "category TEXT, " +
                    "quantity INTEGER NOT NULL DEFAULT 0, " +
                    "total_quantity INTEGER NOT NULL DEFAULT 0" +
                    ");";
            stmt.execute(createBooksTableSql);
            System.out.println("Books table created or already exists.");

            // Create Borrow Records Table
            String createBorrowRecordsTableSql = "CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT NOT NULL, " +
                    "book_id TEXT NOT NULL, " +
                    "borrow_date TEXT NOT NULL, " +
                    "due_date TEXT NOT NULL, " +
                    "return_date TEXT, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createBorrowRecordsTableSql);
            System.out.println("Borrow records table created or already exists.");

            // Create an initial admin user if one doesn't exist
            String checkAdminSql = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            try (ResultSet rs = stmt.executeQuery(checkAdminSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String adminPassword = "admin123"; // Default admin password
                    String hashedPassword = PasswordUtil.hashPassword(adminPassword);
                    String insertAdminSql = "INSERT INTO users (user_id, username, password_hash, role, is_active) " +
                            "VALUES ('admin_id_001', 'admin', ?, 'ADMIN', 1);"; // Using a fixed admin_id
                    try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                        pstmt.setString(1, hashedPassword);
                        pstmt.executeUpdate();
                        System.out.println("Initial admin user created with user_id 'admin_id_001' and username 'admin'. Password: " + adminPassword);
                    }
                } else {
                    System.out.println("Admin user 'admin' already exists or an error occurred checking.");
                }
            }


        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log or ignore
            }
        }
    }
}