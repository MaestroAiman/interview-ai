package com.pfa.interviewai.rest;

import com.pfa.interviewai.rest.dto.HistoryDetailsDto;
import com.pfa.interviewai.rest.dto.HistoryPageDto;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.SessionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/history")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class HistoryRestController extends BaseRestController {

    @Inject private SessionService sessionService;
    @Inject private JwtUtil jwtUtil;

    @GET
    public Response getHistory(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            if (size < 1 || size > 100) size = 10;
            if (page < 0) page = 0;
            HistoryPageDto result = sessionService.getSessionHistory(userId, page, size);
            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/{id}/details")
    public Response getDetails(
            @PathParam("id") String sessionId,
            @CookieParam("interview_jwt") String cookieToken,
            @HeaderParam("Authorization") String authHeader) {

        String token = resolveToken(cookieToken, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            boolean isAdmin = "ADMIN".equals(jwtUtil.extractRole(token));
            HistoryDetailsDto result = isAdmin
                    ? sessionService.getHistoryDetailsAdmin(sessionId)
                    : sessionService.getHistoryDetails(sessionId, userId);
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("not found") ? 404
                : e.getMessage().contains("Access denied") ? 403 : 400;
            return Response.status(status).entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

}
