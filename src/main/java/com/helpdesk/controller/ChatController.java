package com.helpdesk.controller;

import com.helpdesk.model.ChatMessage;
import com.helpdesk.model.KnowledgeBase;
import com.helpdesk.service.GroqService;
import com.helpdesk.service.DatabaseService;
import com.helpdesk.service.ResponseGenerator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ChatController {    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatContainer;

    @FXML
    private TextField messageInput;

    @FXML
    private BorderPane chatView;

    @FXML
    private BorderPane knowledgeBaseView;

    @FXML
    private BorderPane historyView;

    @FXML
    private VBox kbContainer;

    @FXML
    private VBox historyContainer;

    @FXML
    private TextField searchField;

    private GroqService groqService;
    private DatabaseService databaseService;
    private ResponseGenerator responseGenerator;
    private Preferences preferences;
    private DateTimeFormatter timeFormatter;

    @FXML
    public void initialize() {
        // Initialize services
        groqService = new GroqService();
        databaseService = new DatabaseService();
        responseGenerator = new ResponseGenerator(groqService, databaseService);
        preferences = Preferences.userNodeForPackage(SettingsController.class);
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // Apply current theme when application starts
        Platform.runLater(() -> {
            applyCurrentTheme();
        });

        // Add welcome message
        Platform.runLater(() -> {
            addBotMessage("👋 Hi there! I'm your IT Support assistant. How can I help you today?", LocalDateTime.now());

            // Add suggestion buttons
            addSuggestionButtons();
        });

        // Setup search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            loadKnowledgeBase(newValue);
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

        // Get current timestamp
        LocalDateTime timestamp = LocalDateTime.now();

        // Add user message to chat
        addUserMessage(message, timestamp);

        // Clear input
        messageInput.clear();

        // Show typing indicator with animation
        VBox typingBox = createTypingIndicator();
        chatContainer.getChildren().add(typingBox);
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
                chatContainer.getChildren().remove(typingBox);

                // Add bot response with streaming effect
                addBotMessageWithTypingEffect(response, LocalDateTime.now());

                // Save conversation to database
                databaseService.saveConversation(message, response, timestamp);
            });
        });
    }

    private VBox createTypingIndicator() {
        VBox typingBox = new VBox();
        typingBox.setAlignment(Pos.CENTER_LEFT);

        HBox dotsBox = new HBox(3);
        dotsBox.getStyleClass().add("typing-dots-container");

        for (int i = 0; i < 3; i++) {
            Label dot = new Label("•");
            dot.getStyleClass().add("typing-dot");
            dotsBox.getChildren().add(dot);
        }

        Label typingLabel = new Label();
        typingLabel.getStyleClass().add("typing-indicator");
        typingLabel.setGraphic(dotsBox);

        VBox messageBox = new VBox();
        messageBox.getStyleClass().add("bot-message");
        messageBox.getStyleClass().add("typing-animation");
        messageBox.getChildren().add(typingLabel);

        typingBox.getChildren().add(messageBox);

        // Create animation for typing dots
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.4), e -> {
                    ((Label)dotsBox.getChildren().get(0)).setStyle("-fx-opacity: 1;");
                    ((Label)dotsBox.getChildren().get(1)).setStyle("-fx-opacity: 0.3;");
                    ((Label)dotsBox.getChildren().get(2)).setStyle("-fx-opacity: 0.3;");
                }),
                new KeyFrame(Duration.seconds(0.8), e -> {
                    ((Label)dotsBox.getChildren().get(0)).setStyle("-fx-opacity: 0.3;");
                    ((Label)dotsBox.getChildren().get(1)).setStyle("-fx-opacity: 1;");
                    ((Label)dotsBox.getChildren().get(2)).setStyle("-fx-opacity: 0.3;");
                }),
                new KeyFrame(Duration.seconds(1.2), e -> {
                    ((Label)dotsBox.getChildren().get(0)).setStyle("-fx-opacity: 0.3;");
                    ((Label)dotsBox.getChildren().get(1)).setStyle("-fx-opacity: 0.3;");
                    ((Label)dotsBox.getChildren().get(2)).setStyle("-fx-opacity: 1;");
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        return typingBox;
    }

    private void addUserMessage(String message, LocalDateTime timestamp) {
        VBox userMessageBox = new VBox(3);

        // Create the timestamp label
        Label timestampLabel = new Label(timestamp.format(timeFormatter));
        timestampLabel.getStyleClass().add("message-timestamp");
        timestampLabel.setAlignment(Pos.CENTER_RIGHT);

        // Create the message label
        Label userMessageLabel = new Label(message);
        userMessageLabel.getStyleClass().add("user-message");
        userMessageLabel.setWrapText(true); // Ensure wrapping if needed

        // Add this to limit width
        userMessageLabel.setMaxWidth(chatScrollPane.getWidth() * 0.75);
        userMessageBox.getChildren().addAll(timestampLabel, userMessageLabel);
        userMessageBox.setAlignment(Pos.CENTER_RIGHT);

        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_RIGHT);
        messageContainer.getChildren().add(userMessageBox);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void addBotMessageWithTypingEffect(String fullMessage, LocalDateTime timestamp) {
        VBox botMessageBox = new VBox(5);

        // Create the timestamp label
        Label timestampLabel = new Label(timestamp.format(timeFormatter));
        timestampLabel.getStyleClass().add("message-timestamp");
        timestampLabel.setAlignment(Pos.CENTER_LEFT);

        // Create the message container (initially empty)
        VBox messageContentBox = new VBox();
        messageContentBox.getStyleClass().add("bot-message");

        messageContentBox.setMaxWidth(chatScrollPane.getWidth() * 0.75);

        // We'll add content to this incrementally
        StringBuilder currentText = new StringBuilder();

        // Add feedback buttons
        HBox feedbackButtons = new HBox(5);
        feedbackButtons.getStyleClass().add("feedback-buttons");
        feedbackButtons.setVisible(false); // Hide until animation completes

        Button helpfulButton = new Button("👍 Helpful");
        helpfulButton.getStyleClass().add("feedback-button");
        helpfulButton.setOnAction(e -> provideFeedback(fullMessage, true));

        Button notHelpfulButton = new Button("👎 Not helpful");
        notHelpfulButton.getStyleClass().add("feedback-button");
        notHelpfulButton.setOnAction(e -> provideFeedback(fullMessage, false));

        feedbackButtons.getChildren().addAll(helpfulButton, notHelpfulButton);

        botMessageBox.getChildren().addAll(timestampLabel, messageContentBox, feedbackButtons);

        HBox messageContainer = new HBox();
        messageContainer.setAlignment(Pos.CENTER_LEFT);
        messageContainer.getChildren().add(botMessageBox);

        chatContainer.getChildren().add(messageContainer);
        scrollToBottom();

        // Create typing effect
        final int[] charIndex = {0};
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(10), event -> {
                    if (charIndex[0] < fullMessage.length()) {
                        currentText.append(fullMessage.charAt(charIndex[0]));
                        charIndex[0]++;

                        // Update the node with the current text, properly rendered as markdown
                        messageContentBox.getChildren().clear();
                        messageContentBox.getChildren().add(
                                com.helpdesk.util.MarkdownRenderer.renderMarkdown(currentText.toString())
                        );

                        scrollToBottom();
                    } else {
                        // Show feedback buttons when animation completes
                        feedbackButtons.setVisible(true);
                    }
                })
        );
        timeline.setCycleCount(fullMessage.length());
        timeline.play();
    }

    private void addBotMessage(String message, LocalDateTime timestamp) {
        VBox botMessageBox = new VBox(5);

        // Create the timestamp label
        Label timestampLabel = new Label(timestamp.format(timeFormatter));
        timestampLabel.getStyleClass().add("message-timestamp");
        timestampLabel.setAlignment(Pos.CENTER_LEFT);

        // Use our markdown renderer instead of a simple Label
        Node messageNode = com.helpdesk.util.MarkdownRenderer.renderMarkdown(message);
        VBox messageContentBox = new VBox(messageNode);
        messageContentBox.getStyleClass().add("bot-message");

        botMessageBox.getChildren().addAll(timestampLabel, messageContentBox);

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
            addBotMessage("I'm sorry this wasn't helpful. Let me try to connect you with more specific solutions.", LocalDateTime.now());
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

            Scene scene = new Scene(root, 650, 450);

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

    @FXML
    public void showKnowledgeBaseView() {
        chatView.setVisible(false);
        knowledgeBaseView.setVisible(true);
        historyView.setVisible(false);
        loadKnowledgeBase("");
    }

    @FXML
    public void showChatView() {
        chatView.setVisible(true);
        knowledgeBaseView.setVisible(false);
        historyView.setVisible(false);
    }

    @FXML
    public void showHistoryView() {
        chatView.setVisible(false);
        knowledgeBaseView.setVisible(false);
        historyView.setVisible(true);
        loadChatHistory();
    }

    private void loadKnowledgeBase(String searchQuery) {
        kbContainer.getChildren().clear();
        List<KnowledgeBase> entries = databaseService.getKnowledgeBase();

        // Group entries by category
        Map<String, List<KnowledgeBase>> categorizedEntries = entries.stream()
                .filter(entry -> searchQuery.isEmpty() ||
                        entry.getProblem().toLowerCase().contains(searchQuery.toLowerCase()) ||
                        entry.getKeywords().toLowerCase().contains(searchQuery.toLowerCase()))
                .collect(Collectors.groupingBy(KnowledgeBase::getCategory));

        categorizedEntries.forEach((category, categoryEntries) -> {
            // Create category section
            Label categoryLabel = new Label(category);
            categoryLabel.getStyleClass().add("kb-category");
            kbContainer.getChildren().add(categoryLabel);

            // Create cards for each entry
            categoryEntries.forEach(entry -> {
                VBox card = createKnowledgeBaseCard(entry);
                kbContainer.getChildren().add(card);
            });
        });
    }

    private void loadChatHistory() {
        historyContainer.getChildren().clear();
        List<ChatMessage> history = databaseService.getConversationHistory();
        
        for (ChatMessage message : history) {
            if (message.isUser()) {
                addUserMessage(message.getContent(), message.getTimestamp());
            } else {
                addBotMessage(message.getContent(), message.getTimestamp());
            }
        }
    }

    private VBox createKnowledgeBaseCard(KnowledgeBase entry) {
        VBox card = new VBox(10);
        card.getStyleClass().add("kb-card");

        // Category tag
        Label categoryTag = new Label(entry.getCategory());
        categoryTag.getStyleClass().add("kb-tag");

        // Problem title
        Label problemLabel = new Label(entry.getProblem());
        problemLabel.getStyleClass().add("kb-problem");
        problemLabel.setWrapText(true);

        // Keywords
        HBox keywordsBox = new HBox(5);
        keywordsBox.getStyleClass().add("kb-keywords");
        for (String keyword : entry.getKeywords().split(",")) {
            Label keywordTag = new Label(keyword.trim());
            keywordTag.getStyleClass().add("kb-keyword-tag");
            keywordsBox.getChildren().add(keywordTag);
        }

        // Solution content
        VBox solutionBox = new VBox(5);
        solutionBox.getStyleClass().add("kb-solution");
        
        // Parse and format the solution steps
        String[] steps = entry.getSolution().split("\n");
        for (String step : steps) {
            if (!step.trim().isEmpty()) {
                Label stepLabel = new Label(step.trim());
                stepLabel.setWrapText(true);
                stepLabel.getStyleClass().add("kb-step");
                solutionBox.getChildren().add(stepLabel);
            }
        }

        // Use in Chat button with icon
        Button useInChatButton = new Button("Use in Chat");
        FontIcon chatIcon = new FontIcon("fa-comments");  
        chatIcon.setIconSize(14);
        useInChatButton.setGraphic(chatIcon);
        useInChatButton.getStyleClass().add("kb-use-button");
        useInChatButton.setOnAction(e -> {
            showChatView();
            messageInput.setText(entry.getProblem());
            sendMessage();
        });

        // Optional feedback buttons
        HBox feedbackBox = new HBox(10);
        feedbackBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button helpfulButton = new Button("Helpful");
        helpfulButton.getStyleClass().addAll("kb-feedback-button", "helpful");
        FontIcon thumbsUpIcon = new FontIcon("fa-thumbs-up");
        thumbsUpIcon.setIconSize(14);
        helpfulButton.setGraphic(thumbsUpIcon);
        
        Button notHelpfulButton = new Button("Not Helpful");
        notHelpfulButton.getStyleClass().addAll("kb-feedback-button", "not-helpful");
        FontIcon thumbsDownIcon = new FontIcon("fa-thumbs-down");
        thumbsDownIcon.setIconSize(14);
        notHelpfulButton.setGraphic(thumbsDownIcon);
        
        feedbackBox.getChildren().addAll(helpfulButton, notHelpfulButton);

        // Add all components to the card
        card.getChildren().addAll(
            categoryTag,
            problemLabel,
            keywordsBox,
            solutionBox,
            useInChatButton,
            feedbackBox
        );

        return card;
    }
}