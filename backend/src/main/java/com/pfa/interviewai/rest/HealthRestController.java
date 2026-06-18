package com.pfa.interviewai.rest;

import com.pfa.interviewai.config.AppConfig;
import com.pfa.interviewai.rest.dto.HealthStatusDto;
import com.pfa.interviewai.service.AIProviderFactory;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class HealthRestController {

    @Inject private AIProviderFactory aiProviderFactory;
    @Inject private AppConfig appConfig;
    @Context private HttpServletRequest request;

    @GET
    @Path("/ai")
    public Response checkAi() {
        String headerProvider = request.getHeader("X-AI-Provider");
        String providerName = (headerProvider != null && !headerProvider.isBlank())
            ? headerProvider.toLowerCase().trim()
            : appConfig.getActiveAiProvider().toLowerCase().trim();

        String model = "claude".equals(providerName)
            ? appConfig.getClaudeModel()
            : appConfig.getOllamaModel();
        String url = "ollama".equals(providerName) ? appConfig.getOllamaApiUrl() : null;

        long start = System.currentTimeMillis();
        try {
            boolean reachable = aiProviderFactory.getProvider().pingApi();
            long latency = System.currentTimeMillis() - start;
            if (reachable) {
                return Response.ok(new HealthStatusDto(
                    providerName, "ok", providerName + " API is reachable",
                    latency, model, url)).build();
            } else {
                String message = "ollama".equals(providerName)
                    ? "Make sure Ollama is running: ollama serve"
                    : "Claude API ping returned unexpected response";
                return Response.status(503).entity(new HealthStatusDto(
                    providerName, "unreachable", message,
                    latency, model, url)).build();
            }
        } catch (IllegalStateException e) {
            long latency = System.currentTimeMillis() - start;
            return Response.status(503).entity(new HealthStatusDto(
                providerName, "unreachable",
                "ANTHROPIC_API_KEY non configuré sur le serveur",
                latency, model, url)).build();
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            String message = "ollama".equals(providerName)
                ? "Make sure Ollama is running: ollama serve"
                : "Claude API indisponible: " + e.getMessage();
            return Response.status(503).entity(new HealthStatusDto(
                providerName, "unreachable", message,
                latency, model, url)).build();
        }
    }
}
