package com.pfa.interviewai.rest;

import com.pfa.interviewai.model.enums.Difficulty;
import com.pfa.interviewai.model.enums.InterviewType;
import com.pfa.interviewai.rest.dto.AnswerFeedbackResponse;
import com.pfa.interviewai.rest.dto.SessionStateDto;
import com.pfa.interviewai.rest.dto.SessionSummaryDto;
import com.pfa.interviewai.rest.dto.StartSessionRequest;
import com.pfa.interviewai.rest.dto.StartSessionResponse;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.SessionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class InterviewRestController {

    @Inject private SessionService sessionService;
    @Inject private JwtUtil jwtUtil;

    @POST
    @Path("/start")
    public Response startSession(
            StartSessionRequest req,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        if (req.getType() == null || req.getType().isBlank())
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "type is required")).build();
        if (req.getPosition() == null || req.getPosition().isBlank())
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "position is required")).build();
        if (req.getQuestionCount() < 1 || req.getQuestionCount() > 20)
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "questionCount must be between 1 and 20")).build();

        try {
            InterviewType type = InterviewType.valueOf(req.getType().toUpperCase());
            Difficulty difficulty = req.getDifficulty() != null
                ? Difficulty.valueOf(req.getDifficulty().toUpperCase()) : Difficulty.MID;
            String userId = jwtUtil.extractUserId(token);

            StartSessionResponse result = sessionService.startAdaptiveSession(
                type, req.getPosition(), difficulty, req.getQuestionCount(), userId);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", e.getMessage())).build();
        } catch (RuntimeException e) {
            if (isAiError(e))
                return Response.status(503).entity(Map.of("error", "AI service unavailable: " + e.getMessage())).build();
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/answer")
    public Response submitAnswer(
            @PathParam("id") String sessionId,
            Map<String, String> body,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        String questionId = body != null ? body.get("questionId") : null;
        String answerText = body != null ? body.get("answerText") : null;
        if (questionId == null || questionId.isBlank())
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "questionId is required")).build();
        if (answerText == null || answerText.isBlank())
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "answerText is required")).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            AnswerFeedbackResponse result = sessionService.submitAdaptiveAnswer(
                sessionId, questionId, answerText, userId);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("not found") ? 404 : 400;
            return Response.status(status).entity(Map.of("error", e.getMessage())).build();
        } catch (RuntimeException e) {
            if (isAiError(e))
                return Response.status(503).entity(Map.of("error", "AI service unavailable: " + e.getMessage())).build();
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}")
    public Response getSession(
            @PathParam("id") String sessionId,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            SessionStateDto result = sessionService.getSessionState(sessionId, userId);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("not found") ? 404
                : e.getMessage().contains("Access denied") ? 403 : 400;
            return Response.status(status).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/{id}/end")
    public Response endSession(
            @PathParam("id") String sessionId,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            SessionSummaryDto result = sessionService.forceEndSession(sessionId, userId);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("not found") ? 404
                : e.getMessage().contains("Access denied") ? 403 : 400;
            return Response.status(status).entity(Map.of("error", e.getMessage())).build();
        } catch (RuntimeException e) {
            if (isAiError(e))
                return Response.status(503).entity(Map.of("error", "AI service unavailable: " + e.getMessage())).build();
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    private String resolveToken(String cookie, String header) {
        if (cookie != null && !cookie.isBlank()) return cookie;
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }

    private boolean isAiError(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("Claude API") || msg.contains("Anthropic API"));
    }
}
