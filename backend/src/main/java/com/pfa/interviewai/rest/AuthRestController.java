package com.pfa.interviewai.rest;

import com.pfa.interviewai.model.User;
import com.pfa.interviewai.repository.UserRepository;
import com.pfa.interviewai.rest.dto.AuthResponse;
import com.pfa.interviewai.rest.dto.LoginRequest;
import com.pfa.interviewai.rest.dto.RegisterRequest;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.AuthService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class AuthRestController {

    @Inject private AuthService authService;
    @Inject private UserRepository userRepository;
    @Inject private JwtUtil jwtUtil;

    @POST
    @Path("/register")
    public Response register(RegisterRequest req) {
        try {
            String token = authService.register(
                req.getName(), req.getEmail(), req.getPassword(), req.getPassword());
            String userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            return Response.ok(new AuthResponse(token, userId, req.getName(), role)).build();
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("already") ? 409 : 400;
            return Response.status(status)
                .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest req) {
        try {
            String token = authService.login(req.getEmail(), req.getPassword());
            String userId = jwtUtil.extractUserId(token);
            User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
            return Response.ok(new AuthResponse(token, userId, user.getName(), user.getRole())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/logout")
    public Response logout() {
        return Response.ok().build();
    }
}
