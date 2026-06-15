package com.pfa.interviewai.service;

import com.pfa.interviewai.config.AppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import java.util.logging.Logger;

@ApplicationScoped
public class AIProviderFactory {

    private static final Logger log = Logger.getLogger(AIProviderFactory.class.getName());

    @Inject @Named("claude")
    private AIProvider claudeProvider;

    @Inject @Named("ollama")
    private AIProvider ollamaProvider;

    @Inject
    private AppConfig appConfig;

    @Inject
    private jakarta.inject.Provider<HttpServletRequest> requestProvider;

    public AIProvider getProvider() {
        // Request-level override via X-AI-Provider header (sent by mobile app)
        try {
            HttpServletRequest req = requestProvider.get();
            if (req != null) {
                String headerProvider = req.getHeader("X-AI-Provider");
                if ("claude".equalsIgnoreCase(headerProvider)) return claudeProvider;
                if ("ollama".equalsIgnoreCase(headerProvider)) return ollamaProvider;
            }
        } catch (Exception ignored) {
            // Outside HTTP context (e.g., startup / scheduled tasks) — fallback to env var
        }

        String active = appConfig.getActiveAiProvider().toLowerCase().trim();
        return switch (active) {
            case "claude" -> claudeProvider;
            case "ollama" -> ollamaProvider;
            default -> {
                log.warning("Unknown AI_PROVIDER '" + active + "', falling back to ollama");
                yield ollamaProvider;
            }
        };
    }
}
