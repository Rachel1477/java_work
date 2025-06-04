package org.example.client.controller;

import org.example.client.MainApp;
import org.example.client.ClientService;
import org.example.util.AlertUtil;
import org.example.model.Book;
import org.example.model.BorrowRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException; // Import IOException
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private ComboBox<String> searchFieldComboBox;
    @FXML private TextField searchTermField;
    @FXML private TableView<Book> booksTableView;
    @FXML private TableColumn<Book, String> bookIdCol;
    @FXML private TableColumn<Book, String> bookTitleCol;
    @FXML private TableColumn<Book, String> bookAuthorCol;
    @FXML private TableColumn<Book, String> bookCategoryCol;
    @FXML private TableColumn<Book, Integer> bookAvailableCol;

    @FXML private TableView<BorrowRecord> myBorrowsTableView;
    @FXML private TableColumn<BorrowRecord, Integer> borrowRecordIdCol;
    @FXML private TableColumn<BorrowRecord, String> borrowBookIdCol;
    @FXML private TableColumn<BorrowRecord, LocalDate> borrowDateCol;
    @FXML private TableColumn<BorrowRecord, LocalDate> dueDateCol;
    @FXML private TableColumn<BorrowRecord, LocalDate> returnDateCol;
    @FXML private TableColumn<BorrowRecord, String> borrowStatusCol;

    @FXML private ListView<String> recommendationsListView;
    @FXML private Spinner<Integer> recommendationsCountSpinner;
    @FXML private Label overdueLabel;


    private MainApp mainApp;
    private ClientService clientService;
    private ObservableList<Book> booksData = FXCollections.observableArrayList();
    private ObservableList<BorrowRecord> myBorrowsData = FXCollections.observableArrayList();
    private ObservableList<String> recommendationsData = FXCollections.observableArrayList();


    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.clientService = ClientService.getInstance();
        if (clientService.getCurrentUser() != null) {
            welcomeLabel.setText("Welcome, " + clientService.getCurrentUser().getUsername() + "!");
        }
        loadInitialData();
    }

    @FXML
    private void initialize() {
        // Book Table
        bookIdCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        bookTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        bookAuthorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        bookCategoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        bookAvailableCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        booksTableView.setItems(booksData);

        searchFieldComboBox.setItems(FXCollections.observableArrayList("Title", "Author", "Category"));
        searchFieldComboBox.setValue("Title");

        // My Borrows Table
        borrowRecordIdCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        borrowBookIdCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        borrowDateCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        returnDateCol.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        borrowStatusCol.setCellValueFactory(cellData -> {
            BorrowRecord record = cellData.getValue();
            if (record.getReturnDate() != null) return javafx.beans.binding.Bindings.createStringBinding(() -> "Returned");
            return javafx.beans.binding.Bindings.createStringBinding(() -> record.isOverdue() ? "OVERDUE" : "Borrowed");
        });
        myBorrowsTableView.setItems(myBorrowsData);

        recommendationsListView.setItems(recommendationsData);
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 5);
        recommendationsCountSpinner.setValueFactory(valueFactory);
    }

    private void loadInitialData() {
        handleShowAllBooks();
        handleRefreshMyBorrows();
        checkOverdueBooks();
    }

    @FXML
    private void handleShowAllBooks() {
        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() throws Exception {
                return clientService.getAllBooks();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> booksData.setAll(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Books Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleSearchBooks() {
        String field = searchFieldComboBox.getValue().toLowerCase();
        String term = searchTermField.getText();
        if (term.isEmpty()) {
            handleShowAllBooks();
            return;
        }
        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() throws Exception {
                return clientService.searchBooks(field, term);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> booksData.setAll(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Search Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleBorrowBook() {
        Book selectedBook = booksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            AlertUtil.showWarning("No Selection", "No Book Selected", "Please select a book to borrow.");
            return;
        }
        if (selectedBook.getQuantity() <= 0) {
            AlertUtil.showInfo("Out of Stock", "This book is currently out of stock.");
            return;
        }

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return clientService.borrowBook(selectedBook.getBookId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AlertUtil.showInfo("Borrow Success", task.getValue());
            handleShowAllBooks();
            handleRefreshMyBorrows();
            checkOverdueBooks();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Borrow Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleReturnBook() {
        BorrowRecord selectedRecord = myBorrowsTableView.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            AlertUtil.showWarning("No Selection", "No Borrow Record Selected", "Please select a borrowing record to mark a book as returned.");
            return;
        }
        if (selectedRecord.getReturnDate() != null) {
            AlertUtil.showInfo("Already Returned", "This book has already been marked as returned.");
            return;
        }

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return clientService.returnBook(selectedRecord.getBookId());
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            AlertUtil.showInfo("Return Success", task.getValue());
            handleShowAllBooks();
            handleRefreshMyBorrows();
            checkOverdueBooks();
        }));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Return Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleRefreshMyBorrows() {
        Task<List<BorrowRecord>> task = new Task<>() {
            @Override protected List<BorrowRecord> call() throws Exception {
                return clientService.getMyBorrowingRecords();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            myBorrowsData.setAll(task.getValue());
            checkOverdueBooks(); // Re-check overdue books after refreshing borrow list
        }));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Borrows Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleGetRecommendations() {
        int limit = recommendationsCountSpinner.getValue();
        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() throws Exception {
                return clientService.getMyRecommendations(limit);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Book> recommended = task.getValue();
            if (recommended.isEmpty()) {
                recommendationsData.setAll("No recommendations available at the moment.");
            } else {
                recommendationsData.setAll(
                        recommended.stream()
                                .map(b -> b.getTitle() + " by " + b.getAuthor() + " (Category: " + b.getCategory() + ")")
                                .collect(Collectors.toList())
                );
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            AlertUtil.showError("Recommendations Failed", task.getException().getMessage());
            recommendationsData.setAll("Error fetching recommendations.");
        }));
        new Thread(task).start();
    }

    // Helper method to fetch book title, handling potential IOException
    private String getBookTitleSafely(String bookId) {
        try {
            Book book = clientService.getBookById(bookId);
            return (book != null) ? book.getTitle() : "Unknown Title (ID: " + bookId + ")";
        } catch (IOException e) {
            System.err.println("Error fetching book title for ID " + bookId + ": " + e.getMessage());
            return "Title Unavailable (ID: " + bookId + ")";
        }
    }

    private void checkOverdueBooks() {
        Task<List<BorrowRecord>> task = new Task<>() {
            @Override protected List<BorrowRecord> call() throws Exception {
                return clientService.getMyOverdueBooks(); // This fetches the records
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<BorrowRecord> overdue = task.getValue();
            if (overdue != null && !overdue.isEmpty()) {
                // Fetching book titles can be slow if done one by one synchronously on FX thread.
                // For a small number of overdue books, this might be acceptable.
                // For many, consider batching or a different approach.
                String overdueBooksString = overdue.stream()
                        .map(br -> getBookTitleSafely(br.getBookId())) // Use helper method
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", "));

                overdueLabel.setText("OVERDUE BOOKS: " + overdue.size() + " item(s). Please return them soon!");
                AlertUtil.showWarning("Overdue Books", "You have " + overdue.size() + " overdue book(s)!",
                        "Books: " + (overdueBooksString.isEmpty() ? "Details unavailable" : overdueBooksString) + ". Please return them as soon as possible.");
            } else {
                overdueLabel.setText("");
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            overdueLabel.setText("Error checking overdue books.");
            // Optionally show an alert for this failure too
            // AlertUtil.showError("Overdue Check Failed", "Could not check for overdue books: " + task.getException().getMessage());
            System.err.println("Failed to check overdue books: " + task.getException().getMessage());
        }));
        new Thread(task).start();
    }


    @FXML
    private void handleLogout() {
        // Perform logout actions, e.g., notify server, clear session
        clientService.logout(); // Clears currentUser in ClientService
        mainApp.showLoginView();
    }
}