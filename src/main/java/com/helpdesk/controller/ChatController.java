package com.helpdesk.controller;

import com.helpdesk.model.ChatMessage;
import com.helpdesk.service.GroqService;
import com.helpdesk.service.DatabaseService;
import com.helpdesk.service.ResponseGenerator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class ChatController {

    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatContainer;

    @FXML
    private TextField messageInput;

    private GroqService groqService;
    private DatabaseService databaseService;
    private ResponseGenerator responseGenerator;
    private Preferences preferences;

    @FXML
    public void initialize() {
        // Initialize services
        groqService = new GroqService();
        databaseService = new DatabaseService();
        responseGenerator = new ResponseGenerator(groqService, databaseService);
        preferences = Preferences.userNodeForPackage(SettingsController.class);

        // Apply current theme when application starts
        Platform.runLater(() -> {
            applyCurrentTheme();
        });

        // Add welcome message
        Platform.runLater(() -> {
            addBotMessage("👋 Hi there! I'm your IT Support assistant. How can I help you today?");

            // Add suggestion buttons
            addSuggestionButtons();
        });
    }

    private void applyCurrentTheme() {
        Scene scene = chatScrollPane.getScene();
        if (scene != null) {
            String theme = preferences.get("theme", "light");
            String cssPath = theme.equals("dark")
                    ? "/css/mobile-dark.css"
                    : "/css/mobile-light.css";

            scene.getStylesheets().clear();
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        }
    }

    private void addSuggestionButtons() {
        HBox suggestionsBox = new HBox(5);
        suggestionsBox.setAlignment(Pos.CENTER_LEFT);

        String[] suggestions = {
                "WiFi not working",
                "Reset password",
                "Printer issues",
                "Software installation"
        };

        for (String suggestion : suggestions) {
            Button btn = new Button(suggestion);
            btn.getStyleClass().add("suggestion-button");
            btn.setOnAction(e -> {
                messageInput.setText(suggestion);
                sendMessage();
            });
            suggestionsBox.getChildren().add(btn);
        }

        chatContainer.getChildren().add(suggestionsBox);
    }

    @FXML
    public void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) return;

        // Add user message to chat
        addUserMessage(message);

        // Clear input
        messageInput.clear();

        // Show typing indicator
        Label typingIndicator = new Label("Typing...");
        typingIndicator.getStyleClass().add("typing-indicator");
        chatContainer.getChildren().add(typingIndicator);
        scrollToBottom();

        // Get response asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                return responseGenerator.generateResponse(message);
            } catch (Exception e) {
                e.printStackTrace();
                return "Sorry, I encountered an error processing your request. Please try again.";
            }
        }).thenAccept(response -> {
            Platform.runLater(() -> {
                // Remove typing indicator
                chatContainer.getChildren().remove(typingIndicator);

                // Add bot response
                addBotMessage(response);

                // Save conversation to database
                databaseService.saveConversation(message, response, LocalDateTime.now());
            });
        });
    }

    private void addUserMessage(String message) {
        Label userMessageLabel = new Label(message);
        userMessageLabel.getStyleClass().add("user-message");

        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.getChildren().add(userMessageLabel);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        VBox botMessageBox = new VBox(5);

        Label botMessageLabel = new Label(message);
        botMessageLabel.getStyleClass().add("bot-message");
        botMessageBox.getChildren().add(botMessageLabel);

        // Add feedback buttons
        HBox feedbackButtons = new HBox(5);
        feedbackButtons.getStyleClass().add("feedback-buttons");

        Button helpfulButton = new Button("👍 Helpful");
        helpfulButton.getStyleClass().add("feedback-button");
        helpfulButton.setOnAction(e -> provideFeedback(message, true));

        Button notHelpfulButton = new Button("👎 Not helpful");
        notHelpfulButton.getStyleClass().add("feedback-button");
        notHelpfulButton.setOnAction(e -> provideFeedback(message, false));

        feedbackButtons.getChildren().addAll(helpfulButton, notHelpfulButton);
        botMessageBox.getChildren().add(feedbackButtons);

        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.getChildren().add(botMessageBox);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void provideFeedback(String message, boolean helpful) {
        // Save feedback to database
        databaseService.saveFeedback(message, helpful);

        if (!helpful) {
            addBotMessage("I'm sorry this wasn't helpful. Let me try to connect you with more specific solutions.");
            // Here you could implement the escalation logic
        }
    }

    private void scrollToBottom() {
        chatScrollPane.setVvalue(1.0);
    }

    @FXML
    public void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings_view.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setTitle("Settings");

            Scene scene = new Scene(root, 350, 450);

            // Apply current theme to settings window when opened
            String theme = preferences.get("theme", "light");
            String cssPath = theme.equals("dark")
                    ? "/css/mobile-dark.css"
                    : "/css/mobile-light.css";
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());

            settingsStage.setScene(scene);
            settingsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}