package com.pfa.interviewai.rest;

import com.pfa.interviewai.rest.dto.AnswerResponse;
import com.pfa.interviewai.rest.dto.SubmitAnswerRequest;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.SessionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/session")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class SessionRestController {

    @Inject private SessionService sessionService;
    @Inject private JwtUtil jwtUtil;

    @POST
    @Path("/answer")
    public Response submitAnswer(
            SubmitAnswerRequest req,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            AnswerResponse result = sessionService.submitAnswer(req);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }

    private String resolveToken(String cookie, String header) {
        if (cookie != null && !cookie.isBlank()) return cookie;
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }
}
