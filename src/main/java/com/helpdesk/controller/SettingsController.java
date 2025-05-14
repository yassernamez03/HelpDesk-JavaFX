package com.helpdesk.controller;

import com.helpdesk.service.DatabaseService;
import com.helpdesk.service.GroqService;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ToggleButton;
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
    private ToggleButton themeToggle;

    private GroqService groqService;
    private DatabaseService databaseService;
    private Preferences preferences;

    @FXML
    public void initialize() {
        groqService = new GroqService();
        databaseService = new DatabaseService();
        preferences = Preferences.userNodeForPackage(SettingsController.class);

        String savedApiKey = preferences.get("groqApiKey", "");

        // Set toggle state based on saved preference
        String theme = preferences.get("theme", "light");
        boolean isDark = theme.equals("dark");
        themeToggle.setSelected(isDark);
        themeToggle.setText(isDark ? "Dark Mode" : "Light Mode");
    }

    @FXML
    public void toggleTheme() {
        boolean isDark = themeToggle.isSelected();
        String theme = isDark ? "dark" : "light";
        preferences.put("theme", theme);

        themeToggle.setText(isDark ? "Dark Mode" : "Light Mode");

        String cssPath = isDark
                ? "/css/mobile-dark.css"
                : "/css/mobile-light.css";
        String cssResource = getClass().getResource(cssPath).toExternalForm();

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


    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}