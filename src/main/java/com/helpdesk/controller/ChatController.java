package com.helpdesk.controller;

import com.helpdesk.model.ChatMessage;
import com.helpdesk.model.Course;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class ChatController {    
    @FXML
    private ScrollPane chatScrollPane;

    @FXML
    private VBox chatContainer;

    @FXML
    private TextField messageInput;

    @FXML
    private Button recordButton;

    @FXML
    private FontIcon micIcon;

    // Audio recording components
    private boolean isRecording = false;
    private TargetDataLine audioLine;
    private ByteArrayOutputStream audioData;
    private Thread recordingThread;

    @FXML
    private BorderPane chatView;

    @FXML
    private BorderPane knowledgeBaseView;

    @FXML
    private BorderPane libraryView;

    @FXML
    private BorderPane historyView;

    @FXML
    private VBox kbContainer;

    @FXML
    private VBox historyContainer;

    @FXML
    private TextField searchField;

    @FXML
    private TextField librarySearchField;

    @FXML
    private TextField historySearchField;

    @FXML
    private Button chatButton;

    @FXML
    private Button libraryButton;

    @FXML
    private Button knowledgeBaseButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button newChatButton;

    @FXML
    private FlowPane coursesContainer;

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
        });        // Add history search functionality
        if (historySearchField != null) {
            historySearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                loadChatHistory();
            });
        }

        // Add library search functionality
        if (librarySearchField != null) {
            librarySearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                // This will be handled by the LibraryController
                loadCourses(newValue);
            });
        }
    }

    private void applyCurrentTheme() {
        String theme = preferences.get("theme", "light");
        String mobileCssPath = theme.equals("dark") ? "/css/mobile-dark.css" : "/css/mobile-light.css";
        String libraryCssPath = theme.equals("dark") ? "/css/library-dark.css" : "/css/library-light.css";

        Scene scene = chatView.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().addAll(
                getClass().getResource(mobileCssPath).toExternalForm(),
                getClass().getResource(libraryCssPath).toExternalForm()
            );
        }

        // Initialize courses for the library
        loadCourses("");
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

        // Create a container for the message content and speaker button
        HBox messageWithSpeakerBox = new HBox(10);
        messageWithSpeakerBox.setAlignment(Pos.CENTER_LEFT);

        // Add speaker button (initially hidden until animation completes)
        Button speakerButton = new Button();
        speakerButton.getStyleClass().addAll("speaker-button", "modern-button");
        FontIcon speakerIcon = new FontIcon("fa-volume-up");
        speakerIcon.setIconSize(16);
        speakerButton.setGraphic(speakerIcon);
        speakerButton.setTooltip(new Tooltip("Listen to this message"));
//        speakerButton.setVisible(false); // Hide until animation completes

        // Set the action for the speaker button
        speakerButton.setOnAction(e -> {
            speakerButton.setDisable(true); // Disable button while processing

            // Show a loading indicator on the button
            FontIcon loadingIcon = new FontIcon("fa-spinner");
            loadingIcon.setIconSize(16);
            loadingIcon.getStyleClass().add("fa-spin");
            speakerButton.setGraphic(loadingIcon);

            // Process TTS in background
            CompletableFuture.runAsync(() -> {
                try {
                    playTextToSpeech(fullMessage);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showAlert("Error", "Could not play audio: " + ex.getMessage());
                    });
                } finally {
                    Platform.runLater(() -> {
                        // Restore original icon and enable button
                        speakerButton.setGraphic(speakerIcon);
                        speakerButton.setDisable(false);
                    });
                }
            });
        });

        speakerButton.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;");

        messageWithSpeakerBox.getChildren().addAll(messageContentBox, speakerButton);

        // We'll add content to this incrementally
        StringBuilder currentText = new StringBuilder();

        // Add feedback buttons
        HBox feedbackButtons = new HBox(5);
        feedbackButtons.getStyleClass().add("feedback-buttons");

        Button helpfulButton = new Button("👍 Helpful");
        helpfulButton.getStyleClass().add("feedback-button");
        helpfulButton.setOnAction(e -> provideFeedback(fullMessage, true));

        Button notHelpfulButton = new Button("👎 Not helpful");
        notHelpfulButton.getStyleClass().add("feedback-button");
        notHelpfulButton.setOnAction(e -> provideFeedback(fullMessage, false));

        feedbackButtons.getChildren().addAll(helpfulButton, notHelpfulButton);

        botMessageBox.getChildren().addAll(timestampLabel, messageWithSpeakerBox, feedbackButtons);

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
                        // Show feedback buttons and speaker button when animation completes
                        feedbackButtons.setVisible(true);
                        speakerButton.setVisible(true);
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

        // Create a container for the message content and speaker button
        HBox messageWithSpeakerBox = new HBox(10);
        messageWithSpeakerBox.setAlignment(Pos.CENTER_LEFT);

        // Add speaker button
        Button speakerButton = new Button();
        speakerButton.getStyleClass().addAll("speaker-button", "modern-button");
        FontIcon speakerIcon = new FontIcon("fa-volume-up");
        speakerIcon.setIconSize(16);
        speakerButton.setGraphic(speakerIcon);
        speakerButton.setTooltip(new Tooltip("Listen to this message"));

        // Set the action for the speaker button
        speakerButton.setOnAction(e -> {
            speakerButton.setDisable(true); // Disable button while processing

            // Show a loading indicator on the button
            FontIcon loadingIcon = new FontIcon("fa-spinner");
            loadingIcon.setIconSize(16);
            loadingIcon.getStyleClass().add("fa-spin");
            speakerButton.setGraphic(loadingIcon);

            // Process TTS in background
            CompletableFuture.runAsync(() -> {
                try {
                    playTextToSpeech(message);
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        showAlert("Error", "Could not play audio: " + ex.getMessage());
                    });
                } finally {
                    Platform.runLater(() -> {
                        // Restore original icon and enable button
                        speakerButton.setGraphic(speakerIcon);
                        speakerButton.setDisable(false);
                    });
                }
            });
        });

        messageWithSpeakerBox.getChildren().addAll(messageContentBox, speakerButton);
        botMessageBox.getChildren().addAll(timestampLabel, messageWithSpeakerBox);

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


    private void playTextToSpeech(String text) throws Exception {
        // Get the audio data from the Groq API
        byte[] audioData = groqService.convertTextToSpeech(text);

        // Create a temporary file to store the audio data
        File tempFile = File.createTempFile("tts_audio", ".wav");
        tempFile.deleteOnExit();

        // Write the audio data to the temporary file
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(audioData);
        }

        // Create a Media object from the temporary file
        Media media = new Media(tempFile.toURI().toString());

        // Create a MediaPlayer to play the audio
        MediaPlayer mediaPlayer = new MediaPlayer(media);

        // Play the audio
        Platform.runLater(() -> {
            mediaPlayer.play();
        });
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

    private void updateNavigationState(Button activeButton) {
        // Remove active state from all buttons        chatButton.getStyleClass().remove("nav-button-active");
        libraryButton.getStyleClass().remove("nav-button-active");
        knowledgeBaseButton.getStyleClass().remove("nav-button-active");
        historyButton.getStyleClass().remove("nav-button-active");

        // Add active state to the current button
        activeButton.getStyleClass().add("nav-button-active");
    }

    @FXML
    public void showKnowledgeBaseView() {
        chatView.setVisible(false);
        knowledgeBaseView.setVisible(true);
        historyView.setVisible(false);
        libraryView.setVisible(false);
        updateNavigationState(knowledgeBaseButton);
        loadKnowledgeBase("");
    }

    @FXML
    public void showChatView() {
        chatView.setVisible(true);
        knowledgeBaseView.setVisible(false);
        historyView.setVisible(false);
        libraryView.setVisible(false);
        updateNavigationState(chatButton);
    }

    @FXML
    public void showHistoryView() {
        chatView.setVisible(false);
        knowledgeBaseView.setVisible(false);
        historyView.setVisible(true);
        libraryView.setVisible(false);
        updateNavigationState(historyButton);
        loadChatHistory();
    }

    @FXML
    public void startNewChat() {
        chatContainer.getChildren().clear();

        // Add welcome message
        addBotMessage("👋 Hi there! I'm your IT Support assistant. How can I help you today?", LocalDateTime.now());

        // Add suggestion buttons
        addSuggestionButtons();

        // Switch to chat view if not already there
        showChatView();
    }

    @FXML
    public void showLibraryView() {
        chatView.setVisible(false);
        libraryView.setVisible(true);
        knowledgeBaseView.setVisible(false);
        historyView.setVisible(false);
        updateNavigationState(libraryButton);
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
        String searchQuery = historySearchField != null ? historySearchField.getText() : "";
        List<DatabaseService.Conversation> conversations = databaseService.getConversationHistory(searchQuery);

        // Group conversations by date
        Map<LocalDateTime, List<DatabaseService.Conversation>> conversationsByDate = conversations.stream()
                .collect(Collectors.groupingBy(
                    conv -> conv.getTimestamp().toLocalDate().atStartOfDay(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

        conversationsByDate.forEach((date, convs) -> {
            // Add date header
            Label dateLabel = new Label(date.toLocalDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
            dateLabel.getStyleClass().add("history-date-header");
            historyContainer.getChildren().add(dateLabel);

            // Add each conversation for this date
            convs.forEach(conv -> {
                VBox conversationBox = new VBox(10);
                conversationBox.getStyleClass().add("history-conversation-box");

                // Create preview of the conversation
                VBox previewBox = new VBox(5);
                previewBox.getStyleClass().add("conversation-preview");

                // Add timestamp
                Label timestampLabel = new Label(conv.getTimestamp().format(timeFormatter));
                timestampLabel.getStyleClass().add("conversation-timestamp");

                // Add first bot response preview
                Text previewText = new Text(conv.getFirstBotResponse());
                previewText.setWrappingWidth(300);
                previewText.getStyleClass().add("conversation-preview-text");

                previewBox.getChildren().addAll(timestampLabel, previewText);

                // Add conversation actions
                HBox actionBar = new HBox(10);
                actionBar.setAlignment(Pos.CENTER_RIGHT);
                actionBar.getStyleClass().add("conversation-actions");

                // Expand/Collapse button
                Button expandButton = new Button("", new FontIcon("fa-chevron-down"));
                expandButton.getStyleClass().add("expand-button");

                // Continue button
                Button continueButton = new Button("Continue", new FontIcon("fa-arrow-right"));
                continueButton.getStyleClass().addAll("history-action-button", "continue-button");
                continueButton.setOnAction(e -> continueConversation(conv.getMessages().get(0).getContent()));

                // Delete button
                Button deleteButton = new Button("Delete", new FontIcon("fa-trash"));
                deleteButton.getStyleClass().addAll("history-action-button", "delete-button");
                deleteButton.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Conversation");
                    alert.setHeaderText("Delete this conversation?");
                    alert.setContentText("This action cannot be undone.");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            ChatMessage firstMsg = conv.getMessages().get(0);
                            ChatMessage firstBotMsg = conv.getMessages().get(1);
                            databaseService.deleteConversation(
                                firstMsg.getContent(),
                                firstBotMsg.getContent(),
                                firstMsg.getTimestamp()
                            );
                            loadChatHistory();
                        }
                    });
                });

                actionBar.getChildren().addAll(expandButton, deleteButton, continueButton);

                // Create expandable detail view
                VBox detailView = new VBox(10);
                detailView.getStyleClass().add("conversation-detail");
                detailView.setVisible(false);
                detailView.setManaged(false);

                // Add all messages to detail view
                conv.getMessages().forEach(msg -> addHistoryMessage(detailView, msg));

                // Setup expand/collapse functionality
                expandButton.setOnAction(e -> {
                    boolean isExpanded = detailView.isVisible();
                    detailView.setVisible(!isExpanded);
                    detailView.setManaged(!isExpanded);
                    ((FontIcon)expandButton.getGraphic()).setIconLiteral(
                        isExpanded ? "fa-chevron-down" : "fa-chevron-up"
                    );
                });

                conversationBox.getChildren().addAll(previewBox, actionBar, detailView);
                historyContainer.getChildren().add(conversationBox);
            });
        });
    }

    private void addHistoryMessage(VBox container, ChatMessage message) {
        VBox messageBox = new VBox(3);
        messageBox.getStyleClass().add(message.isUser() ? "user-message" : "bot-message");

        Label timestampLabel = new Label(message.getTimestamp().format(timeFormatter));
        timestampLabel.getStyleClass().add("message-timestamp");

        VBox contentBox = new VBox();
        if (message.isUser()) {
            Label contentLabel = new Label(message.getContent());
            contentLabel.setWrapText(true);
            contentBox.getChildren().add(contentLabel);
        } else {
            contentBox.getChildren().add(
                com.helpdesk.util.MarkdownRenderer.renderMarkdown(message.getContent())
            );
        }

        messageBox.getChildren().addAll(timestampLabel, contentBox);
        container.getChildren().add(messageBox);
    }

    private void continueConversation(String message) {
        showChatView();
        messageInput.setText(message);
        sendMessage();
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

    private void loadCourses(String searchText) {
        if (coursesContainer == null) return;

        coursesContainer.getChildren().clear();
        List<Course> courses = getCourses(); // This would normally come from a service

        if (searchText != null && !searchText.isEmpty()) {
            courses = courses.stream()
                .filter(course -> 
                    course.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                    course.getDescription().toLowerCase().contains(searchText.toLowerCase()))
                .collect(Collectors.toList());
        }

        for (Course course : courses) {
            VBox courseCard = course.createCourseCard();
            coursesContainer.getChildren().add(courseCard);
        }
    }

    private List<Course> getCourses() {
        List<Course> courses = new ArrayList<>();
        courses.add(new Course(
            "Network Troubleshooting Basics",
            "Learn the fundamentals of diagnosing and fixing common network issues",
            "/images/network-troubleshooting.jpg"
        ));
        courses.add(new Course(
            "Windows Security Essentials",
            "Master essential Windows security practices and malware protection",
            "/images/windows-security.jpg"
        ));
        courses.add(new Course(
            "Software Installation Guide",
            "Step-by-step guides for installing and configuring common software",
            "/images/software-installation.jpg"
        ));
        return courses;
    }

    @FXML
    public void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        try {
            // Update UI to show recording state
            isRecording = true;
            micIcon.setIconLiteral("fa-microphone-slash");
            micIcon.setIconColor(Color.RED);
            recordButton.getStyleClass().add("recording");

            // Configure audio format - use standard WAV format
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    16000.0f, // 16kHz sample rate
                    16,       // 16-bit samples
                    1,        // Mono
                    2,        // Frame size (2 bytes for 16-bit mono)
                    16000.0f, // Frame rate
                    false     // Little endian
            );

            // Get and open the target data line
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                showAlert("Error", "Audio recording is not supported on this system.");
                return;
            }

            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();

            // Create a stream to hold the captured data
            audioData = new ByteArrayOutputStream();

            // Create a thread to capture the audio data
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                int bytesRead;

                try {
                    while (isRecording) {
                        bytesRead = audioLine.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            audioData.write(buffer, 0, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to start recording: " + e.getMessage());
                    isRecording = false;
                }
            });

            recordingThread.start();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to start recording: " + e.getMessage());
            isRecording = false;
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        // Update UI to show stopped state
        isRecording = false;
        micIcon.setIconLiteral("fa-microphone");
        micIcon.setIconColor(Color.BLACK);
        recordButton.getStyleClass().remove("recording");

        // Stop and close the audio line
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }

        // Wait for the recording thread to finish
        try {
            if (recordingThread != null) {
                recordingThread.join();
            }

            // Process the recorded audio
            if (audioData != null && audioData.size() > 0) {
                processRecordedAudio();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to process recording: " + e.getMessage());
        }
    }

    private void processRecordedAudio() {
        // Show a processing message and visual indicator
        Platform.runLater(() -> {
            messageInput.setText("Processing voice message...");
            messageInput.setDisable(true);
            messageInput.getStyleClass().add("processing"); // Add processing style
        });

        // Process the audio data asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Get the audio data as a byte array
                byte[] audioBytes = audioData.toByteArray();

                // Convert speech to text using GroqService
                String recognizedText = groqService.convertSpeechToText(audioBytes);

                // Update the UI with the recognized text
                Platform.runLater(() -> {
                    // Remove processing style
                    messageInput.getStyleClass().remove("processing");

                    if (recognizedText.startsWith("Error") || recognizedText.startsWith("Please set up")) {
                        // Show error message
                        showAlert("Speech Recognition Error", recognizedText);
                        messageInput.setText("");
                    } else {
                        // Show the transcribed text
                        messageInput.setText(recognizedText);
                    }
                    messageInput.setDisable(false);
                    messageInput.requestFocus();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    // Remove processing style
                    messageInput.getStyleClass().remove("processing");

                    showAlert("Error", "Failed to process audio: " + e.getMessage());
                    messageInput.setText("");
                    messageInput.setDisable(false);
                });
            }
        });
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Exits the application when the exit button is clicked
     */
    @FXML
    public void exitApplication() {
        Platform.exit();
    }
}
