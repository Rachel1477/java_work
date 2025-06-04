package org.example.network;

import org.example.model.User; // For future use with actual authentication

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    // private User currentUser; // To store logged-in user state in the future

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true) // true for auto-flush
        ) {
            String clientRequest;
            while ((clientRequest = reader.readLine()) != null) {
                System.out.println("Received from client " + clientSocket.getPort() + ": " + clientRequest);
                String response = processRequest(clientRequest);
                writer.println(response);
                System.out.println("Sent to client " + clientSocket.getPort() + ": " + response);

                if (clientRequest.startsWith(RequestType.TERMINATE_CONNECTION.name())) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("IO Error handling client " + clientSocket.getPort() + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket " + clientSocket.getPort() + ": " + e.getMessage());
            }
            System.out.println("Client disconnected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
        }
    }

    private String processRequest(String requestString) {
        String[] parts = requestString.split("::");
        if (parts.length == 0) {
            return "ERROR::INVALID_REQUEST_FORMAT";
        }

        RequestType requestType;
        try {
            requestType = RequestType.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            return "ERROR::UNKNOWN_REQUEST_TYPE::" + parts[0];
        }

        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        // Placeholder for actual business logic (DAO calls, etc.)
        // For now, we'll have simple responses.
        switch (requestType) {
            case PING:
                return "SUCCESS::PONG";
            case LOGIN:
                if (args.length == 2) {
                    // Placeholder: In a real app, call AuthService.login(args[0], args[1])
                    // For now, simulate success if username is "admin" and password "admin123"
                    if ("admin".equals(args[0]) && "admin123".equals(args[1])) {
                        // this.currentUser = new Admin(args[0], args[0], args[1]); // Example for future
                        return "SUCCESS::LOGIN_SUCCESSFUL::Welcome " + args[0];
                    } else if ("user".equals(args[0]) && "user123".equals(args[1])) {
                        // this.currentUser = new NormalUser(args[0], args[0], args[1]); // Example for future
                        return "SUCCESS::LOGIN_SUCCESSFUL::Welcome " + args[0];
                    } else {
                        return "FAILURE::LOGIN_FAILED::Invalid credentials";
                    }
                } else {
                    return "ERROR::LOGIN_INVALID_ARGS::Expected username and password";
                }
            case REGISTER:
                if (args.length >= 2) { // username, password, [role]
                    // Placeholder: In a real app, call UserService.register(...)
                    return "SUCCESS::REGISTRATION_ATTEMPTED_FOR::" + args[0];
                } else {
                    return "ERROR::REGISTER_INVALID_ARGS";
                }
            case GET_ALL_BOOKS:
                // Placeholder: call BookDAO.getAllBooks() and serialize the list
                return "SUCCESS::BOOK_LIST_PLACEHOLDER::[Book1 Details; Book2 Details]";
            case ADD_BOOK:
                if (args.length == 6) { // bookId, title, author, category, quantity, totalQuantity
                    // Placeholder: call BookDAO.addBook(...)
                    return "SUCCESS::BOOK_ADDED_PLACEHOLDER::" + args[0];
                } else {
                    return "ERROR::ADD_BOOK_INVALID_ARGS";
                }
            case BORROW_BOOK:
                if (args.length == 2) { // userId, bookId
                    // Placeholder: call BorrowService.borrowBook(args[0], args[1])
                    // Needs to check book availability, user status, etc.
                    // Update book quantity (thread-safe)
                    return "SUCCESS::BORROW_ATTEMPTED::User " + args[0] + " for Book " + args[1];
                } else {
                    return "ERROR::BORROW_BOOK_INVALID_ARGS";
                }
            case TERMINATE_CONNECTION:
                return "SUCCESS::CONNECTION_TERMINATED";
            default:
                return "INFO::REQUEST_RECEIVED::" + requestType.name() + "::" + String.join(":", args);
        }
    }
}