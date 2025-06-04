package org.example.model;


public class NormalUser extends User {
    public NormalUser(String userId, String username, String password) {
        super(userId, username, password, Role.NORMAL_USER);
    }
    // NormalUser specific methods can be added later if needed
}
