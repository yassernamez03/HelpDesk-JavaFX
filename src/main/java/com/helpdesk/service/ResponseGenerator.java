package com.helpdesk.service;

import com.helpdesk.model.KnowledgeBase;
import java.io.IOException;
import java.util.List;

public class ResponseGenerator {

    private final GroqService groqService;
    private final DatabaseService databaseService;

    public ResponseGenerator(GroqService groqService, DatabaseService databaseService) {
        this.groqService = groqService;
        this.databaseService = databaseService;
    }

    public String generateResponse(String userQuery) {
        try {
            // First, try to match query with knowledge base entries
            String knowledgeBaseResponse = findInKnowledgeBase(userQuery);

            if (knowledgeBaseResponse != null) {
                return knowledgeBaseResponse;
            }

            // If no match in knowledge base, use Groq API
            return groqService.processQuery(userQuery);

        } catch (IOException e) {
            e.printStackTrace();
            return "I'm having trouble connecting to my knowledge service right now. Please try again later.";
        }
    }

    private String findInKnowledgeBase(String query) {
        List<KnowledgeBase> knowledgeBaseEntries = databaseService.getKnowledgeBase();
        String queryLower = query.toLowerCase();

        for (KnowledgeBase entry : knowledgeBaseEntries) {
            String[] keywords = entry.getKeywords().split(",");

            // Check if query contains any keywords
            for (String keyword : keywords) {
                if (queryLower.contains(keyword.toLowerCase())) {
                    return "Here's a solution for your " + entry.getCategory() + " issue:\n\n" +
                            "Problem: " + entry.getProblem() + "\n\n" +
                            "Solution:\n" + entry.getSolution();
                }
            }
        }

        return null; // No match found
    }
}