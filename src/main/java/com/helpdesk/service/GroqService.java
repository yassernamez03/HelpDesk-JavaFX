package com.helpdesk.service;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroqService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final OkHttpClient client;
    private final Gson gson;
    private String apiKey;

    public GroqService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        // Load API key from environment or config
        this.apiKey = "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";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String processQuery(String userQuery) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Please set up your Groq API key in settings.";
        }

        // Prepare request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "llama3-70b-8192"); // Choose appropriate model

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an IT support helpdesk assistant. Your goal is to help users solve common IT problems. " +
                "Provide clear, step-by-step solutions for issues like WiFi connection problems, password resets, printer issues, " +
                "software installation, computer maintenance, and other common IT issues. Be concise but thorough in your answers.");

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userQuery);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        payload.put("messages", messages); // This will be correctly serialized by Gson

        payload.put("temperature", 0.3); // Lower temperature for more focused responses
        payload.put("max_tokens", 500); // Limit response length

        // Create request
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                gson.toJson(payload)
        );

        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error: " + response.code() + " - " + response.message();
            }

            String responseBody = response.body().string();
            Map<String, Object> jsonResponse = gson.fromJson(responseBody, Map.class);

            // Extract the AI response from the JSON
            if (jsonResponse.containsKey("choices")) {

                // Correct handling of the ArrayList returned by Gson
                List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }

            return "Sorry, I couldn't process your request at this time.";
        }
    }
}