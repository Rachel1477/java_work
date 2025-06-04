package org.example.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
    private String hostname;
    private int port;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(hostname, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to server: " + hostname + ":" + port);
            return true;
        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error when connecting: " + e.getMessage());
        }
        return false;
    }

    public String sendRequest(String request) {
        if (writer == null || reader == null) {
            System.err.println("Not connected to server.");
            return "ERROR::NOT_CONNECTED";
        }
        try {
            System.out.println("Sending to server: " + request);
            writer.println(request);
            String response = reader.readLine();
            System.out.println("Received from server: " + response);
            return response;
        } catch (IOException e) {
            System.err.println("Error during communication: " + e.getMessage());
            // Attempt to close on error to prevent resource leaks
            disconnect();
            return "ERROR::COMMUNICATION_ERROR::" + e.getMessage();
        }
    }

    public void disconnect() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    // Example main method for a simple command-line client
    public static void main(String[] args) {
        Client client = new Client("localhost", 12345);
        if (!client.connect()) {
            return;
        }

        // Test PING
        client.sendRequest(RequestType.PING.name());

        // Test LOGIN
        client.sendRequest(RequestType.LOGIN.name() + "::admin::admin123"); // Successful
        client.sendRequest(RequestType.LOGIN.name() + "::admin::wrongpass"); // Failed

        // Test GET_ALL_BOOKS
        client.sendRequest(RequestType.GET_ALL_BOOKS.name());

        // Test ADD_BOOK (example)
        client.sendRequest(RequestType.ADD_BOOK.name() + "::978-123::Test Book::Test Author::Fiction::5::10");

        // Interactive mode
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nEnter requests (e.g., PING or LOGIN::user::pass) or type 'EXIT' to quit:");
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            if ("EXIT".equalsIgnoreCase(input)) {
                client.sendRequest(RequestType.TERMINATE_CONNECTION.name());
                break;
            }
            if (!input.isEmpty()) {
                client.sendRequest(input);
            }
        }

        client.disconnect();
        scanner.close();
    }
}
