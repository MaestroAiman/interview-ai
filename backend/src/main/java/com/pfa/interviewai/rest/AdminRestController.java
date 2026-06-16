package com.pfa.interviewai.rest;

import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.User;
import com.pfa.interviewai.model.enums.SessionStatus;
import com.pfa.interviewai.rest.dto.AdminDashboardDto;
import com.pfa.interviewai.rest.dto.AdminUserCreateRequest;
import com.pfa.interviewai.rest.dto.AdminUserUpdateRequest;
import com.pfa.interviewai.rest.dto.UserAdminDto;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.SessionService;
import com.pfa.interviewai.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class AdminRestController {

    @Inject private UserService userService;
    @Inject private SessionService sessionService;
    @Inject private JwtUtil jwtUtil;

    @GET
    @Path("/dashboard")
    public Response getDashboard(@HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            List<com.pfa.interviewai.model.User> users = userService.findAll();
            List<InterviewSession> allSessions = sessionService.findAll();

            long completedCount = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();
            long inProgressCount = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.IN_PROGRESS)
                .count();
            float platformAvgScore = (float) allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED && s.getOverallScore() > 0)
                .mapToDouble(InterviewSession::getOverallScore)
                .average()
                .orElse(0.0);

            List<AdminDashboardDto.SessionRowDto> rows = allSessions.stream()
                .map(s -> new AdminDashboardDto.SessionRowDto(
                    s.getId(),
                    s.getUserId(),
                    s.getPosition(),
                    s.getType()       != null ? s.getType().name()       : null,
                    s.getDifficulty() != null ? s.getDifficulty().name() : null,
                    s.getOverallScore(),
                    s.getStatus()     != null ? s.getStatus().name()     : null,
                    s.getStartedAt()
                ))
                .toList();

            AdminDashboardDto dto = new AdminDashboardDto(
                users.size(), completedCount, inProgressCount, platformAvgScore, rows);
            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/users")
    public Response listUsers(@HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            List<UserAdminDto> dtos = userService.findAll().stream()
                    .map(u -> new UserAdminDto(u.getId(), u.getName(), u.getEmail(),
                            u.getRole(), u.getTargetPosition(), u.getCreatedAt()))
                    .toList();
            return Response.ok(dtos).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/users/{userId}/promote")
    public Response promote(@PathParam("userId") String userId,
                            @HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            userService.setRole(userId, "ADMIN");
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/users/{userId}/demote")
    public Response demote(@PathParam("userId") String userId,
                           @HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        if (userId.equals(extractUserId(authHeader)))
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Cannot demote yourself")).build();
        try {
            userService.setRole(userId, "USER");
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/users")
    public Response createUser(AdminUserCreateRequest req,
                                @HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            User created = userService.createUser(
                    req.getName(), req.getEmail(), req.getPassword(),
                    req.getRole(), req.getTargetPosition());
            UserAdminDto dto = new UserAdminDto(
                    created.getId(), created.getName(), created.getEmail(),
                    created.getRole(), created.getTargetPosition(), created.getCreatedAt());
            return Response.status(Response.Status.CREATED).entity(dto).build();
        } catch (IllegalArgumentException e) {
            return Response.status(409)
                    .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @PUT
    @Path("/users/{userId}")
    public Response updateUser(@PathParam("userId") String userId,
                                AdminUserUpdateRequest req,
                                @HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            userService.updateUser(userId, req.getName(), req.getEmail(),
                    req.getTargetPosition(), req.getRole());
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/users/{userId}")
    public Response deleteUser(@PathParam("userId") String userId,
                                @HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        if (userId.equals(extractUserId(authHeader)))
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Cannot delete yourself")).build();
        try {
            userService.deleteUser(userId);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @DELETE
    @Path("/sessions/{sessionId}")
    public Response deleteSession(@PathParam("sessionId") String sessionId,
                                   @HeaderParam("Authorization") String authHeader) {
        if (!isAdmin(authHeader))
            return Response.status(Response.Status.FORBIDDEN).build();
        try {
            sessionService.deleteSessionById(sessionId);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage())).build();
        }
    }

    private String resolveToken(String header) {
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    private boolean isAdmin(String header) {
        String t = resolveToken(header);
        return t != null && jwtUtil.isTokenValid(t) && "ADMIN".equals(jwtUtil.extractRole(t));
    }

    private String extractUserId(String header) {
        String t = resolveToken(header);
        return t != null ? jwtUtil.extractUserId(t) : null;
    }
}
