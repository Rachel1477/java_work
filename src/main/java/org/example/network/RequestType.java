package org.example.network;

public enum RequestType {
    // User operations
    LOGIN,              // LOGIN::username::password
    REGISTER,           // REGISTER::username::password::role (role could be optional for self-registration)
    LOGOUT,             // LOGOUT

    // Book operations (User & Admin)
    GET_ALL_BOOKS,      // GET_ALL_BOOKS
    SEARCH_BOOK_TITLE,  // SEARCH_BOOK_TITLE::title_query
    SEARCH_BOOK_AUTHOR, // SEARCH_BOOK_AUTHOR::author_query
    SEARCH_BOOK_CATEGORY,// SEARCH_BOOK_CATEGORY::category_query

    // Book operations (User)
    BORROW_BOOK,        // BORROW_BOOK::userId::bookId
    RETURN_BOOK,        // RETURN_BOOK::userId::bookId
    VIEW_MY_BORROWING_RECORDS, // VIEW_MY_BORROWING_RECORDS::userId

    // Book operations (Admin)
    ADD_BOOK,           // ADD_BOOK::bookId::title::author::category::quantity::totalQuantity
    UPDATE_BOOK,        // UPDATE_BOOK::bookId::title::author::category::quantity::totalQuantity
    DELETE_BOOK,        // DELETE_BOOK::bookId

    // Borrowing records (Admin)
    VIEW_ALL_BORROWING_RECORDS, // VIEW_ALL_BORROWING_RECORDS

    // User Management (Admin)
    // FREEZE_ACCOUNT, UNFREEZE_ACCOUNT etc. (Example: MANAGE_USER_ACCOUNT::userId::action)
    // For simplicity, might combine into a general manage user function later

    // Statistics (Admin)
    // VIEW_POPULAR_BOOKS,

    // General
    PING,               // PING
    TERMINATE_CONNECTION // TERMINATE_CONNECTION (client signals disconnect)
}