package com.pfa.interviewai.rest;

import com.pfa.interviewai.rest.dto.BenchmarkResultDto;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.ClaudeAIService;
import com.pfa.interviewai.service.OllamaAIService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Admin-only endpoint that calls both AI providers with the same prompt in parallel
 * and returns a side-by-side latency + response comparison.
 *
 * Usage: GET /api/benchmark/ai?operation=generateQuestions
 * Requires: Authorization: Bearer <ADMIN_JWT>
 */
@Path("/benchmark")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class BenchmarkRestController {

    private static final String BENCH_TYPE       = "TECHNICAL";
    private static final String BENCH_POSITION   = "Software Engineer";
    private static final String BENCH_DIFFICULTY = "MID";
    private static final int    BENCH_COUNT      = 3;

    @Inject private ClaudeAIService claudeService;
    @Inject private OllamaAIService ollamaService;
    @Inject private JwtUtil         jwtUtil;

    @GET
    @Path("/ai")
    public Response benchmark(
            @QueryParam("operation") String operation,
            @HeaderParam("Authorization") String authHeader) {

        if (!isAdmin(authHeader)) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity("{\"error\":\"ADMIN role required\"}")
                .build();
        }

        String op = (operation == null || operation.isBlank()) ? "generateQuestions" : operation;

        // Both providers called in parallel — metrics recorded inside each service
        CompletableFuture<BenchmarkResultDto.ProviderResult> claudeFuture =
            CompletableFuture.supplyAsync(() -> run("claude", op));

        CompletableFuture<BenchmarkResultDto.ProviderResult> ollamaFuture =
            CompletableFuture.supplyAsync(() -> run("ollama", op));

        BenchmarkResultDto dto = new BenchmarkResultDto(
            op,
            BENCH_TYPE + " | " + BENCH_POSITION + " | " + BENCH_DIFFICULTY + " | count=" + BENCH_COUNT,
            claudeFuture.join(),
            ollamaFuture.join()
        );
        return Response.ok(dto).build();
    }

    private BenchmarkResultDto.ProviderResult run(String provider, String operation) {
        long start = System.currentTimeMillis();
        try {
            List<String> questions = "claude".equals(provider)
                ? claudeService.generateQuestions(BENCH_TYPE, BENCH_POSITION, BENCH_DIFFICULTY, BENCH_COUNT)
                : ollamaService.generateQuestions(BENCH_TYPE, BENCH_POSITION, BENCH_DIFFICULTY, BENCH_COUNT);

            long elapsed = System.currentTimeMillis() - start;
            String raw = questions.toString();
            String truncated = raw.length() > 1000 ? raw.substring(0, 1000) + "..." : raw;
            return new BenchmarkResultDto.ProviderResult(provider, elapsed, truncated, "success", null);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return new BenchmarkResultDto.ProviderResult(provider, elapsed, null, "error", e.getMessage());
        }
    }

    private boolean isAdmin(String header) {
        if (header == null || !header.startsWith("Bearer ")) return false;
        String token = header.substring(7);
        return jwtUtil.isTokenValid(token) && "ADMIN".equals(jwtUtil.extractRole(token));
    }
}
