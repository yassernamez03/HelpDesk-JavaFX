package com.helpdesk.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageParser {

    // Common IT problem keywords
    private static final Map<String, List<String>> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("wifi", Arrays.asList("wifi", "internet", "connection", "network", "router", "wireless"));
        KEYWORDS.put("password", Arrays.asList("password", "login", "credential", "forgot", "reset", "account"));
        KEYWORDS.put("printer", Arrays.asList("print", "printer", "scanner", "copy", "ink", "toner", "paper"));
        KEYWORDS.put("software", Arrays.asList("install", "program", "software", "application", "app", "download"));
        KEYWORDS.put("email", Arrays.asList("email", "mail", "outlook", "gmail", "inbox", "send"));
        KEYWORDS.put("storage", Arrays.asList("disk", "storage", "space", "full", "memory", "drive", "clean"));
    }

    /**
     * Parses a message to determine the IT problem category
     * @param message The user message
     * @return The detected category or null if none found
     */
    public static String detectCategory(String message) {
        String lowercaseMessage = message.toLowerCase();
        Map<String, Integer> categoryScores = new HashMap<>();

        // Score each category based on keyword matches
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (lowercaseMessage.contains(keyword)) {
                    score++;
                }
            }

            if (score > 0) {
                categoryScores.put(category, score);
            }
        }

        // Return the category with the highest score, or null if none found
        if (categoryScores.isEmpty()) {
            return null;
        }

        return Collections.max(categoryScores.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * Extracts device type from message if mentioned
     * @param message The user message
     * @return The detected device type or null if none found
     */
    public static String detectDeviceType(String message) {
        String lowercaseMessage = message.toLowerCase();

        Map<String, String> devicePatterns = new HashMap<>();
        devicePatterns.put("(\\b(windows|win|pc|desktop|laptop)\\b)", "Windows PC");
        devicePatterns.put("(\\b(mac|macbook|imac|apple computer)\\b)", "Mac");
        devicePatterns.put("(\\b(iphone|ios)\\b)", "iPhone");
        devicePatterns.put("(\\b(android|samsung|pixel|oneplus)\\b)", "Android");
        devicePatterns.put("(\\b(linux|ubuntu|fedora|debian)\\b)", "Linux");

        for (Map.Entry<String, String> entry : devicePatterns.entrySet()) {
            Pattern pattern = Pattern.compile(entry.getKey());
            Matcher matcher = pattern.matcher(lowercaseMessage);

            if (matcher.find()) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Extracts error codes from message if present
     * @param message The user message
     * @return List of error codes found in the message
     */
    public static List<String> extractErrorCodes(String message) {
        List<String> errorCodes = new ArrayList<>();

        // Common error code patterns
        String[] patterns = {
                "error\\s+(?:code)?\\s*(\\w+\\d+\\w*)",  // Error CODE123
                "(0x[0-9a-fA-F]+)",                      // Hexadecimal codes like 0x80004005
                "\\b([A-Z]\\d{2,}-\\d{3,})\\b"           // Codes like E23-001 or BSoD codes
        };

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);

            while (matcher.find()) {
                errorCodes.add(matcher.group(1));
            }
        }

        return errorCodes;
    }
}