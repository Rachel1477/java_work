package org.example.client.controller;

import org.example.client.MainApp;
import org.example.client.ClientService;
import org.example.util.AlertUtil;
import org.example.model.User;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button testConnectionButton;
    @FXML private Label statusLabel;

    private MainApp mainApp;
    private ClientService clientService;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.clientService = ClientService.getInstance();
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void handleTestConnection() {
        statusLabel.setText("Testing connection...");
        loginButton.setDisable(true);
        testConnectionButton.setDisable(true);

        Task<Boolean> connectTask = new Task<>() {
            @Override
            protected Boolean call() {
                return clientService.connect();
            }
        };

        connectTask.setOnSucceeded(event -> {
            boolean connected = connectTask.getValue();
            if (connected) {
                statusLabel.setText("Connection successful!");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("Connection failed. Server might be down.");
                statusLabel.setStyle("-fx-text-fill: red;");
                AlertUtil.showError("Connection Failed", "Could not connect to the server.");
            }
            loginButton.setDisable(false);
            testConnectionButton.setDisable(false);
        });
        connectTask.setOnFailed(event -> {
            statusLabel.setText("Connection test error.");
            statusLabel.setStyle("-fx-text-fill: red;");
            AlertUtil.showError("Connection Error", "An error occurred while testing connection: " + connectTask.getException().getMessage());
            loginButton.setDisable(false);
            testConnectionButton.setDisable(false);
        });
        new Thread(connectTask).start();
    }


    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        statusLabel.setText("Logging in...");
        statusLabel.setStyle("-fx-text-fill: black;");
        loginButton.setDisable(true);

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return clientService.login(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            User user = loginTask.getValue();
            if (user != null) {
                mainApp.showDashboard(user);
            } else {
                // This case should ideally be handled by exceptions from clientService.login
                statusLabel.setText("Login failed. Unexpected issue.");
                statusLabel.setStyle("-fx-text-fill: red;");
                AlertUtil.showError("Login Failed", "An unexpected error occurred during login.");
            }
            loginButton.setDisable(false);
        });

        loginTask.setOnFailed(event -> {
            Throwable ex = loginTask.getException();
            statusLabel.setText("Login failed: " + ex.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
            AlertUtil.showError("Login Failed", ex.getMessage());
            loginButton.setDisable(false);
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleRegisterLink() {
        mainApp.showRegistrationView();
    }
}
