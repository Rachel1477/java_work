package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.BorrowRecord;
import org.example.model.Book; // For statistics


import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class BorrowRecordDao {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    public int addBorrowRecord(BorrowRecord record, Connection conn) throws SQLException {
        String sql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, record.getUserId());
            pstmt.setString(2, record.getBookId());
            pstmt.setString(3, record.getBorrowDate().format(DATE_FORMATTER));
            pstmt.setString(4, record.getDueDate().format(DATE_FORMATTER));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating borrow record failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating borrow record failed, no ID obtained.");
                }
            }
        }
    }

    public Optional<BorrowRecord> getBorrowRecordById(int recordId) {
        String sql = "SELECT * FROM borrow_records WHERE record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, recordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBorrowRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrow record by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<BorrowRecord> getBorrowRecordsByUserId(String userId) {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM borrow_records WHERE user_id = ? ORDER BY borrow_date DESC"; // Get recent ones first
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(mapRowToBorrowRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrow records by user ID: " + e.getMessage());
        }
        return records;
    }

    public Set<String> getBorrowedBookIdsByUserId(String userId) {
        Set<String> bookIds = new java.util.HashSet<>();
        // Only considers books ever borrowed, not just currently active loans for recommendations
        String sql = "SELECT DISTINCT book_id FROM borrow_records WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                bookIds.add(rs.getString("book_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching borrowed book IDs by user ID: " + e.getMessage());
        }
        return bookIds;
    }


    public List<BorrowRecord> getAllBorrowRecords() {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM borrow_records ORDER BY borrow_date DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                records.add(mapRowToBorrowRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all borrow records: " + e.getMessage());
        }
        return records;
    }

    public Optional<BorrowRecord> getActiveBorrowRecordByUserAndBook(String userId, String bookId) {
        String sql = "SELECT * FROM borrow_records WHERE user_id = ? AND book_id = ? AND return_date IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.setString(2, bookId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToBorrowRecord(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active borrow record: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean updateBorrowRecordReturnDate(int recordId, LocalDate returnDate, Connection conn) throws SQLException {
        String sql = "UPDATE borrow_records SET return_date = ? WHERE record_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnDate.format(DATE_FORMATTER));
            pstmt.setInt(2, recordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Method for statistics: Get most popular books
    // Returns a Map of Book object to Borrow Count
    public Map<Book, Long> getMostPopularBooks(int limit) {
        Map<Book, Long> popularBooks = new LinkedHashMap<>(); // Preserve order
        String sql = "SELECT b.book_id, b.title, b.author, b.category, b.quantity, b.total_quantity, COUNT(br.book_id) as borrow_count " +
                "FROM borrow_records br " +
                "JOIN books b ON br.book_id = b.book_id " +
                "GROUP BY br.book_id " +
                "ORDER BY borrow_count DESC, b.title ASC " +
                "LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Book book = new Book(
                        rs.getString("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getInt("total_quantity")
                );
                long borrowCount = rs.getLong("borrow_count");
                popularBooks.put(book, borrowCount);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching most popular books: " + e.getMessage());
            e.printStackTrace();
        }
        return popularBooks;
    }

    // Method for statistics: Get trending books (most borrowed in a period)
    // Returns a Map of Book object to Borrow Count in that period
    public Map<Book, Long> getTrendingBooks(int limit, int daysPeriod) {
        Map<Book, Long> trendingBooks = new LinkedHashMap<>();
        LocalDate sinceDate = LocalDate.now().minusDays(daysPeriod);
        String sql = "SELECT b.book_id, b.title, b.author, b.category, b.quantity, b.total_quantity, COUNT(br.book_id) as borrow_count " +
                "FROM borrow_records br " +
                "JOIN books b ON br.book_id = b.book_id " +
                "WHERE date(br.borrow_date) >= date(?) " + // Compare dates correctly in SQLite
                "GROUP BY br.book_id " +
                "ORDER BY borrow_count DESC, b.title ASC " +
                "LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sinceDate.format(DATE_FORMATTER));
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Book book = new Book(
                        rs.getString("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getInt("total_quantity")
                );
                long borrowCount = rs.getLong("borrow_count");
                trendingBooks.put(book, borrowCount);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching trending books: " + e.getMessage());
            e.printStackTrace();
        }
        return trendingBooks;
    }


    private BorrowRecord mapRowToBorrowRecord(ResultSet rs) throws SQLException {
        int recordId = rs.getInt("record_id");
        String userId = rs.getString("user_id");
        String bookId = rs.getString("book_id");
        LocalDate borrowDate = LocalDate.parse(rs.getString("borrow_date"), DATE_FORMATTER);
        LocalDate dueDate = LocalDate.parse(rs.getString("due_date"), DATE_FORMATTER);
        String returnDateStr = rs.getString("return_date");
        LocalDate returnDate = (returnDateStr == null || returnDateStr.isEmpty()) ? null : LocalDate.parse(returnDateStr, DATE_FORMATTER);

        return new BorrowRecord(recordId, userId, bookId, borrowDate, dueDate, returnDate);
    }
}