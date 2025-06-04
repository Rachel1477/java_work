package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.BorrowRecord;
import org.example.model.Book; // For statistics


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
// Import java.sql.Date for direct mapping to MySQL DATE type
import java.sql.Date; // Alias to avoid conflict with java.util.Date if used
import java.time.LocalDate;
// DateTimeFormatter might still be useful for parsing if dates come as strings, but not for setting SQL Date
// import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BorrowRecordDao {
    // DateTimeFormatter may not be needed if we consistently use java.sql.Date with PreparedStatement
    // private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public int addBorrowRecord(BorrowRecord record, Connection conn) throws SQLException {
        String sql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, record.getUserId());
            pstmt.setString(2, record.getBookId());
            // Convert LocalDate to java.sql.Date for MySQL DATE type
            pstmt.setDate(3, Date.valueOf(record.getBorrowDate()));
            pstmt.setDate(4, Date.valueOf(record.getDueDate()));

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
        String sql = "SELECT * FROM borrow_records WHERE user_id = ? ORDER BY borrow_date DESC";
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
            if (returnDate != null) {
                pstmt.setDate(1, Date.valueOf(returnDate));
            } else {
                pstmt.setNull(1, java.sql.Types.DATE);
            }
            pstmt.setInt(2, recordId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    public Map<Book, Long> getMostPopularBooks(int limit) {
        Map<Book, Long> popularBooks = new LinkedHashMap<>();
        // MySQL DATE() function is not needed if borrow_date is already a DATE type
        String sql = "SELECT b.book_id, b.title, b.author, b.category, b.quantity, b.total_quantity, COUNT(br.book_id) as borrow_count " +
                "FROM borrow_records br " +
                "JOIN books b ON br.book_id = b.book_id " +
                "GROUP BY b.book_id, b.title, b.author, b.category, b.quantity, b.total_quantity " + // MySQL is stricter with GROUP BY
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

    public Map<Book, Long> getTrendingBooks(int limit, int daysPeriod) {
        Map<Book, Long> trendingBooks = new LinkedHashMap<>();
        LocalDate sinceDate = LocalDate.now().minusDays(daysPeriod);
        // For MySQL, directly compare with DATE type column
        String sql = "SELECT b.book_id, b.title, b.author, b.category, b.quantity, b.total_quantity, COUNT(br.book_id) as borrow_count " +
                "FROM borrow_records br " +
                "JOIN books b ON br.book_id = b.book_id " +
                "WHERE br.borrow_date >= ? " + // Use java.sql.Date for comparison
                "GROUP BY b.book_id, b.title, b.author, b.category, b.quantity, b.total_quantity " + // MySQL GROUP BY
                "ORDER BY borrow_count DESC, b.title ASC " +
                "LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(sinceDate)); // Convert LocalDate to java.sql.Date
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
        // Get java.sql.Date and convert to LocalDate
        Date borrowSqlDate = rs.getDate("borrow_date");
        LocalDate borrowDate = (borrowSqlDate != null) ? borrowSqlDate.toLocalDate() : null;

        Date dueSqlDate = rs.getDate("due_date");
        LocalDate dueDate = (dueSqlDate != null) ? dueSqlDate.toLocalDate() : null;

        Date returnSqlDate = rs.getDate("return_date");
        LocalDate returnDate = (returnSqlDate != null) ? returnSqlDate.toLocalDate() : null;

        // Ensure borrowDate and dueDate are not null if the DB constraint is NOT NULL
        if (borrowDate == null || dueDate == null) {
            // This shouldn't happen if DB schema enforces NOT NULL for these
            throw new SQLException("Borrow date or due date is null from database for record_id: " + recordId);
        }

        return new BorrowRecord(recordId, userId, bookId, borrowDate, dueDate, returnDate);
    }
}