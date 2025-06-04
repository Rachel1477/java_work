package org.example.db;

import org.example.util.PasswordUtil; // Assuming PasswordUtil is in this package

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    // MySQL connection details from your Docker setup
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "1111"; // Your Docker exposed port
    private static final String DB_NAME = "my_db";    // Your Docker MYSQL_DATABASE
    private static final String DB_USER = "root";     // Your Docker MYSQL_ROOT_USER (default is root)
    private static final String DB_PASSWORD = "123456"; // Your Docker MYSQL_ROOT_PASSWORD

    // JDBC URL for MySQL
    // Added serverTimezone for compatibility, and allowPublicKeyRetrieval for some MySQL versions
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    // Private constructor to prevent instantiation
    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        try {
            // Explicitly load the MySQL driver (optional for modern JDBC, but good practice)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Ensure it's in the classpath.");
            throw new SQLException("MySQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void initializeDatabase() {
        // IMPORTANT: Dropping tables in MySQL will fail if there are foreign key constraints
        // and the dependent table has data. For development, you might drop in reverse order of creation
        // or temporarily disable foreign key checks.
        // For now, we'll attempt to create IF NOT EXISTS.
        // If you need a clean slate, manually drop tables in MySQL Workbench/CLI or handle it here carefully.
        /*
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // MySQL specific way to temporarily disable FK checks for dropping
            stmt.execute("SET FOREIGN_KEY_CHECKS=0;");
            stmt.execute("DROP TABLE IF EXISTS borrow_records;");
            stmt.execute("DROP TABLE IF EXISTS books;");
            stmt.execute("DROP TABLE IF EXISTS users;");
            stmt.execute("SET FOREIGN_KEY_CHECKS=1;");
            System.out.println("Existing tables dropped for re-initialization (MySQL).");
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
        */


        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create Users Table - MySQL syntax
            String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id VARCHAR(255) PRIMARY KEY, " + // VARCHAR for flexibility
                    "username VARCHAR(255) NOT NULL UNIQUE, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "role ENUM('ADMIN', 'NORMAL_USER') NOT NULL," + // MySQL ENUM type
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE" + // MySQL BOOLEAN (alias for TINYINT(1))
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"; // Specify engine and charset
            stmt.execute(createUserTableSql);
            System.out.println("Users table created or already exists in MySQL.");

            // Create Books Table - MySQL syntax
            String createBooksTableSql = "CREATE TABLE IF NOT EXISTS books (" +
                    "book_id VARCHAR(255) PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "author VARCHAR(255) NOT NULL, " +
                    "category VARCHAR(100), " +
                    "quantity INT NOT NULL DEFAULT 0, " +
                    "total_quantity INT NOT NULL DEFAULT 0" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            stmt.execute(createBooksTableSql);
            System.out.println("Books table created or already exists in MySQL.");

            // Create Borrow Records Table - MySQL syntax
            // AUTO_INCREMENT for MySQL is different from SQLite's AUTOINCREMENT
            // DATE type for dates in MySQL
            String createBorrowRecordsTableSql = "CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "book_id VARCHAR(255) NOT NULL, " +
                    "borrow_date DATE NOT NULL, " +
                    "due_date DATE NOT NULL, " +
                    "return_date DATE NULL, " + // DATE can be NULL
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
            stmt.execute(createBorrowRecordsTableSql);
            System.out.println("Borrow records table created or already exists in MySQL.");

            // Create an initial admin user if one doesn't exist
            String checkAdminSql = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
            try (ResultSet rs = stmt.executeQuery(checkAdminSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String adminPassword = "admin123"; // Default admin password
                    String hashedPassword = PasswordUtil.hashPassword(adminPassword);
                    // Using PreparedStatement for inserting data is safer
                    String insertAdminSql = "INSERT INTO users (user_id, username, password_hash, role, is_active) " +
                            "VALUES (?, ?, ?, 'ADMIN', TRUE);";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                        pstmt.setString(1, "admin_id_001"); // Fixed admin_id
                        pstmt.setString(2, "admin");
                        pstmt.setString(3, hashedPassword);
                        pstmt.executeUpdate();
                        System.out.println("Initial admin user created in MySQL (user_id: admin_id_001, username: admin).");
                    }
                } else {
                    System.out.println("Admin user 'admin' already exists in MySQL or error occurred.");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error initializing MySQL database: " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for debugging
        }
    }

    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log or ignore based on preference
                // System.err.println("Error closing resource: " + e.getMessage());
            }
        }
    }
}