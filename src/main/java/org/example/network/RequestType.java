package org.example.network;

public enum RequestType {
    // User operations
    LOGIN,              // LOGIN::username::password
    REGISTER,           // REGISTER::userId::username::password::role (role can be NORMAL_USER for self-reg)
    LOGOUT,             // LOGOUT (currently placeholder)

    // Book operations (User & Admin)
    GET_ALL_BOOKS,      // GET_ALL_BOOKS
    SEARCH_BOOK,        // SEARCH_BOOK::searchField(title/author/category)::searchTerm
    GET_BOOK_BY_ID,     // GET_BOOK_BY_ID::bookId

    // Book operations (User)
    BORROW_BOOK,        // BORROW_BOOK::userId::bookId
    RETURN_BOOK,        // RETURN_BOOK::userId::bookId (or use recordId if GUI allows selecting a specific loan)
    // For simplicity, RETURN_BOOK::userId::bookId assumes only one active loan per book by a user.
    VIEW_MY_BORROWING_RECORDS, // VIEW_MY_BORROWING_RECORDS::userId
    GET_MY_OVERDUE_BOOKS, // GET_MY_OVERDUE_BOOKS::userId (extra, for overdue reminders)


    // Book operations (Admin)
    ADD_BOOK,           // ADD_BOOK::bookId::title::author::category::quantity::totalQuantity
    UPDATE_BOOK,        // UPDATE_BOOK::bookId::title::author::category::quantity::totalQuantity (entire object)
    DELETE_BOOK,        // DELETE_BOOK::bookId

    // Borrowing records (Admin)
    VIEW_ALL_BORROWING_RECORDS, // VIEW_ALL_BORROWING_RECORDS

    // User Management (Admin)
    GET_ALL_USERS,      // GET_ALL_USERS
    UPDATE_USER_STATUS, // UPDATE_USER_STATUS::userId::isActive(true/false)
    // DELETE_USER,     // DELETE_USER::userId (use with caution)

    // General
    PING,               // PING
    TERMINATE_CONNECTION // TERMINATE_CONNECTION (client signals disconnect)
}