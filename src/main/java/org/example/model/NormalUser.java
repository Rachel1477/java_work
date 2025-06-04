package org.example.model;


public class NormalUser extends User {
    public NormalUser(String userId, String username, String passwordHash) {
        super(userId, username, passwordHash, Role.NORMAL_USER, true); // Default to active
    }
    public NormalUser(String userId, String username, String passwordHash, boolean isActive) {
        super(userId, username, passwordHash, Role.NORMAL_USER, isActive);
    }
}
