package org.example.model;


import java.util.Objects;

public class Book {
    private String bookId; // ISBN or a unique generated ID
    private String title;
    private String author;
    private String category;
    private int quantity; // Available quantity
    private int totalQuantity; // Total quantity owned

    public Book(String bookId, String title, String author, String category, int quantity, int totalQuantity) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.category = category;
        this.quantity = quantity;
        this.totalQuantity = totalQuantity;
    }

    // Getters
    public String getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getTotalQuantity() { return totalQuantity; }


    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("Total quantity cannot be negative.");
        }
        this.totalQuantity = totalQuantity;
        // Ensure available quantity is not more than total
        if (this.quantity > totalQuantity) {
            this.quantity = totalQuantity;
        }
    }


    public void decreaseQuantity(int amount) {
        if (this.quantity - amount < 0) {
            throw new IllegalStateException("Cannot decrease quantity below zero.");
        }
        this.quantity -= amount;
    }

    public void increaseQuantity(int amount) {
        if (this.quantity + amount > this.totalQuantity) {
            throw new IllegalStateException("Cannot increase available quantity beyond total quantity.");
        }
        this.quantity += amount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(bookId, book.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId);
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookId='" + bookId + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", category='" + category + '\'' +
                ", quantity=" + quantity +
                ", totalQuantity=" + totalQuantity +
                '}';
    }
}