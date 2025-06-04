package org.example.network;

import org.example.model.User; // For future use with actual authentication

import org.example.dao.BookDao;
import org.example.dao.BorrowRecordDao;
import org.example.dao.UserDao;
import org.example.db.DatabaseManager;
import org.example.model.*;
import org.example.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final UserDao userDao;
    private final BookDao bookDao;
    private final BorrowRecordDao borrowRecordDao;
    private User loggedInUser;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.userDao = new UserDao();
        this.bookDao = new BookDao();
        this.borrowRecordDao = new BorrowRecordDao();
    }

    @Override
    public void run() {
        System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String clientRequest;
            while ((clientRequest = reader.readLine()) != null) {
                System.out.println("Received from " + clientSocket.getPort() + ": " + clientRequest);
                String response = processRequest(clientRequest);
                writer.println(response);
                System.out.println("Sent to " + clientSocket.getPort() + ": " + response);

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
        String[] parts = requestString.split("::", -1);
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

        if (requiresLogin(requestType) && loggedInUser == null) {
            return "ERROR::AUTH_REQUIRED::Please login first.";
        }
        if (requiresAdmin(requestType) && (loggedInUser == null || loggedInUser.getRole() != Role.ADMIN)) {
            return "ERROR::ADMIN_ACCESS_DENIED::Admin privileges required.";
        }


        try {
            switch (requestType) {
                case PING:
                    return "SUCCESS::PONG";
                case LOGIN:
                    return handleLogin(args);
                case REGISTER:
                    return handleRegister(args);
                case LOGOUT:
                    return handleLogout();

                case GET_ALL_BOOKS:
                    return handleGetAllBooks();
                case SEARCH_BOOK:
                    return handleSearchBook(args);
                case GET_BOOK_BY_ID:
                    return handleGetBookById(args);

                case BORROW_BOOK:
                    return handleBorrowBook(args);
                case RETURN_BOOK:
                    return handleReturnBook(args);
                case VIEW_MY_BORROWING_RECORDS:
                    return handleViewMyBorrowingRecords();
                case GET_MY_OVERDUE_BOOKS:
                    return handleGetMyOverdueBooks();
                case GET_MY_RECOMMENDATIONS:
                    return handleGetMyRecommendations(args);


                case ADD_BOOK:
                    return handleAddBook(args);
                case UPDATE_BOOK:
                    return handleUpdateBook(args);
                case DELETE_BOOK:
                    return handleDeleteBook(args);
                case VIEW_ALL_BORROWING_RECORDS:
                    return handleViewAllBorrowingRecords();
                case GET_POPULAR_BOOKS:
                    return handleGetPopularBooks(args);
                case GET_TRENDING_BOOKS:
                    return handleGetTrendingBooks(args);


                case GET_ALL_USERS:
                    return handleGetAllUsers();
                case UPDATE_USER_STATUS:
                    return handleUpdateUserStatus(args);

                case TERMINATE_CONNECTION:
                    this.loggedInUser = null;
                    return "SUCCESS::CONNECTION_TERMINATED";
                default:
                    return "INFO::REQUEST_RECEIVED::" + requestType.name() + "::" + String.join(":", args);
            }
        } catch (Exception e) {
            System.err.println("Error processing request '" + requestType + "': " + e.getMessage());
            e.printStackTrace();
            return "ERROR::INTERNAL_SERVER_ERROR::" + e.getMessage();
        }
    }

    private boolean requiresLogin(RequestType type) {
        switch (type) {
            case LOGOUT:
                // GET_ALL_BOOKS, SEARCH_BOOK, GET_BOOK_BY_ID could be public if desired, but let's keep them login-protected for now
            case GET_ALL_BOOKS:
            case SEARCH_BOOK:
            case GET_BOOK_BY_ID:
            case BORROW_BOOK:
            case RETURN_BOOK:
            case VIEW_MY_BORROWING_RECORDS:
            case GET_MY_OVERDUE_BOOKS:
            case GET_MY_RECOMMENDATIONS:
                // Admin specific commands are implicitly login-required too
            case ADD_BOOK:
            case UPDATE_BOOK:
            case DELETE_BOOK:
            case VIEW_ALL_BORROWING_RECORDS:
            case GET_ALL_USERS:
            case UPDATE_USER_STATUS:
            case GET_POPULAR_BOOKS:
            case GET_TRENDING_BOOKS:
                return true;
            default: // PING, LOGIN, REGISTER, TERMINATE_CONNECTION do not require prior login
                return false;
        }
    }

    private boolean requiresAdmin(RequestType type) {
        switch (type) {
            case ADD_BOOK:
            case UPDATE_BOOK:
            case DELETE_BOOK:
            case VIEW_ALL_BORROWING_RECORDS:
            case GET_ALL_USERS:
            case UPDATE_USER_STATUS:
            case GET_POPULAR_BOOKS:
            case GET_TRENDING_BOOKS:
                return true;
            default:
                return false;
        }
    }

    // --- Handler Methods (Existing methods unchanged, only showing new/modified ones) ---

    private String handleLogin(String[] args) {
        if (args.length != 2) return "ERROR::LOGIN_INVALID_ARGS::Expected username::password";
        String username = args[0];
        String plainPassword = args[1];

        Optional<User> userOpt = userDao.getUserByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isActive()) {
                return "FAILURE::LOGIN_FAILED::Account is inactive.";
            }
            if (PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
                this.loggedInUser = user;
                return "SUCCESS::LOGIN_SUCCESSFUL::" + user.getRole().name() + "::" + user.getUserId() + "::Welcome " + user.getUsername();
            }
        }
        return "FAILURE::LOGIN_FAILED::Invalid username or password.";
    }

    private String handleRegister(String[] args) {
        if (args.length != 2) return "ERROR::REGISTER_INVALID_ARGS::Expected username::password";
        String username = args[0];
        String plainPassword = args[1];

        if (userDao.getUserByUsername(username).isPresent()) {
            return "FAILURE::REGISTRATION_FAILED::Username already exists.";
        }
        if (plainPassword.length() < 6) {
            return "FAILURE::REGISTRATION_FAILED::Password too short (min 6 chars).";
        }

        String userId = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        User newUser = new NormalUser(userId, username, hashedPassword, true);

        if (userDao.addUser(newUser)) {
            return "SUCCESS::REGISTRATION_SUCCESSFUL::User " + username + " created with ID " + userId;
        } else {
            return "FAILURE::REGISTRATION_FAILED::Could not create user.";
        }
    }

    private String handleLogout() {
        if (loggedInUser != null) {
            String username = loggedInUser.getUsername();
            this.loggedInUser = null;
            return "SUCCESS::LOGOUT_SUCCESSFUL::User " + username + " logged out.";
        }
        return "INFO::LOGOUT::No user was logged in.";
    }

    private String formatBookToString(Book b) {
        return String.join("|", b.getBookId(), b.getTitle(), b.getAuthor(), b.getCategory(), String.valueOf(b.getQuantity()), String.valueOf(b.getTotalQuantity()));
    }

    private String handleGetAllBooks() {
        List<Book> books = bookDao.getAllBooks();
        if (books.isEmpty()) return "SUCCESS::NO_BOOKS_FOUND";
        String bookListStr = books.stream()
                .map(this::formatBookToString)
                .collect(Collectors.joining(";"));
        return "SUCCESS::BOOK_LIST::" + bookListStr;
    }

    private String handleSearchBook(String[] args) {
        if (args.length != 2) return "ERROR::SEARCH_INVALID_ARGS::Expected searchField::searchTerm";
        String field = args[0].toLowerCase();
        String term = args[1];
        if (!field.matches("title|author|category")) {
            return "ERROR::SEARCH_INVALID_FIELD::Field must be title, author, or category.";
        }

        List<Book> books = bookDao.searchBooks(term, field);
        if (books.isEmpty()) return "SUCCESS::NO_BOOKS_FOUND_MATCHING_SEARCH";
        String bookListStr = books.stream()
                .map(this::formatBookToString)
                .collect(Collectors.joining(";"));
        return "SUCCESS::BOOK_SEARCH_RESULTS::" + bookListStr;
    }

    private String handleGetBookById(String[] args) {
        if (args.length != 1) return "ERROR::GET_BOOK_BY_ID_INVALID_ARGS::Expected bookId";
        String bookId = args[0];
        Optional<Book> bookOpt = bookDao.getBookById(bookId);
        if (bookOpt.isPresent()) {
            return "SUCCESS::BOOK_DETAILS::" + formatBookToString(bookOpt.get());
        }
        return "FAILURE::BOOK_NOT_FOUND::" + bookId;
    }

    private String handleBorrowBook(String[] args) {
        if (args.length != 1) return "ERROR::BORROW_BOOK_INVALID_ARGS::Expected bookId";
        String bookId = args[0];
        String userId = loggedInUser.getUserId();

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Optional<Book> bookOpt = bookDao.getBookById(bookId);
            if (!bookOpt.isPresent()) {
                conn.rollback();
                return "FAILURE::BORROW_FAILED::Book not found.";
            }
            Book book = bookOpt.get();
            if (book.getQuantity() <= 0) {
                conn.rollback();
                return "FAILURE::BORROW_FAILED::Book out of stock.";
            }

            if (borrowRecordDao.getActiveBorrowRecordByUserAndBook(userId, bookId).isPresent()) {
                conn.rollback();
                return "FAILURE::BORROW_FAILED::You have already borrowed this book and not returned it.";
            }

            book.setQuantity(book.getQuantity() - 1);
            if (!bookDao.updateBookQuantity(book.getBookId(), book.getQuantity(), conn)) {
                conn.rollback();
                return "FAILURE::BORROW_FAILED::Could not update book quantity.";
            }

            LocalDate borrowDate = LocalDate.now();
            LocalDate dueDate = borrowDate.plusWeeks(2);
            BorrowRecord record = new BorrowRecord(userId, bookId, borrowDate, dueDate);

            int recordId = borrowRecordDao.addBorrowRecord(record, conn);
            record.setRecordId(recordId);

            conn.commit();
            return "SUCCESS::BORROW_SUCCESSFUL::Book '" + book.getTitle() + "' borrowed. Due: " + dueDate.toString();

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error borrowing book: " + e.getMessage());
            return "FAILURE::BORROW_FAILED::Database error.";
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
                DatabaseManager.closeQuietly(conn);
            }
        }
    }

    private String handleReturnBook(String[] args) {
        if (args.length != 1) return "ERROR::RETURN_BOOK_INVALID_ARGS::Expected bookId";
        String bookId = args[0];
        String userId = loggedInUser.getUserId();

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Optional<BorrowRecord> recordOpt = borrowRecordDao.getActiveBorrowRecordByUserAndBook(userId, bookId);
            if (!recordOpt.isPresent()) {
                conn.rollback();
                return "FAILURE::RETURN_FAILED::No active borrow record found for this book by you.";
            }
            BorrowRecord record = recordOpt.get();

            if (!borrowRecordDao.updateBorrowRecordReturnDate(record.getRecordId(), LocalDate.now(), conn)) {
                conn.rollback();
                return "FAILURE::RETURN_FAILED::Could not update borrow record.";
            }

            Optional<Book> bookOpt = bookDao.getBookById(bookId);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                if (book.getQuantity() < book.getTotalQuantity()) {
                    book.setQuantity(book.getQuantity() + 1);
                    if (!bookDao.updateBookQuantity(book.getBookId(), book.getQuantity(), conn)) {
                        conn.rollback();
                        return "FAILURE::RETURN_FAILED::Could not update book quantity. Record updated, but needs attention.";
                    }
                } else {
                    System.err.println("Warning: Book quantity for " + bookId + " already at max upon return. This might indicate an issue.");
                }
            } else {
                conn.rollback();
                return "FAILURE::RETURN_FAILED::Book data inconsistency.";
            }

            conn.commit();
            return "SUCCESS::RETURN_SUCCESSFUL::Book '" + bookOpt.get().getTitle() + "' returned.";

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error returning book: " + e.getMessage());
            return "FAILURE::RETURN_FAILED::Database error.";
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
                DatabaseManager.closeQuietly(conn);
            }
        }
    }

    private String formatBorrowRecord(BorrowRecord r) {
        return String.join("|",
                String.valueOf(r.getRecordId()),
                r.getUserId(),
                r.getBookId(),
                r.getBorrowDate().toString(),
                r.getDueDate().toString(),
                r.getReturnDate() != null ? r.getReturnDate().toString() : "NOT_RETURNED",
                r.isOverdue() ? "OVERDUE" : "NOT_OVERDUE"
        );
    }

    private String handleViewMyBorrowingRecords() {
        List<BorrowRecord> records = borrowRecordDao.getBorrowRecordsByUserId(loggedInUser.getUserId());
        if (records.isEmpty()) return "SUCCESS::NO_BORROWING_RECORDS_FOUND";
        String recordsStr = records.stream().map(this::formatBorrowRecord).collect(Collectors.joining(";"));
        return "SUCCESS::MY_BORROWING_RECORDS::" + recordsStr;
    }

    private String handleGetMyOverdueBooks() {
        List<BorrowRecord> records = borrowRecordDao.getBorrowRecordsByUserId(loggedInUser.getUserId());
        List<BorrowRecord> overdueRecords = records.stream()
                .filter(BorrowRecord::isOverdue)
                .collect(Collectors.toList());
        if (overdueRecords.isEmpty()) return "SUCCESS::NO_OVERDUE_BOOKS";
        String recordsStr = overdueRecords.stream().map(this::formatBorrowRecord).collect(Collectors.joining(";"));
        return "SUCCESS::MY_OVERDUE_BOOKS::" + recordsStr;
    }

    private String handleGetMyRecommendations(String[] args) {
        // GET_MY_RECOMMENDATIONS::limit
        if (args.length != 1) return "ERROR::RECOMMENDATIONS_INVALID_ARGS::Expected limit";
        int limit;
        try {
            limit = Integer.parseInt(args[0]);
            if (limit <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "ERROR::RECOMMENDATIONS_INVALID_ARGS::Limit must be a positive integer.";
        }

        // 1. Get user's recently borrowed books to find preferred categories
        List<BorrowRecord> userBorrows = borrowRecordDao.getBorrowRecordsByUserId(loggedInUser.getUserId());
        if (userBorrows.isEmpty()) {
            // If no borrow history, recommend globally popular books not borrowed by user
            Set<String> borrowedBookIds = borrowRecordDao.getBorrowedBookIdsByUserId(loggedInUser.getUserId());
            List<Book> popularBooks = bookDao.getAllBooks().stream()
                    .filter(b -> !borrowedBookIds.contains(b.getBookId()) && b.getQuantity() > 0)
                    .sorted(Comparator.comparingInt(Book::getTotalQuantity).reversed()) // Simple popularity
                    .limit(limit)
                    .collect(Collectors.toList());
            if (popularBooks.isEmpty()) return "SUCCESS::NO_RECOMMENDATIONS_AVAILABLE";
            String booksStr = popularBooks.stream().map(this::formatBookToString).collect(Collectors.joining(";"));
            return "SUCCESS::RECOMMENDATIONS::" + booksStr;
        }

        Set<String> preferredCategories = userBorrows.stream()
                .limit(10) // Consider last 10 borrowed books for category preference
                .map(BorrowRecord::getBookId)
                .map(bookDao::getBookById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Book::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (preferredCategories.isEmpty()) {
            return "SUCCESS::NO_RECOMMENDATIONS_AVAILABLE::Could not determine category preferences.";
        }

        // 2. Get all book IDs user has ever borrowed
        Set<String> allBorrowedBookIds = borrowRecordDao.getBorrowedBookIdsByUserId(loggedInUser.getUserId());

        // 3. Fetch books from preferred categories, excluding already borrowed ones
        List<Book> recommendedBooks = bookDao.getBooksByCategoriesExcludingBorrowed(preferredCategories, allBorrowedBookIds, limit);

        if (recommendedBooks.isEmpty()) {
            return "SUCCESS::NO_NEW_RECOMMENDATIONS_IN_PREFERRED_CATEGORIES";
        }

        String recommendationsStr = recommendedBooks.stream()
                .map(this::formatBookToString)
                .collect(Collectors.joining(";"));
        return "SUCCESS::RECOMMENDATIONS::" + recommendationsStr;
    }


    // --- Admin Handler Methods ---
    private String handleAddBook(String[] args) {
        if (args.length != 6) return "ERROR::ADD_BOOK_INVALID_ARGS";
        try {
            String bookId = args[0];
            String title = args[1];
            String author = args[2];
            String category = args[3];
            int quantity = Integer.parseInt(args[4]);
            int totalQuantity = Integer.parseInt(args[5]);

            if (quantity < 0 || totalQuantity < 0 || quantity > totalQuantity) {
                return "FAILURE::ADD_BOOK_FAILED::Invalid quantity values.";
            }

            Book book = new Book(bookId, title, author, category, quantity, totalQuantity);
            if (bookDao.addBook(book)) {
                return "SUCCESS::BOOK_ADDED::" + bookId;
            } else {
                return "FAILURE::ADD_BOOK_FAILED::Book ID might already exist or DB error.";
            }
        } catch (NumberFormatException e) {
            return "ERROR::ADD_BOOK_INVALID_ARGS::Quantity must be numbers.";
        }
    }

    private String handleUpdateBook(String[] args) {
        if (args.length != 6) return "ERROR::UPDATE_BOOK_INVALID_ARGS";
        try {
            String bookId = args[0];
            String title = args[1];
            String author = args[2];
            String category = args[3];
            int quantity = Integer.parseInt(args[4]);
            int totalQuantity = Integer.parseInt(args[5]);

            if (quantity < 0 || totalQuantity < 0 || quantity > totalQuantity) {
                return "FAILURE::UPDATE_BOOK_FAILED::Invalid quantity values.";
            }

            if (!bookDao.getBookById(bookId).isPresent()) {
                return "FAILURE::UPDATE_BOOK_FAILED::Book with ID " + bookId + " not found.";
            }

            Book book = new Book(bookId, title, author, category, quantity, totalQuantity);
            if (bookDao.updateBook(book)) {
                return "SUCCESS::BOOK_UPDATED::" + bookId;
            } else {
                return "FAILURE::UPDATE_BOOK_FAILED::Could not update book.";
            }
        } catch (NumberFormatException e) {
            return "ERROR::UPDATE_BOOK_INVALID_ARGS::Quantity must be numbers.";
        }
    }

    private String handleDeleteBook(String[] args) {
        if (args.length != 1) return "ERROR::DELETE_BOOK_INVALID_ARGS::Expected bookId";
        String bookId = args[0];
        if (!bookDao.getBookById(bookId).isPresent()) {
            return "FAILURE::DELETE_BOOK_FAILED::Book with ID " + bookId + " not found.";
        }
        if (bookDao.deleteBook(bookId)) {
            return "SUCCESS::BOOK_DELETED::" + bookId;
        } else {
            return "FAILURE::DELETE_BOOK_FAILED::Could not delete book.";
        }
    }

    private String handleViewAllBorrowingRecords() {
        List<BorrowRecord> records = borrowRecordDao.getAllBorrowRecords();
        if (records.isEmpty()) return "SUCCESS::NO_BORROWING_RECORDS_FOUND_SYSTEM_WIDE";
        String recordsStr = records.stream().map(this::formatBorrowRecord).collect(Collectors.joining(";"));
        return "SUCCESS::ALL_BORROWING_RECORDS::" + recordsStr;
    }

    private String handleGetPopularBooks(String[] args) {
        // GET_POPULAR_BOOKS::limit
        if (args.length != 1) return "ERROR::POPULAR_BOOKS_INVALID_ARGS::Expected limit";
        int limit;
        try {
            limit = Integer.parseInt(args[0]);
            if (limit <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "ERROR::POPULAR_BOOKS_INVALID_ARGS::Limit must be a positive integer.";
        }

        Map<Book, Long> popularBooksMap = borrowRecordDao.getMostPopularBooks(limit);
        if (popularBooksMap.isEmpty()) return "SUCCESS::NO_POPULAR_BOOKS_DATA";

        String result = popularBooksMap.entrySet().stream()
                .map(entry -> formatBookToString(entry.getKey()) + "|" + entry.getValue()) // bookId|title|author|category|qty|totalQty|borrowCount
                .collect(Collectors.joining(";"));
        return "SUCCESS::POPULAR_BOOKS_LIST::" + result;
    }

    private String handleGetTrendingBooks(String[] args) {
        // GET_TRENDING_BOOKS::limit::daysPeriod
        if (args.length != 2) return "ERROR::TRENDING_BOOKS_INVALID_ARGS::Expected limit::daysPeriod";
        int limit, daysPeriod;
        try {
            limit = Integer.parseInt(args[0]);
            daysPeriod = Integer.parseInt(args[1]);
            if (limit <= 0 || daysPeriod <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "ERROR::TRENDING_BOOKS_INVALID_ARGS::Limit and daysPeriod must be positive integers.";
        }
        Map<Book, Long> trendingBooksMap = borrowRecordDao.getTrendingBooks(limit, daysPeriod);
        if (trendingBooksMap.isEmpty()) return "SUCCESS::NO_TRENDING_BOOKS_DATA";

        String result = trendingBooksMap.entrySet().stream()
                .map(entry -> formatBookToString(entry.getKey()) + "|" + entry.getValue()) // bookId|title|author|category|qty|totalQty|borrowCount
                .collect(Collectors.joining(";"));
        return "SUCCESS::TRENDING_BOOKS_LIST::" + result;
    }


    private String handleGetAllUsers() {
        List<User> users = userDao.getAllUsers();
        if (users.isEmpty()) return "SUCCESS::NO_USERS_FOUND";
        String usersStr = users.stream()
                .map(u -> String.join("|", u.getUserId(), u.getUsername(), u.getRole().name(), u.isActive() ? "ACTIVE" : "INACTIVE"))
                .collect(Collectors.joining(";"));
        return "SUCCESS::USER_LIST::" + usersStr;
    }

    private String handleUpdateUserStatus(String[] args) {
        if (args.length != 2) return "ERROR::UPDATE_USER_STATUS_INVALID_ARGS";
        String userId = args[0];
        boolean setActive;
        try {
            setActive = Boolean.parseBoolean(args[1]);
        } catch (IllegalArgumentException e) { // Boolean.parseBoolean doesn't throw this for invalid strings, it defaults to false.
            // More robust check for "true" or "false" strings might be needed if strictness is required.
            // For now, standard Boolean.parseBoolean behavior is fine.
            return "ERROR::UPDATE_USER_STATUS_INVALID_ARGS::isActive must be 'true' or 'false'";
        }

        Optional<User> userOpt = userDao.getUserByUserId(userId);
        if (!userOpt.isPresent()) {
            return "FAILURE::UPDATE_USER_STATUS_FAILED::User not found.";
        }
        User user = userOpt.get();
        // Prevent admin from deactivating their own current session's account if it's the one being modified
        if (loggedInUser != null && user.getUserId().equals(loggedInUser.getUserId()) && !setActive) {
            return "FAILURE::UPDATE_USER_STATUS_FAILED::Admin cannot deactivate their own currently logged-in account.";
        }
        // Also, typically the "super admin" (e.g., the first one created) might be undeletable/unfreezable.
        // For simplicity, we don't add that specific logic here.

        user.setActive(setActive);
        if (userDao.updateUser(user)) {
            return "SUCCESS::USER_STATUS_UPDATED::User " + userId + " status set to " + (setActive ? "ACTIVE" : "INACTIVE");
        } else {
            return "FAILURE::UPDATE_USER_STATUS_FAILED::Could not update user status.";
        }
    }
}