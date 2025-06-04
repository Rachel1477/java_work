package org.example.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;



public class AlertUtil {

    public static void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Implementation for showError(String title, String content)
    // This was the one likely causing the "cannot resolve method showError(String, String)" error
    // if header was null and it tried to call showError(String, String, String)
    public static void showError(String title, String content) {
        showError(title, null, content); // Calls the three-argument version with a null header
    }

    public static void showError(String content) {
        showError("Error", null, content); // Calls the three-argument version
    }

    public static void showInfo(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Implementation for showInfo(String title, String content)
    public static void showInfo(String title, String content) {
        showInfo(title, null, content); // Calls the three-argument version with a null header
    }

    public static void showInfo(String content) {
        showInfo("Information", null, content); // Calls the three-argument version
    }

    public static void showWarning(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}