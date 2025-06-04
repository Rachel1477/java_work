package org.example.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public Server(int port, int poolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    public void start() {
        if (running) {
            System.out.println("Server is already running.");
            return;
        }
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port: " + port);
            System.out.println("Waiting for client connections...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept(); // Blocking call
                    threadPool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (!running) {
                        System.out.println("Server socket closed, shutting down.");
                        break;
                    }
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) { // Only log error if it wasn't a controlled shutdown
                System.err.println("Could not listen on port " + port + ": " + e.getMessage());
            }
        } finally {
            stop(); // Ensure resources are cleaned up
        }
    }

    public void stop() {
        running = false;
        System.out.println("Stopping server...");
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!threadPool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.err.println("Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException ie) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        System.out.println("Server stopped.");
    }

    // Getter for running status, useful for graceful shutdown logic from other threads
    public boolean isRunning() {
        return running;
    }
}