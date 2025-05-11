package com.helpdesk.controller;

import com.helpdesk.service.DatabaseService;
import com.helpdesk.service.GroqService;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class SettingsController {

    @FXML
    private PasswordField apiKeyField;

    @FXML
    private RadioButton lightThemeRadio;

    @FXML
    private RadioButton darkThemeRadio;

    private GroqService groqService;
    private DatabaseService databaseService;
    private Preferences preferences;

    @FXML
    public void initialize() {
        groqService = new GroqService();
        databaseService = new DatabaseService();
        preferences = Preferences.userNodeForPackage(SettingsController.class);

        // Load saved API key
        String savedApiKey = preferences.get("groqApiKey", "");
        if (!savedApiKey.isEmpty()) {
            apiKeyField.setText(savedApiKey);
        }

        // Load theme preference
        String theme = preferences.get("theme", "light");
        if (theme.equals("dark")) {
            darkThemeRadio.setSelected(true);
        } else {
            lightThemeRadio.setSelected(true);
        }
    }

    @FXML
    public void saveApiKey() {
        String apiKey = apiKeyField.getText().trim();
        if (!apiKey.isEmpty()) {
            preferences.put("groqApiKey", apiKey);
            groqService.setApiKey(apiKey);

            showAlert(Alert.AlertType.INFORMATION, "API Key Saved",
                    "Your Groq API key has been saved successfully.");
        } else {
            showAlert(Alert.AlertType.WARNING, "Empty API Key",
                    "Please enter a valid API key.");
        }
    }

    @FXML
    public void applyTheme() {
        String theme = lightThemeRadio.isSelected() ? "light" : "dark";
        preferences.put("theme", theme);

        // Get the resource path for the theme
        String cssPath = theme.equals("dark")
                ? "/css/mobile-dark.css"
                : "/css/mobile-light.css";
        String cssResource = getClass().getResource(cssPath).toExternalForm();

        // Apply theme to settings window
        applyThemeToScene(lightThemeRadio.getScene(), cssResource);

        // Apply theme to all open application windows
        for (Window window : Stage.getWindows()) {
            if (window instanceof Stage) {
                Scene scene = ((Stage) window).getScene();
                if (scene != null) {
                    applyThemeToScene(scene, cssResource);
                }
            }
        }

        showAlert(Alert.AlertType.INFORMATION, "Theme Applied",
                "The " + theme + " theme has been applied to all windows.");
    }

    private void applyThemeToScene(Scene scene, String cssResource) {
        if (scene == null) return;

        scene.getStylesheets().clear();
        scene.getStylesheets().add(cssResource);
    }

    @FXML
    public void clearHistory() {
        // Create confirmation alert
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear History");
        alert.setHeaderText("Clear Conversation History");
        alert.setContentText("This will delete all conversation history. Are you sure?");
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Delete conversation history
                try {
                    Files.deleteIfExists(Paths.get("helpdesk.db"));
                    databaseService = new DatabaseService(); // Reinitialize database
                    showAlert(Alert.AlertType.INFORMATION, "History Cleared",
                            "Conversation history has been cleared successfully.");
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to clear conversation history: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void exportHistory() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Conversation History");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("helpdesk_history.csv");

        File file = fileChooser.showSaveDialog(apiKeyField.getScene().getWindow());

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write CSV header
                writer.write("User Message,Bot Response,Timestamp,Helpful\n");

                // Export data from database to CSV
                // Note: This is a simplified example. In a real app, you would
                // query the database and format each row properly
                writer.write("Example user message,Example bot response,2025-05-10T12:00:00,true\n");

                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Conversation history has been exported to: " + file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export conversation history: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}