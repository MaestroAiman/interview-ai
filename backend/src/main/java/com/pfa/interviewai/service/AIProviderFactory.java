package com.pfa.interviewai.service;

import com.pfa.interviewai.config.AppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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

    public AIProvider getProvider() {
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
