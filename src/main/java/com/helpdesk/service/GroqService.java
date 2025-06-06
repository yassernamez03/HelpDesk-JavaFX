package com.helpdesk.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.helpdesk.util.Constants;
import okhttp3.*;
import okhttp3.MultipartBody;
import okhttp3.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class GroqService {

    @FunctionalInterface
    private interface ApiCallSupplier {
        Response call() throws IOException;
    }

    private static final String GROQ_API_URL = Constants.GROQ_API_URL;
    private static final String GROQ_AUDIO_API_URL = Constants.GROQ_AUDIO_API_URL;
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_BACKOFF_MS = 1000; // 1 second
    private final OkHttpClient client;
    private final Gson gson;
    private final Random random;
    private String apiKey; // Groq API key

    public GroqService() {
        // Create OkHttpClient with longer timeouts for audio processing
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.random = new Random();
        // Load API keys from environment or config
        this.apiKey = "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";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }



    private Response executeWithRetry(ApiCallSupplier apiCallSupplier) throws IOException {
        int retries = 0;
        int backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            Response response = apiCallSupplier.call();

            // If successful or not a 429 error, return the response
            if (response.isSuccessful() || response.code() != 429) {
                return response;
            }

            // Close the response body to avoid resource leaks
            if (response.body() != null) {
                response.body().close();
            }

            // If we've reached max retries, throw an exception
            if (retries >= MAX_RETRIES) {
                throw new IOException("Maximum retries reached for API call after receiving 429 errors");
            }

            // Calculate backoff time with jitter (randomness to avoid all clients retrying at the same time)
            int jitterMs = random.nextInt(backoffMs / 2);
            int sleepTimeMs = backoffMs + jitterMs;

            try {
                Thread.sleep(sleepTimeMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Retry interrupted", e);
            }

            // Increase backoff for next retry (exponential backoff)
            backoffMs *= 2;
            retries++;
        }
    }

    public String processQuery(String userQuery) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Please set up your Groq API key in settings.";
        }

        // Prepare request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", Constants.GROQ_MODEL); // Choose appropriate model

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

        // Execute request with retry for 429 errors
        try (Response response = executeWithRetry(() -> client.newCall(request).execute())) {
            if (!response.isSuccessful()) {
                if (response.code() == 429) {
                    return "Error: Too many requests. Please try again later.";
                }
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

    public String convertSpeechToText(byte[] audioData) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Please set up your Groq API key in settings.";
        }

        try {
            // Convert raw PCM data to a proper WAV file with headers
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    16000.0f, // 16kHz sample rate
                    16,       // 16-bit samples
                    1,        // Mono
                    2,        // Frame size (2 bytes for 16-bit mono)
                    16000.0f, // Frame rate
                    false     // Little endian
            );

            // Create a temporary file to store the WAV audio data
            File tempFile = File.createTempFile("audio", ".wav");

            // Convert raw PCM to WAV with proper headers
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(
                    bais,
                    format,
                    audioData.length / format.getFrameSize()
            );

            // Write the audio data to the temporary file as a WAV file
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, tempFile);

            // Create the request body using the temporary file
            RequestBody fileBody = RequestBody.create(MediaType.parse("audio/wav"), tempFile);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "audio.wav", fileBody)
                    .addFormDataPart("model", Constants.GROQ_WHISPER_MODEL)
                    .addFormDataPart("language", "en")
                    .build();

            Request request = new Request.Builder()
                    .url(GROQ_AUDIO_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error details";
                    return "Error: " + response.code() + " - " + response.message() + ". Details: " + errorBody;
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                if (jsonResponse.has("text")) {
                    return jsonResponse.get("text").getAsString();
                }
                return "Sorry, I couldn't transcribe your speech.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing audio: " + e.getMessage();
        }
    }

    /**
     * Converts text to speech using the Groq TTS API
     * @param text The text to convert to speech
     * @return The audio data as a byte array
     * @throws IOException If there's an error communicating with the API
     */
    public byte[] convertTextToSpeech(String text) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("Please set up your Groq API key in settings.");
        }

        // Prepare request payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", Constants.GROQ_TTS_MODEL);
        payload.put("input", text);
        payload.put("voice", Constants.GROQ_TTS_VOICE);
        payload.put("response_format", "wav");

        // Create request
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                gson.toJson(payload)
        );

        Request request = new Request.Builder()
                .url(Constants.GROQ_TTS_API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request with retry for 429 errors
        try (Response response = executeWithRetry(() -> client.newCall(request).execute())) {
            if (!response.isSuccessful()) {
                throw new IOException("Error: " + response.code() + " - " + response.message());
            }

            // Return the audio data as a byte array
            return response.body().bytes();
        }
    }
}
