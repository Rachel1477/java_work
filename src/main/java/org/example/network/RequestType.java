package org.example.network;

public enum RequestType {
    // User operations
    LOGIN,              // LOGIN::username::password
    REGISTER,           // REGISTER::username::password (userId and NORMAL_USER role are auto-assigned)
    LOGOUT,             // LOGOUT

    // Book operations (User & Admin)
    GET_ALL_BOOKS,      // GET_ALL_BOOKS
    SEARCH_BOOK,        // SEARCH_BOOK::searchField(title/author/category)::searchTerm
    GET_BOOK_BY_ID,     // GET_BOOK_BY_ID::bookId

    // Book operations (User)
    BORROW_BOOK,        // BORROW_BOOK::bookId (userId from loggedInUser)
    RETURN_BOOK,        // RETURN_BOOK::bookId (userId from loggedInUser)
    VIEW_MY_BORROWING_RECORDS, // VIEW_MY_BORROWING_RECORDS (userId from loggedInUser)
    GET_MY_OVERDUE_BOOKS, // GET_MY_OVERDUE_BOOKS (userId from loggedInUser)
    GET_MY_RECOMMENDATIONS, // GET_MY_RECOMMENDATIONS::limit (userId from loggedInUser)

    // Book operations (Admin)
    ADD_BOOK,           // ADD_BOOK::bookId::title::author::category::quantity::totalQuantity
    UPDATE_BOOK,        // UPDATE_BOOK::bookId::title::author::category::quantity::totalQuantity
    DELETE_BOOK,        // DELETE_BOOK::bookId

    // Borrowing records & Statistics (Admin)
    VIEW_ALL_BORROWING_RECORDS, // VIEW_ALL_BORROWING_RECORDS
    GET_POPULAR_BOOKS,          // GET_POPULAR_BOOKS::limit
    GET_TRENDING_BOOKS,         // GET_TRENDING_BOOKS::limit::daysPeriod

    // User Management (Admin)
    GET_ALL_USERS,      // GET_ALL_USERS
    UPDATE_USER_STATUS, // UPDATE_USER_STATUS::userId::isActive(true/false)

    // General
    PING,               // PING
    TERMINATE_CONNECTION // TERMINATE_CONNECTION
}