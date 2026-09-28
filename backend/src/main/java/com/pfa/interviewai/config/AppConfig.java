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
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET n'est pas défini : ajoutez-le dans backend/.env (voir .env.example)");
        }
        return secret;
    }

    public long getJwtExpirationMs() {
        return 86_400_000L; // 24 hours in ms
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
