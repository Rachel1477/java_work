package org.example.client.controller;

import org.example.client.MainApp;
import org.example.client.ClientService;
import org.example.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegistrationController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;

    private MainApp mainApp; // Not strictly needed if only closing stage, but good for consistency
    private ClientService clientService;
    private Stage dialogStage;


    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.clientService = ClientService.getInstance();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }


    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }
        if (password.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters long.");
            return;
        }

        statusLabel.setText("Registering...");
        registerButton.setDisable(true);

        Task<String> registerTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return clientService.register(username, password);
            }
        };

        registerTask.setOnSucceeded(event -> {
            String successMessage = registerTask.getValue();
            AlertUtil.showInfo("Registration Successful", "Account Created", successMessage + "\nYou can now login.");
            if (dialogStage != null) dialogStage.close();
        });

        registerTask.setOnFailed(event -> {
            Throwable ex = registerTask.getException();
            statusLabel.setText("Registration failed: " + ex.getMessage());
            AlertUtil.showError("Registration Failed", ex.getMessage());
            registerButton.setDisable(false);
        });

        new Thread(registerTask).start();
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}
