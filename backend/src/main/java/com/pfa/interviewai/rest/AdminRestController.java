package com.pfa.interviewai.rest;

import com.pfa.interviewai.rest.dto.UserAdminDto;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
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
    @Inject private JwtUtil jwtUtil;

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
