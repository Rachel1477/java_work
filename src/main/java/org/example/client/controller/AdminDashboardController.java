package org.example.client.controller;

import org.example.client.MainApp;
import org.example.client.ClientService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
// Add other imports for TableView, etc., as you build it out

public class AdminDashboardController {

    @FXML private Label welcomeLabelAdmin;
    // Add other @FXML annotations for UI elements here

    private MainApp mainApp;
    private ClientService clientService;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.clientService = ClientService.getInstance();
        if (clientService.getCurrentUser() != null) {
            welcomeLabelAdmin.setText("Admin Dashboard - User: " + clientService.getCurrentUser().getUsername());
        }
        // Load initial admin data or setup views
    }

    @FXML
    private void initialize() {
        // Initialization logic for admin dashboard elements
        System.out.println("Admin Dashboard Initialized.");
    }

    @FXML
    private void handleLogoutAdmin() {
        clientService.logout();
        mainApp.showLoginView();
    }

    // Add handler methods for admin functionalities:
    // e.g., handleAddBook, handleManageUsers, handleViewStatistics, etc.
}
