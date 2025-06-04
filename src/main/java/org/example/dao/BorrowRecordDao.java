package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.BorrowRecord;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BorrowRecordDao {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    public int addBorrowRecord(BorrowRecord record, Connection conn) throws SQLException {
        // Assumes conn is part of an existing transaction
        String sql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date) VALUES (?, ?, ?, ?)";
        // Use Statement.RETURN_GENERATED_KEYS to get the auto-incremented ID
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
                    return generatedKeys.getInt(1); // Return the new record_id
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
        String sql = "SELECT * FROM borrow_records WHERE user_id = ?";
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

    public List<BorrowRecord> getAllBorrowRecords() {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM borrow_records";
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

    // To find if a user has an active (non-returned) loan for a specific book
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
        // Assumes conn is part of an existing transaction
        String sql = "UPDATE borrow_records SET return_date = ? WHERE record_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnDate.format(DATE_FORMATTER));
            pstmt.setInt(2, recordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
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