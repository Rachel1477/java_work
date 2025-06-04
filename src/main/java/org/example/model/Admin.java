package org.example.model;

public class Admin extends User {
    public Admin(String userId, String username, String password) {
        super(userId, username, password, Role.ADMIN);
    }
    // Admin specific methods can be added later if needed
}
