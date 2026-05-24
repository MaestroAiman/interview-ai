package com.pfa.interviewai.service;

import com.pfa.interviewai.model.User;
import com.pfa.interviewai.repository.UserRepository;
import com.pfa.interviewai.security.JwtUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.security.MessageDigest;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class AuthService {

    @Inject private UserRepository userRepository;
    @Inject private JwtUtil jwtUtil;

    private static final String HASH_ALGO = "SHA-256";

    public String register(String name, String email,
                            String password, String confirmPassword)
            throws ExecutionException, InterruptedException {

        if (!password.equals(confirmPassword))
            throw new IllegalArgumentException("Passwords do not match");

        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered");

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(hashPassword(password))
                .role("USER")
                .createdAt(java.time.Instant.now().toString())
                .build();

        userRepository.save(user);
        return jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole());
    }

    public String login(String email, String password)
            throws ExecutionException, InterruptedException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!hashPassword(password).equals(user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");

        return jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole());
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
