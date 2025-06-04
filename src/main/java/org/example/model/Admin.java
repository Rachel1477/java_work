package org.example.model;

public class Admin extends User {
    public Admin(String userId, String username, String passwordHash) {
        super(userId, username, passwordHash, Role.ADMIN, true); // Default to active
    }
    public Admin(String userId, String username, String passwordHash, boolean isActive) {
        super(userId, username, passwordHash, Role.ADMIN, isActive);
    }
}
