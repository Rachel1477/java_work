package org.example.model;


import java.time.LocalDate;
import java.util.Objects;

public class BorrowRecord {
    private int recordId; // Auto-incremented by DB
    private String userId;
    private String bookId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate; // Null if not yet returned

    // Constructor for new records (recordId will be set by DB)
    public BorrowRecord(String userId, String bookId, LocalDate borrowDate, LocalDate dueDate) {
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = null;
    }

    // Constructor for records retrieved from DB (includes recordId and potentially returnDate)
    public BorrowRecord(int recordId, String userId, String bookId, LocalDate borrowDate, LocalDate dueDate, LocalDate returnDate) {
        this.recordId = recordId;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    // Getters
    public int getRecordId() {
        return recordId;
    }

    public String getUserId() {
        return userId;
    }

    public String getBookId() {
        return bookId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    // Setter for recordId (used after insertion into DB if needed by DAO)
    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    // Setter for returnDate
    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public boolean isOverdue() {
        return returnDate == null && LocalDate.now().isAfter(dueDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorrowRecord that = (BorrowRecord) o;
        return recordId == that.recordId; // If recordId is 0 (not yet persisted), this might be problematic
        // A better equals might consider all fields if recordId is not set.
        // For simplicity, using recordId here.
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }

    @Override
    public String toString() {
        return "BorrowRecord{" +
                "recordId=" + recordId +
                ", userId='" + userId + '\'' +
                ", bookId='" + bookId + '\'' +
                ", borrowDate=" + borrowDate +
                ", dueDate=" + dueDate +
                ", returnDate=" + returnDate +
                '}';
    }
}