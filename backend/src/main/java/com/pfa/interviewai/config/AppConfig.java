package com.pfa.interviewai.config;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppConfig {

    public String getClaudeApiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank())
            throw new IllegalStateException(
                "ANTHROPIC_API_KEY environment variable is not set. " +
                "Set it before starting the application.");
        return key.strip();
    }

    public String getClaudeModel() {
        return "claude-sonnet-4-6";
    }

    public String getFirebaseProjectId() {
        return "interview-simulator-pfa";
    }

    public String getJwtSecret() {
        // Default is a valid Base64-encoded 256-bit key; override via JWT_SECRET env var
        return System.getenv().getOrDefault("JWT_SECRET",
            "REDACTED_JWT_SECRET");
    }

    public long getJwtExpirationMs() {
        return 86400000L; // 24 hours
    }

    public String getClaudeApiUrl() {
        return "https://api.anthropic.com/v1/messages";
    }

    public int getClaudeMaxTokens() {
        return 1500;
    }

    public int getClaudeTimeoutSeconds() {
        return 30;
    }

    public String getActiveAiProvider() {
        return System.getenv().getOrDefault("AI_PROVIDER", "ollama");
    }

    public String getOllamaApiUrl() {
        return System.getenv().getOrDefault("OLLAMA_API_URL", "http://localhost:11434");
    }

    public String getOllamaModel() {
        return System.getenv().getOrDefault("OLLAMA_MODEL", "mistral:7b");
    }

    public int getOllamaConnectTimeoutMs() {
        String v = System.getenv("OLLAMA_CONNECT_TIMEOUT_MS");
        return v != null ? Integer.parseInt(v) : 5000;
    }

    public int getOllamaReadTimeoutMs() {
        String v = System.getenv("OLLAMA_READ_TIMEOUT_MS");
        return v != null ? Integer.parseInt(v) : 180000;
    }
}
