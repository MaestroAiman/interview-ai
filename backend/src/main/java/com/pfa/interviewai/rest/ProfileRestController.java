package com.pfa.interviewai.rest;

import com.pfa.interviewai.model.User;
import com.pfa.interviewai.repository.UserRepository;
import com.pfa.interviewai.security.JwtUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Path("/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ProfileRestController extends BaseRestController {

    private static final Logger log = Logger.getLogger(ProfileRestController.class.getName());

    @Inject private UserRepository userRepository;
    @Inject private JwtUtil jwtUtil;

    @GET
    public Response getProfile(
            @CookieParam("interview_jwt") String cookie,
            @HeaderParam("Authorization") String authHeader
    ) {
        String token = resolveToken(cookie, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(401).entity(Map.of("error", "Unauthorized")).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty())
                return Response.status(404).entity(Map.of("error", "User not found")).build();
            User user = userOpt.get();
            return Response.ok(Map.of(
                    "name", user.getName() != null ? user.getName() : "",
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "targetPosition", user.getTargetPosition() != null ? user.getTargetPosition() : "",
                    "role", user.getRole() != null ? user.getRole() : "USER"
            )).build();
        } catch (Exception e) {
            log.severe("Error fetching profile: " + e.getMessage());
            return Response.status(500).entity(Map.of("error", "Internal error")).build();
        }
    }

    @PUT
    public Response updateProfile(
            UpdateProfileRequest req,
            @CookieParam("interview_jwt") String cookie,
            @HeaderParam("Authorization") String authHeader
    ) {
        String token = resolveToken(cookie, authHeader);
        if (token == null || !jwtUtil.isTokenValid(token))
            return Response.status(401).entity(Map.of("error", "Unauthorized")).build();

        if (req == null || req.getName() == null || req.getName().isBlank())
            return Response.status(400).entity(Map.of("error", "Le nom est requis")).build();

        if (req.getName().length() > 100)
            return Response.status(400).entity(Map.of("error", "Nom trop long (max 100 caractères)")).build();

        try {
            String userId = jwtUtil.extractUserId(token);
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty())
                return Response.status(404).entity(Map.of("error", "User not found")).build();

            User user = userOpt.get();
            user.setName(req.getName().trim());
            user.setTargetPosition(req.getTargetPosition() != null ? req.getTargetPosition().trim() : "");
            userRepository.update(user);

            return Response.ok(Map.of("message", "Profil mis à jour")).build();
        } catch (Exception e) {
            log.severe("Error updating profile: " + e.getMessage());
            return Response.status(500).entity(Map.of("error", "Internal error")).build();
        }
    }

    public static class UpdateProfileRequest {
        private String name;
        private String targetPosition;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTargetPosition() { return targetPosition; }
        public void setTargetPosition(String targetPosition) { this.targetPosition = targetPosition; }
    }
}
