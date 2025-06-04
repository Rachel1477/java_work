package org.example.client;
import org.example.model.Book;
import org.example.model.BorrowRecord;
import org.example.model.Role;
import org.example.model.User; // Assuming User model is accessible
import org.example.network.RequestType; // From your backend

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter; // For UTF-8
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets; // For UTF-8
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClientService {
    private static ClientService instance;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final String hostname = "localhost"; // Or get from config
    private final int port = 12345;          // Or get from config

    private User currentUser; // Store logged-in user info

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;


    private ClientService() {
        // Private constructor for singleton
    }

    public static synchronized ClientService getInstance() {
        if (instance == null) {
            instance = new ClientService();
        }
        return instance;
    }

    public boolean connect() {
        try {
            if (socket == null || socket.isClosed()) {
                socket = new Socket(hostname, port);
                // Use UTF-8 for consistency
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                System.out.println("GUI Client: Connected to server.");
            }
            return true;
        } catch (UnknownHostException e) {
            System.err.println("GUI Client: Server not found: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("GUI Client: I/O error when connecting: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (writer != null) writer.println(RequestType.TERMINATE_CONNECTION.name());
            if (socket != null && !socket.isClosed()) socket.close();
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            currentUser = null; // Clear user on disconnect
            System.out.println("GUI Client: Disconnected from server.");
        } catch (IOException e) {
            System.err.println("GUI Client: Error disconnecting: " + e.getMessage());
        } finally {
            socket = null;
            writer = null;
            reader = null;
        }
    }

    private String sendRequestGetResponse(String request) throws IOException {
        if (!connect()) { // Ensure connection
            throw new IOException("Failed to connect to the server.");
        }
        System.out.println("GUI Client Sending: " + request);
        writer.println(request);
        String response = reader.readLine();
        System.out.println("GUI Client Received: " + response);
        if (response == null) {
            // This can happen if the server closes the connection abruptly
            // or if the readLine() is interrupted before any data.
            disconnect(); // Try to clean up client side resources
            throw new IOException("Server closed connection or no response received.");
        }
        return response;
    }

    // --- User Operations ---
    public User login(String username, String password) throws IOException, AuthenticationException {
        String request = RequestType.LOGIN.name() + "::" + username + "::" + password;
        String response = sendRequestGetResponse(request); // Can throw IOException
        String[] parts = response.split("::", -1);

        // Based on your client log: SUCCESS::LOGIN_SUCCESSFUL::NORMAL_USER::user_6d013e6b::Welcome rachel
        if ("SUCCESS".equals(parts[0]) && "LOGIN_SUCCESSFUL".equals(parts[1]) && parts.length >= 5) {
            Role role = Role.valueOf(parts[2].toUpperCase()); // parts[2] is ROLE ("NORMAL_USER")
            String userId = parts[3];                         // parts[3] is USER_ID ("user_6d013e6b")
            // String welcomeMsg = parts[4];                  // parts[4] is "Welcome rachel" (We'll use the input username)

            this.currentUser = (role == Role.ADMIN) ?
                    new org.example.model.Admin(userId, username, null) : // password hash not needed client-side
                    new org.example.model.NormalUser(userId, username, null);
            return this.currentUser;
        } else if ("FAILURE".equals(parts[0])) {
            throw new AuthenticationException(parts.length > 2 ? parts[2] : "Login failed due to server indicating failure.");
        } else {
            // Log the unexpected response for debugging
            System.err.println("GUI Client: Unexpected login response format: " + response);
            throw new IOException("Unexpected server response during login: " + response);
        }
    }

    public String register(String username, String password) throws IOException, RegistrationException {
        String request = RequestType.REGISTER.name() + "::" + username + "::" + password;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "REGISTRATION_SUCCESSFUL".equals(parts[1])) {
            return parts[2]; // e.g., "User username created with ID userId"
        } else if ("FAILURE".equals(parts[0])) {
            throw new RegistrationException(parts.length > 2 ? parts[2] : "Registration failed.");
        } else {
            throw new IOException("Unexpected server response: " + response);
        }
    }

    public void logout() {
        if (currentUser != null) {
            try {
                // Send logout request, but don't critically depend on its response for client-side logout
                if (socket != null && !socket.isClosed() && writer != null) {
                    sendRequestGetResponse(RequestType.LOGOUT.name()); // Can throw IOException, catch it.
                }
            } catch (IOException e) {
                System.err.println("GUI Client: Error during server logout request: " + e.getMessage());
                // Proceed with client-side logout anyway
            } finally {
                currentUser = null; // Always clear current user on client side
            }
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // --- Book Operations ---
    public List<Book> getAllBooks() throws IOException {
        String response = sendRequestGetResponse(RequestType.GET_ALL_BOOKS.name());
        return parseBookListResponse(response);
    }

    public List<Book> searchBooks(String field, String term) throws IOException {
        String request = RequestType.SEARCH_BOOK.name() + "::" + field + "::" + term;
        String response = sendRequestGetResponse(request);
        return parseBookListResponse(response, "BOOK_SEARCH_RESULTS");
    }

    public Book getBookById(String bookId) throws IOException {
        String request = RequestType.GET_BOOK_BY_ID.name() + "::" + bookId;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "BOOK_DETAILS".equals(parts[1]) && parts.length > 2) {
            return parseBook(parts[2]);
        } else if ("FAILURE".equals(parts[0]) && "BOOK_NOT_FOUND".equals(parts[1])) {
            return null;
        }
        System.err.println("GUI Client: Failed to get book details. Raw response: " + response);
        throw new IOException("Failed to get book details: " + (parts.length > 2 ? parts[2] : response));
    }


    public String borrowBook(String bookId) throws IOException, OperationException {
        if (currentUser == null) throw new OperationException("User not logged in.");
        String request = RequestType.BORROW_BOOK.name() + "::" + bookId;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "BORROW_SUCCESSFUL".equals(parts[1])) {
            return parts[2]; // Success message
        } else if ("FAILURE".equals(parts[0])) {
            throw new OperationException(parts.length > 2 ? parts[2] : "Borrow failed.");
        }
        throw new IOException("Unexpected server response for borrow: " + response);
    }

    public String returnBook(String bookId) throws IOException, OperationException {
        if (currentUser == null) throw new OperationException("User not logged in.");
        String request = RequestType.RETURN_BOOK.name() + "::" + bookId;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "RETURN_SUCCESSFUL".equals(parts[1])) {
            return parts[2]; // Success message
        } else if ("FAILURE".equals(parts[0])) {
            throw new OperationException(parts.length > 2 ? parts[2] : "Return failed.");
        }
        throw new IOException("Unexpected server response for return: " + response);
    }

    public List<BorrowRecord> getMyBorrowingRecords() throws IOException {
        if (currentUser == null) return Collections.emptyList();
        String response = sendRequestGetResponse(RequestType.VIEW_MY_BORROWING_RECORDS.name());
        return parseBorrowRecordListResponse(response, "MY_BORROWING_RECORDS");
    }

    public List<BorrowRecord> getMyOverdueBooks() throws IOException {
        if (currentUser == null) return Collections.emptyList();
        String response = sendRequestGetResponse(RequestType.GET_MY_OVERDUE_BOOKS.name());
        return parseBorrowRecordListResponse(response, "MY_OVERDUE_BOOKS");
    }

    public List<Book> getMyRecommendations(int limit) throws IOException {
        if (currentUser == null) return Collections.emptyList();
        String request = RequestType.GET_MY_RECOMMENDATIONS.name() + "::" + limit;
        String response = sendRequestGetResponse(request);
        return parseBookListResponse(response, "RECOMMENDATIONS");
    }

    // --- Admin Operations ---
    public String addBook(Book book) throws IOException, OperationException {
        String request = RequestType.ADD_BOOK.name() + "::" + book.getBookId() + "::" + book.getTitle() + "::" +
                book.getAuthor() + "::" + book.getCategory() + "::" + book.getQuantity() + "::" + book.getTotalQuantity();
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "BOOK_ADDED".equals(parts[1])) {
            return "Book added successfully: " + parts[2];
        } else if ("FAILURE".equals(parts[0])) {
            throw new OperationException(parts.length > 2 ? parts[2] : "Add book failed.");
        }
        throw new IOException("Unexpected server response for add book: " + response);
    }

    public String updateBook(Book book) throws IOException, OperationException {
        String request = RequestType.UPDATE_BOOK.name() + "::" + book.getBookId() + "::" + book.getTitle() + "::" +
                book.getAuthor() + "::" + book.getCategory() + "::" + book.getQuantity() + "::" + book.getTotalQuantity();
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "BOOK_UPDATED".equals(parts[1])) {
            return "Book updated successfully: " + parts[2];
        } else if ("FAILURE".equals(parts[0])) {
            throw new OperationException(parts.length > 2 ? parts[2] : "Update book failed.");
        }
        throw new IOException("Unexpected server response for update book: " + response);
    }

    public String deleteBook(String bookId) throws IOException, OperationException {
        String request = RequestType.DELETE_BOOK.name() + "::" + bookId;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "BOOK_DELETED".equals(parts[1])) {
            return "Book deleted successfully: " + parts[2];
        } else if ("FAILURE".equals(parts[0])) {
            throw new OperationException(parts.length > 2 ? parts[2] : "Delete book failed.");
        }
        throw new IOException("Unexpected server response for delete book: " + response);
    }

    public List<User> getAllUsers() throws IOException {
        String response = sendRequestGetResponse(RequestType.GET_ALL_USERS.name());
        String[] parts = response.split("::", -1);
        List<User> users = new ArrayList<>();
        if ("SUCCESS".equals(parts[0]) && "USER_LIST".equals(parts[1])) {
            if (parts.length > 2 && !parts[2].isEmpty() && !"NO_USERS_FOUND".equals(parts[2])) {
                String[] userStrings = parts[2].split(";");
                for (String userStr : userStrings) {
                    String[] fields = userStr.split("\\|");
                    if (fields.length == 4) {
                        String userId = fields[0];
                        String username = fields[1];
                        Role role = Role.valueOf(fields[2].toUpperCase());
                        boolean isActive = "ACTIVE".equalsIgnoreCase(fields[3]);
                        User user = (role == Role.ADMIN) ?
                                new org.example.model.Admin(userId, username, null, isActive) :
                                new org.example.model.NormalUser(userId, username, null, isActive);
                        users.add(user);
                    }
                }
            }
            return users;
        } else if ("SUCCESS".equals(parts[0]) && "NO_USERS_FOUND".equals(parts[1])) {
            return Collections.emptyList();
        }
        throw new IOException("Failed to get users: " + response);
    }

    public String updateUserStatus(String userId, boolean isActive) throws IOException, OperationException {
        String request = RequestType.UPDATE_USER_STATUS.name() + "::" + userId + "::" + isActive;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && "USER_STATUS_UPDATED".equals(parts[1])) {
            return parts[2]; // Success message
        } else if ("FAILURE".equals(parts[0])) {
            throw new OperationException(parts.length > 2 ? parts[2] : "Update user status failed.");
        }
        throw new IOException("Unexpected server response for update user status: " + response);
    }

    public List<BorrowRecord> getAllBorrowingRecords() throws IOException {
        String response = sendRequestGetResponse(RequestType.VIEW_ALL_BORROWING_RECORDS.name());
        return parseBorrowRecordListResponse(response, "ALL_BORROWING_RECORDS");
    }

    public List<String> getPopularBooks(int limit) throws IOException {
        String request = RequestType.GET_POPULAR_BOOKS.name() + "::" + limit;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && parts.length > 2 && "POPULAR_BOOKS_LIST".equals(parts[1])) {
            if (parts[2].isEmpty() || "NO_POPULAR_BOOKS_DATA".equals(parts[2])) return Collections.emptyList();
            return Arrays.asList(parts[2].split(";"));
        } else if ("SUCCESS".equals(parts[0]) && "NO_POPULAR_BOOKS_DATA".equals(parts[1])) {
            return Collections.emptyList();
        }
        throw new IOException("Failed to get popular books: " + response);
    }

    public List<String> getTrendingBooks(int limit, int daysPeriod) throws IOException {
        String request = RequestType.GET_TRENDING_BOOKS.name() + "::" + limit + "::" + daysPeriod;
        String response = sendRequestGetResponse(request);
        String[] parts = response.split("::", -1);
        if ("SUCCESS".equals(parts[0]) && parts.length > 2 && "TRENDING_BOOKS_LIST".equals(parts[1])) {
            if (parts[2].isEmpty() || "NO_TRENDING_BOOKS_DATA".equals(parts[2])) return Collections.emptyList();
            return Arrays.asList(parts[2].split(";"));
        } else if ("SUCCESS".equals(parts[0]) && "NO_TRENDING_BOOKS_DATA".equals(parts[1])) {
            return Collections.emptyList();
        }
        throw new IOException("Failed to get trending books: " + response);
    }


    // --- Helper Parsers ---
    private Book parseBook(String bookData) {
        String[] fields = bookData.split("\\|");
        if (fields.length == 6) {
            try {
                return new Book(fields[0], fields[1], fields[2], fields[3],
                        Integer.parseInt(fields[4]), Integer.parseInt(fields[5]));
            } catch (NumberFormatException e) {
                System.err.println("GUI Client: Error parsing book number field: " + bookData + " - " + e.getMessage());
                return null;
            }
        }
        System.err.println("GUI Client: Incorrect number of fields for book data: " + bookData);
        return null;
    }

    private List<Book> parseBookListResponse(String response) throws IOException {
        return parseBookListResponse(response, "BOOK_LIST");
    }

    private List<Book> parseBookListResponse(String response, String expectedType) throws IOException {
        String[] parts = response.split("::", -1);
        List<Book> books = new ArrayList<>();
        if ("SUCCESS".equals(parts[0]) && parts.length > 1) { // Check length > 1 for parts[1]
            if (expectedType.equals(parts[1]) && parts.length > 2) { // Check length > 2 for parts[2] (payload)
                if (parts[2].isEmpty()) return Collections.emptyList();

                String[] bookStrings = parts[2].split(";");
                for (String bookStr : bookStrings) {
                    Book book = parseBook(bookStr);
                    if (book != null) books.add(book);
                }
                return books;
            } else if (parts[1].startsWith("NO_") || parts[1].endsWith("_FOUND") || parts[1].endsWith("_AVAILABLE") || parts[1].endsWith("_CATEGORIES")) {
                // Handles cases like NO_BOOKS_FOUND, NO_RECOMMENDATIONS_AVAILABLE etc. directly
                return Collections.emptyList();
            }
        }
        System.err.println("GUI Client: Failed to parse book list. Raw response: " + response + " (expected type: " + expectedType + ")");
        throw new IOException("Failed to parse book list. Response: " + response);
    }

    private BorrowRecord parseBorrowRecord(String recordData) {
        String[] fields = recordData.split("\\|");
        if (fields.length >= 6) {
            try {
                int recordId = Integer.parseInt(fields[0]);
                String userId = fields[1];
                String bookId = fields[2];
                LocalDate borrowDate = LocalDate.parse(fields[3], DATE_FORMATTER);
                LocalDate dueDate = LocalDate.parse(fields[4], DATE_FORMATTER);
                LocalDate returnDate = "NOT_RETURNED".equalsIgnoreCase(fields[5]) || fields[5] == null || fields[5].isEmpty() ?
                        null : LocalDate.parse(fields[5], DATE_FORMATTER);
                return new BorrowRecord(recordId, userId, bookId, borrowDate, dueDate, returnDate);
            } catch (Exception e) {
                System.err.println("GUI Client: Error parsing borrow record field: " + recordData + " - " + e.getMessage());
                return null;
            }
        }
        System.err.println("GUI Client: Incorrect number of fields for borrow record data: " + recordData);
        return null;
    }

    private List<BorrowRecord> parseBorrowRecordListResponse(String response, String expectedType) throws IOException {
        String[] parts = response.split("::", -1);
        List<BorrowRecord> records = new ArrayList<>();
        if ("SUCCESS".equals(parts[0]) && parts.length > 1) { // Check length > 1 for parts[1]
            if (expectedType.equals(parts[1]) && parts.length > 2) { // Check length > 2 for parts[2] (payload)
                if (parts[2].isEmpty()) return Collections.emptyList();

                String[] recordStrings = parts[2].split(";");
                for (String recordStr : recordStrings) {
                    BorrowRecord record = parseBorrowRecord(recordStr);
                    if (record != null) records.add(record);
                }
                return records;
            } else if (parts[1].startsWith("NO_")) { // Handles NO_BORROWING_RECORDS_FOUND etc.
                return Collections.emptyList();
            }
        }
        System.err.println("GUI Client: Failed to parse borrow record list. Raw response: " + response + " (expected type: " + expectedType + ")");
        throw new IOException("Failed to parse borrow record list. Response: " + response);
    }

    // Custom Exceptions
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) { super(message); }
    }
    public static class RegistrationException extends Exception {
        public RegistrationException(String message) { super(message); }
    }
    public static class OperationException extends Exception {
        public OperationException(String message) { super(message); }
    }
}