package org.example;

import org.example.db.DatabaseManager;
import org.example.network.Server;

public class MainServer {
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10; // Number of concurrent clients

    public static void main(String[] args) {
        // Initialize database first (creates tables, default admin if not present)
        System.out.println("Initializing database...");
        DatabaseManager.initializeDatabase();
        System.out.println("Database initialization complete.");

        Server server = new Server(PORT, THREAD_POOL_SIZE);

        // Add a shutdown hook for graceful server shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server.isRunning()) {
                System.out.println("Shutdown hook triggered. Stopping server...");
                server.stop();
            }
        }));

        try {
            server.start(); // This will block until server is stopped or an error occurs
        } catch (Exception e) {
            System.err.println("Failed to start the server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Ensure stop is called if start throws an exception not caught by Server.start()
            if (server.isRunning()) {
                server.stop();
            }
        }
    }
}