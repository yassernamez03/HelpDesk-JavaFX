package com.helpdesk.util;

public class Constants {
    // Application settings
    public static final String APP_NAME = "IT Support Helpdesk";
    public static final String APP_VERSION = "1.0.0";

    // Database settings
    public static final String DB_NAME = "helpdesk.db";
    public static final String DB_URL = "jdbc:sqlite:" + DB_NAME;

    // API settings
    public static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final String GROQ_AUDIO_API_URL = "https://api.groq.com/openai/v1/audio/translations";
    public static final String GROQ_TTS_API_URL = "https://api.groq.com/openai/v1/audio/speech";
    public static final String GROQ_MODEL = "llama3-70b-8192";
    public static final String GROQ_WHISPER_MODEL = "whisper-large-v3";
    public static final String GROQ_TTS_MODEL = "playai-tts";
    public static final String GROQ_TTS_VOICE = "Celeste-PlayAI";

    // UI settings
    public static final int WINDOW_WIDTH = 380;
    public static final int WINDOW_HEIGHT = 650;

    // Response timeout in seconds
    public static final int API_TIMEOUT = 30;

    // Predefined suggestions
    public static final String[] SUGGESTIONS = {
            "WiFi not working",
            "Reset password",
            "Printer issues",
            "Software installation"
    };

    // System message for API
    public static final String SYSTEM_MESSAGE =
            "You are an IT support helpdesk assistant. Your goal is to help users solve common IT problems. " +
                    "Provide clear, step-by-step solutions for issues like WiFi connection problems, password resets, " +
                    "printer issues, software installation, computer maintenance, and other common IT issues. " +
                    "Be concise but thorough in your answers.";

    // Preferences keys
    public static final String PREF_API_KEY = "groqApiKey";
    public static final String PREF_THEME = "theme";
    public static final String PREF_USERNAME = "username";
}
