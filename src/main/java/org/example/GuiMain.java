package org.example;

import org.example.client.MainApp;
import javafx.application.Application;

public class GuiMain {
    public static void main(String[] args) {
        // You can still initialize the database here if needed,
        // or ensure MainServer does it before starting the GUI.
        // com.university.library.db.DatabaseManager.initializeDatabase();
        Application.launch(MainApp.class, args);
    }
}
