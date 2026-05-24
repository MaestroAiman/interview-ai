package com.pfa.interviewai.rest;

import com.pfa.interviewai.config.AppConfig;
import com.pfa.interviewai.rest.dto.HealthStatusDto;
import com.pfa.interviewai.service.ClaudeAIService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class HealthRestController {

    @Inject private ClaudeAIService claudeAIService;
    @Inject private AppConfig appConfig;

    @GET
    @Path("/ai")
    public Response checkAi() {
        long start = System.currentTimeMillis();
        try {
            boolean reachable = claudeAIService.pingApi();
            long latency = System.currentTimeMillis() - start;
            if (reachable) {
                return Response.ok(new HealthStatusDto(
                    "ok", "Claude API is reachable", latency, appConfig.getClaudeModel())).build();
            } else {
                return Response.status(503).entity(new HealthStatusDto(
                    "error", "Claude API ping returned unexpected response",
                    latency, appConfig.getClaudeModel())).build();
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            return Response.status(503).entity(new HealthStatusDto(
                "error", "Claude API unreachable: " + e.getMessage(),
                latency, appConfig.getClaudeModel())).build();
        }
    }
}
