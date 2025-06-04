package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    // Database will be created in the project's root directory, or inside src/main/resources/
    // If you put it in resources, it might be tricky if the app is packaged as a JAR.
    // For development, project root is fine.
    private static final String DB_URL = "jdbc:sqlite:library.db";
    // If you want it in resources (e.g. for read-only DB bundled with app):
    // private static final String DB_URL = "jdbc:sqlite::resource:library.db";
    // Or, for development, specify an absolute or relative path:
    // private static final String DB_URL = "jdbc:sqlite:src/main/resources/library.db";


    // Private constructor to prevent instantiation
    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            // The SQLite JDBC driver auto-registers itself in modern Java versions.
            // Class.forName("org.sqlite.JDBC"); // Usually not needed anymore
        } catch (Exception e) {
            // Handle potential ClassNotFoundException if driver loading was manual
            System.err.println("SQLite JDBC Driver not found. Ensure it's in the classpath.");
            throw new SQLException("SQLite JDBC Driver not found", e);
        }
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create Users Table
            String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id TEXT PRIMARY KEY, " + // Using username as user_id for simplicity
                    "username TEXT NOT NULL UNIQUE, " +
                    "password TEXT NOT NULL, " + // Store hashed passwords in a real app
                    "role TEXT NOT NULL CHECK(role IN ('ADMIN', 'NORMAL_USER'))" +
                    ");";
            stmt.execute(createUserTableSql);
            System.out.println("Users table created or already exists.");

            // Create Books Table
            String createBooksTableSql = "CREATE TABLE IF NOT EXISTS books (" +
                    "book_id TEXT PRIMARY KEY, " + // ISBN or unique ID
                    "title TEXT NOT NULL, " +
                    "author TEXT NOT NULL, " +
                    "category TEXT, " +
                    "quantity INTEGER NOT NULL DEFAULT 0, " + // Available copies
                    "total_quantity INTEGER NOT NULL DEFAULT 0" + // Total copies owned
                    ");";
            stmt.execute(createBooksTableSql);
            System.out.println("Books table created or already exists.");

            // Create Borrow Records Table
            String createBorrowRecordsTableSql = "CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id TEXT NOT NULL, " +
                    "book_id TEXT NOT NULL, " +
                    "borrow_date TEXT NOT NULL, " + // Store as YYYY-MM-DD string
                    "due_date TEXT NOT NULL, " +   // Store as YYYY-MM-DD string
                    "return_date TEXT, " +         // Store as YYYY-MM-DD string, NULLABLE
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createBorrowRecordsTableSql);
            System.out.println("Borrow records table created or already exists.");

            // Optional: Create an initial admin user if one doesn't exist
            // This is good for first-time setup.
            // In a real app, consider a more secure way to bootstrap the first admin.
            String checkAdminSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
            var rs = stmt.executeQuery(checkAdminSql);
            if (rs.next() && rs.getInt(1) == 0) {
                String insertAdminSql = "INSERT INTO users (user_id, username, password, role) " +
                        "VALUES ('admin', 'admin', 'admin123', 'ADMIN');"; // Password must be hashed!
                stmt.executeUpdate(insertAdminSql);
                System.out.println("Initial admin user created.");
            }


        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper to close resources - useful for DAO classes later
    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log or ignore
                System.err.println("Error closing resource: " + e.getMessage());
            }
        }
    }
}