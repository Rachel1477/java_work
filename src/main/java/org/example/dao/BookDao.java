package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BookDao {

    public boolean addBook(Book book) {
        if (getBookById(book.getBookId()).isPresent()) {
            System.err.println("Book with ID " + book.getBookId() + " already exists.");
            return false;
        }
        String sql = "INSERT INTO books (book_id, title, author, category, quantity, total_quantity) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getBookId());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getCategory());
            pstmt.setInt(5, book.getQuantity());
            pstmt.setInt(6, book.getTotalQuantity());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
            return false;
        }
    }

    public Optional<Book> getBookById(String bookId) {
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching book by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all books: " + e.getMessage());
        }
        return books;
    }

    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, category = ?, quantity = ?, total_quantity = ? WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setString(3, book.getCategory());
            pstmt.setInt(4, book.getQuantity());
            pstmt.setInt(5, book.getTotalQuantity());
            pstmt.setString(6, book.getBookId());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating book: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBookQuantity(String bookId, int newQuantity, Connection conn) throws SQLException {
        String sql = "UPDATE books SET quantity = ? WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setString(2, bookId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean deleteBook(String bookId) {
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bookId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting book: " + e.getMessage());
            return false;
        }
    }

    public List<Book> searchBooks(String searchTerm, String searchField) {
        List<Book> books = new ArrayList<>();
        if (!searchField.matches("title|author|category")) {
            System.err.println("Invalid search field: " + searchField);
            return books;
        }
        String sql = "SELECT * FROM books WHERE " + searchField + " LIKE ? AND quantity > 0"; // Only show available books in search
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                books.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error searching books: " + e.getMessage());
        }
        return books;
    }

    public List<Book> getBooksByCategoriesExcludingBorrowed(Set<String> categories, Set<String> borrowedBookIds, int limit) {
        List<Book> recommendedBooks = new ArrayList<>();
        if (categories.isEmpty()) {
            return recommendedBooks;
        }

        // Constructing the IN clause for categories
        String categoryPlaceholders = categories.stream().map(c -> "?").collect(Collectors.joining(", "));

        // Constructing the NOT IN clause for borrowedBookIds, if any
        String borrowedExclusionSql = "";
        if (!borrowedBookIds.isEmpty()) {
            String borrowedPlaceholders = borrowedBookIds.stream().map(id -> "?").collect(Collectors.joining(", "));
            borrowedExclusionSql = " AND b.book_id NOT IN (" + borrowedPlaceholders + ")";
        }

        // Fetch books from specified categories, not borrowed, with available quantity, ordered by some popularity metric (e.g., total_quantity or later by borrow count)
        // For simplicity now, order by title, limit
        // A more advanced recommendation would join with borrow_records to get borrow counts for popularity.
        String sql = "SELECT b.* FROM books b " +
                "WHERE b.category IN (" + categoryPlaceholders + ")" +
                borrowedExclusionSql +
                " AND b.quantity > 0 " +
                "ORDER BY b.total_quantity DESC, b.title " + // Prioritize books with more total copies, then by title
                "LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            for (String category : categories) {
                pstmt.setString(paramIndex++, category);
            }
            if (!borrowedBookIds.isEmpty()) {
                for (String bookId : borrowedBookIds) {
                    pstmt.setString(paramIndex++, bookId);
                }
            }
            pstmt.setInt(paramIndex, limit);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                recommendedBooks.add(mapRowToBook(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recommended books: " + e.getMessage());
            e.printStackTrace();
        }
        return recommendedBooks;
    }


    private Book mapRowToBook(ResultSet rs) throws SQLException {
        return new Book(
                rs.getString("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("category"),
                rs.getInt("quantity"),
                rs.getInt("total_quantity")
        );
    }
}