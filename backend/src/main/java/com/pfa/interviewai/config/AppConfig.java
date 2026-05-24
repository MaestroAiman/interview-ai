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
}
