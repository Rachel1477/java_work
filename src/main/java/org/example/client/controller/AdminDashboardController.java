package org.example.client.controller;

import org.example.client.MainApp;
import org.example.client.ClientService;
import org.example.util.AlertUtil;
import org.example.model.Book;
import org.example.model.BorrowRecord;
import org.example.model.Role;
import org.example.model.User;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets; // <<< FIX 1: Import Insets
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
// Removed javafx.util.Pair as it wasn't used

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // <<< FIX 2: Import Collectors

public class AdminDashboardController {

    @FXML private Label welcomeLabelAdmin;

    // Book Management Tab
    @FXML private TableView<Book> allBooksTableView;
    @FXML private TableColumn<Book, String> adminBookIdCol;
    @FXML private TableColumn<Book, String> adminBookTitleCol;
    @FXML private TableColumn<Book, String> adminBookAuthorCol;
    @FXML private TableColumn<Book, String> adminBookCategoryCol;
    @FXML private TableColumn<Book, Integer> adminBookAvailableCol;
    @FXML private TableColumn<Book, Integer> adminBookTotalCol;
    private ObservableList<Book> allBooksData = FXCollections.observableArrayList();

    // User Management Tab
    @FXML private TableView<User> allUsersTableView;
    @FXML private TableColumn<User, String> adminUserIdCol;
    @FXML private TableColumn<User, String> adminUsernameCol;
    @FXML private TableColumn<User, Role> adminUserRoleCol;
    @FXML private TableColumn<User, String> adminUserStatusCol;
    private ObservableList<User> allUsersData = FXCollections.observableArrayList();

    // All Borrowing Records Tab
    @FXML private TableView<BorrowRecord> allBorrowsTableView;
    @FXML private TableColumn<BorrowRecord, Integer> adminBorrowRecIdCol;
    @FXML private TableColumn<BorrowRecord, String> adminBorrowUserIdCol;
    @FXML private TableColumn<BorrowRecord, String> adminBorrowBookIdCol;
    @FXML private TableColumn<BorrowRecord, LocalDate> adminBorrowDateCol;
    @FXML private TableColumn<BorrowRecord, LocalDate> adminDueDateCol;
    @FXML private TableColumn<BorrowRecord, LocalDate> adminReturnDateCol;
    @FXML private TableColumn<BorrowRecord, String> adminBorrowStatusCol;
    private ObservableList<BorrowRecord> allBorrowsData = FXCollections.observableArrayList();

    // Statistics Tab
    @FXML private Spinner<Integer> popularBooksLimitSpinner;
    @FXML private ListView<String> popularBooksListView;
    @FXML private Spinner<Integer> trendingBooksLimitSpinner;
    @FXML private Spinner<Integer> trendingBooksDaysSpinner;
    @FXML private ListView<String> trendingBooksListView;
    private ObservableList<String> popularBooksDisplayData = FXCollections.observableArrayList();
    private ObservableList<String> trendingBooksDisplayData = FXCollections.observableArrayList();


    private MainApp mainApp;
    private ClientService clientService;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.clientService = ClientService.getInstance();
        if (clientService.getCurrentUser() != null) {
            welcomeLabelAdmin.setText("Admin Dashboard - User: " + clientService.getCurrentUser().getUsername());
        }
        loadInitialAdminData();
    }

    @FXML
    private void initialize() {
        // Book Management Table
        adminBookIdCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        adminBookTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        adminBookAuthorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        adminBookCategoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        adminBookAvailableCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        adminBookTotalCol.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));
        allBooksTableView.setItems(allBooksData);

        // User Management Table
        adminUserIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        adminUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        adminUserRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        adminUserStatusCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().isActive() ? "Active" : "Inactive")
        );
        allUsersTableView.setItems(allUsersData);

        // All Borrow Records Table
        adminBorrowRecIdCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        adminBorrowUserIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        adminBorrowBookIdCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        adminBorrowDateCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        adminDueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        adminReturnDateCol.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        adminBorrowStatusCol.setCellValueFactory(cellData -> {
            BorrowRecord record = cellData.getValue();
            if (record.getReturnDate() != null) return javafx.beans.binding.Bindings.createStringBinding(() -> "Returned");
            return javafx.beans.binding.Bindings.createStringBinding(() -> record.isOverdue() ? "OVERDUE" : "Borrowed");
        });
        allBorrowsTableView.setItems(allBorrowsData);

        // Statistics
        popularBooksListView.setItems(popularBooksDisplayData);
        trendingBooksListView.setItems(trendingBooksDisplayData);
    }

    private void loadInitialAdminData() {
        handleRefreshAllBooks();
        handleRefreshAllUsers();
        handleRefreshAllBorrowRecords();
    }

    @FXML
    private void handleRefreshAllBooks() {
        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() throws Exception {
                return clientService.getAllBooks();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> allBooksData.setAll(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Books Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleAddBookAction() {
        Optional<Book> result = showBookDialog(null);
        result.ifPresent(book -> {
            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return clientService.addBook(book);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                AlertUtil.showInfo("Book Added", task.getValue());
                handleRefreshAllBooks();
            }));
            task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Add Book Failed", task.getException().getMessage())));
            new Thread(task).start();
        });
    }

    @FXML
    private void handleEditBookAction() {
        Book selectedBook = allBooksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            AlertUtil.showWarning("No Selection", "No Book Selected", "Please select a book to edit.");
            return;
        }
        Optional<Book> result = showBookDialog(selectedBook);
        result.ifPresent(book -> {
            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return clientService.updateBook(book);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                AlertUtil.showInfo("Book Updated", task.getValue());
                handleRefreshAllBooks();
            }));
            task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Update Book Failed", task.getException().getMessage())));
            new Thread(task).start();
        });
    }

    @FXML
    private void handleDeleteBookAction() {
        Book selectedBook = allBooksTableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            AlertUtil.showWarning("No Selection", "No Book Selected", "Please select a book to delete.");
            return;
        }
        if (AlertUtil.showConfirmation("Confirm Delete", "Delete Book?", "Are you sure you want to delete the book: " + selectedBook.getTitle() + "?")) {
            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return clientService.deleteBook(selectedBook.getBookId());
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                AlertUtil.showInfo("Book Deleted", task.getValue());
                handleRefreshAllBooks();
            }));
            task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Delete Book Failed", task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    private Optional<Book> showBookDialog(Book bookToEdit) {
        Dialog<Book> dialog = new Dialog<>();
        dialog.setTitle(bookToEdit == null ? "Add New Book" : "Edit Book");
        dialog.setHeaderText(bookToEdit == null ? "Enter details for the new book." : "Edit details for the book.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField bookIdField = new TextField();
        bookIdField.setPromptText("Book ID (e.g., ISBN)");
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextField authorField = new TextField();
        authorField.setPromptText("Author");
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category");
        Spinner<Integer> quantitySpinner = new Spinner<>(0, 1000, 0);
        Spinner<Integer> totalQuantitySpinner = new Spinner<>(0, 1000, 0);

        if (bookToEdit != null) {
            bookIdField.setText(bookToEdit.getBookId());
            bookIdField.setEditable(false);
            titleField.setText(bookToEdit.getTitle());
            authorField.setText(bookToEdit.getAuthor());
            categoryField.setText(bookToEdit.getCategory());
            quantitySpinner.getValueFactory().setValue(bookToEdit.getQuantity());
            totalQuantitySpinner.getValueFactory().setValue(bookToEdit.getTotalQuantity());
        }

        grid.add(new Label("Book ID:"), 0, 0); grid.add(bookIdField, 1, 0);
        grid.add(new Label("Title:"), 0, 1); grid.add(titleField, 1, 1);
        grid.add(new Label("Author:"), 0, 2); grid.add(authorField, 1, 2);
        grid.add(new Label("Category:"), 0, 3); grid.add(categoryField, 1, 3);
        grid.add(new Label("Available Qty:"), 0, 4); grid.add(quantitySpinner, 1, 4);
        grid.add(new Label("Total Qty:"), 0, 5); grid.add(totalQuantitySpinner, 1, 5);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(bookIdField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String bookId = bookIdField.getText();
                    String title = titleField.getText();
                    String author = authorField.getText();
                    String category = categoryField.getText();
                    int quantity = quantitySpinner.getValue();
                    int totalQuantity = totalQuantitySpinner.getValue();

                    if (bookId.isEmpty() || title.isEmpty() || author.isEmpty()) {
                        AlertUtil.showError("Validation Error", "Book ID, Title, and Author cannot be empty.");
                        return null;
                    }
                    if (quantity > totalQuantity) {
                        AlertUtil.showError("Validation Error", "Available quantity cannot exceed total quantity.");
                        return null;
                    }
                    return new Book(bookId, title, author, category, quantity, totalQuantity);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Input Error", "Quantity fields must be valid numbers.");
                    return null;
                }
            }
            return null;
        });
        return dialog.showAndWait();
    }

    @FXML
    private void handleRefreshAllUsers() {
        Task<List<User>> task = new Task<>() {
            @Override protected List<User> call() throws Exception {
                return clientService.getAllUsers();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> allUsersData.setAll(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Users Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleToggleUserStatusAction() {
        User selectedUser = allUsersTableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            AlertUtil.showWarning("No Selection", "No User Selected", "Please select a user to toggle their status.");
            return;
        }
        if (selectedUser.getUserId().equals(clientService.getCurrentUser().getUserId())){
            AlertUtil.showError("Operation Denied", "Cannot change status of your own account.");
            return;
        }

        boolean newStatus = !selectedUser.isActive();
        String action = newStatus ? "unfreeze (activate)" : "freeze (deactivate)";

        if (AlertUtil.showConfirmation("Confirm Status Change", "Toggle User Status?",
                "Are you sure you want to " + action + " user: " + selectedUser.getUsername() + "?")) {
            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return clientService.updateUserStatus(selectedUser.getUserId(), newStatus);
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                AlertUtil.showInfo("User Status Updated", task.getValue());
                handleRefreshAllUsers();
            }));
            task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Update User Status Failed", task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    @FXML
    private void handleRefreshAllBorrowRecords() {
        Task<List<BorrowRecord>> task = new Task<>() {
            @Override protected List<BorrowRecord> call() throws Exception {
                return clientService.getAllBorrowingRecords();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> allBorrowsData.setAll(task.getValue())));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Borrow Records Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleShowPopularBooks() {
        int limit = popularBooksLimitSpinner.getValue();
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return clientService.getPopularBooks(limit);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<String> data = task.getValue();
            if (data.isEmpty()) {
                popularBooksDisplayData.setAll("No popular books data available.");
            } else {
                popularBooksDisplayData.setAll(
                        data.stream()
                                .map(s -> {
                                    String[] parts = s.split("\\|");
                                    // Format: bookId|title|author|category|qty|totalQty|borrowCount
                                    if (parts.length >= 7) { // Ensure enough parts
                                        return String.format("'%s' by %s (Category: %s) - Borrows: %s", parts[1], parts[2], parts[3], parts[6]);
                                    }
                                    return "Invalid data: " + s;
                                })
                                .collect(Collectors.toList()) // <<< FIX 2: Changed from .toList()
                );
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Popular Books Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleShowTrendingBooks() {
        int limit = trendingBooksLimitSpinner.getValue();
        int days = trendingBooksDaysSpinner.getValue();
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return clientService.getTrendingBooks(limit, days);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<String> data = task.getValue();
            if (data.isEmpty()) {
                trendingBooksDisplayData.setAll("No trending books data available for the period.");
            } else {
                trendingBooksDisplayData.setAll(
                        data.stream()
                                .map(s -> {
                                    String[] parts = s.split("\\|");
                                    // Format: bookId|title|author|category|qty|totalQty|borrowCount
                                    if (parts.length >= 7) { // Ensure enough parts
                                        return String.format("'%s' by %s (Category: %s) - Borrows in period: %s", parts[1], parts[2], parts[3], parts[6]);
                                    }
                                    return "Invalid data: " + s;
                                })
                                .collect(Collectors.toList()) // <<< FIX 2: Changed from .toList()
                );
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> AlertUtil.showError("Load Trending Books Failed", task.getException().getMessage())));
        new Thread(task).start();
    }

    @FXML
    private void handleLogoutAdmin() {
        clientService.logout();
        mainApp.showLoginView();
    }
}