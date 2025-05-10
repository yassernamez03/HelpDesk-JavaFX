package com.helpdesk.service;

import com.helpdesk.model.ChatMessage;
import com.helpdesk.model.KnowledgeBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private Connection connection;

    public DatabaseService() {
        try {
            // Initialize database
            connection = DriverManager.getConnection("jdbc:sqlite:helpdesk.db");
            createTablesIfNotExist();
            loadInitialKnowledgeBase();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTablesIfNotExist() throws SQLException {
        // Create conversations table
        Statement statement = connection.createStatement();
        statement.execute(
                "CREATE TABLE IF NOT EXISTS conversations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_message TEXT, " +
                        "bot_response TEXT, " +
                        "timestamp TEXT, " +
                        "helpful BOOLEAN DEFAULT NULL)"
        );

        // Create knowledge base table
        statement.execute(
                "CREATE TABLE IF NOT EXISTS knowledge_base (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "category TEXT, " +
                        "keywords TEXT, " +
                        "problem TEXT, " +
                        "solution TEXT)"
        );

        statement.close();
    }

    private void loadInitialKnowledgeBase() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM knowledge_base");

            if (resultSet.next() && resultSet.getInt(1) == 0) {
                // If knowledge base is empty, add initial data
                addKnowledgeBaseEntry("WiFi", "connection,network,internet,wifi",
                        "Unable to connect to WiFi",
                        "1. Check if WiFi is turned on (look for physical switch or function key)\n" +
                                "2. Ensure airplane mode is off\n" +
                                "3. Restart your router and wait 30 seconds\n" +
                                "4. Forget the network and reconnect\n" +
                                "5. Verify your password is correct");

                addKnowledgeBaseEntry("Password", "forgot,reset,password,account,login",
                        "Forgot password or need to reset",
                        "1. Visit the account recovery page\n" +
                                "2. Click on 'Forgot password'\n" +
                                "3. Enter your email address\n" +
                                "4. Check your email for reset instructions\n" +
                                "5. Create a new strong password");

                addKnowledgeBaseEntry("Printer", "printing,printer,scan,copy,document",
                        "Printer not working",
                        "1. Check if printer is powered on and connected\n" +
                                "2. Verify paper and ink/toner levels\n" +
                                "3. Restart the printer\n" +
                                "4. Check for paper jams\n" +
                                "5. Reinstall printer drivers");

                // Add more entries for other categories...
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addKnowledgeBaseEntry(String category, String keywords, String problem, String solution) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO knowledge_base (category, keywords, problem, solution) VALUES (?, ?, ?, ?)"
            );
            statement.setString(1, category);
            statement.setString(2, keywords);
            statement.setString(3, problem);
            statement.setString(4, solution);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveConversation(String userMessage, String botResponse, LocalDateTime timestamp) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO conversations (user_message, bot_response, timestamp) VALUES (?, ?, ?)"
            );
            statement.setString(1, userMessage);
            statement.setString(2, botResponse);
            statement.setString(3, timestamp.toString());
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveFeedback(String botResponse, boolean helpful) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE conversations SET helpful = ? WHERE bot_response = ? AND helpful IS NULL"
            );
            statement.setBoolean(1, helpful);
            statement.setString(2, botResponse);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<KnowledgeBase> getKnowledgeBase() {
        List<KnowledgeBase> knowledgeBaseList = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM knowledge_base");

            while (resultSet.next()) {
                KnowledgeBase entry = new KnowledgeBase(
                        resultSet.getInt("id"),
                        resultSet.getString("category"),
                        resultSet.getString("keywords"),
                        resultSet.getString("problem"),
                        resultSet.getString("solution")
                );
                knowledgeBaseList.add(entry);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return knowledgeBaseList;
    }

    public List<ChatMessage> getConversationHistory() {
        List<ChatMessage> history = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM conversations ORDER BY timestamp");

            while (resultSet.next()) {
                // Create and add user message
                ChatMessage userMessage = new ChatMessage(
                        resultSet.getString("user_message"),
                        true,
                        LocalDateTime.parse(resultSet.getString("timestamp"))
                );
                history.add(userMessage);

                // Create and add bot response
                ChatMessage botResponse = new ChatMessage(
                        resultSet.getString("bot_response"),
                        false,
                        LocalDateTime.parse(resultSet.getString("timestamp"))
                );
                history.add(botResponse);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return history;
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}