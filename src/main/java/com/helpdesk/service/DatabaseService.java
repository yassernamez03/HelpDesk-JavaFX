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
                // Network Issues
                addKnowledgeBaseEntry("WiFi", "connection,network,internet,wifi,wireless",
                        "Unable to connect to WiFi",
                        "1. Check if WiFi is turned on (look for physical switch or function key)\n" +
                                "2. Ensure airplane mode is off\n" +
                                "3. Restart your router and wait 30 seconds\n" +
                                "4. Forget the network and reconnect\n" +
                                "5. Verify your password is correct");

                addKnowledgeBaseEntry("Network", "slow,internet,speed,connection,network",
                        "Slow Internet Connection",
                        "1. Run a speed test at speedtest.net\n" +
                                "2. Check other devices on the network\n" +
                                "3. Position your device closer to the router\n" +
                                "4. Clear browser cache and cookies\n" +
                                "5. Contact your ISP if problems persist");

                // System Issues
                addKnowledgeBaseEntry("System", "slow,performance,computer,laptop,pc",
                        "Computer Running Slowly",
                        "1. Check Task Manager for resource usage\n" +
                                "2. Close unnecessary programs\n" +
                                "3. Run disk cleanup and defragmentation\n" +
                                "4. Update Windows and drivers\n" +
                                "5. Consider adding more RAM or upgrading to SSD");

                addKnowledgeBaseEntry("Windows", "blue,screen,crash,error,bsod",
                        "Blue Screen of Death (BSOD)",
                        "1. Note down the error code\n" +
                                "2. Update all drivers\n" +
                                "3. Run Windows Memory Diagnostic\n" +
                                "4. Check for malware\n" +
                                "5. System restore to last known good configuration");

                // Hardware Issues
                addKnowledgeBaseEntry("Printer", "printing,printer,scan,copy,document",
                        "Printer not working",
                        "1. Check if printer is powered on and connected\n" +
                                "2. Verify paper and ink/toner levels\n" +
                                "3. Clear print queue\n" +
                                "4. Check for paper jams\n" +
                                "5. Reinstall printer drivers");

                addKnowledgeBaseEntry("Hardware", "usb,device,external,drive,hardware",
                        "USB Device Not Recognized",
                        "1. Try a different USB port\n" +
                                "2. Unplug other USB devices\n" +
                                "3. Check Device Manager for errors\n" +
                                "4. Update USB drivers\n" +
                                "5. Restart computer with device plugged in");

                // Software Issues
                addKnowledgeBaseEntry("Software", "install,program,application,software,app",
                        "Program Won't Install",
                        "1. Check system requirements\n" +
                                "2. Run as administrator\n" +
                                "3. Disable antivirus temporarily\n" +
                                "4. Clean temp files\n" +
                                "5. Download fresh copy of installer");

                addKnowledgeBaseEntry("Browser", "chrome,firefox,edge,browser,internet",
                        "Browser Issues",
                        "1. Clear browser cache and cookies\n" +
                                "2. Disable extensions\n" +
                                "3. Update browser to latest version\n" +
                                "4. Check for malware\n" +
                                "5. Reset browser settings");

                // Security Issues
                addKnowledgeBaseEntry("Security", "virus,malware,antivirus,security,protection",
                        "Suspected Malware Infection",
                        "1. Run full system antivirus scan\n" +
                                "2. Update antivirus definitions\n" +
                                "3. Boot in Safe Mode if needed\n" +
                                "4. Check startup programs\n" +
                                "5. Consider system restore");

                addKnowledgeBaseEntry("Password", "forgot,reset,password,account,login",
                        "Password Reset Required",
                        "1. Visit the account recovery page\n" +
                                "2. Click on 'Forgot password'\n" +
                                "3. Enter your email address\n" +
                                "4. Check your email for reset instructions\n" +
                                "5. Create a new strong password");
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