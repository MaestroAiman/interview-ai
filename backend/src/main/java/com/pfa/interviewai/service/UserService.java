package com.pfa.interviewai.service;

import com.pfa.interviewai.model.User;
import com.pfa.interviewai.repository.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class UserService {

    @Inject
    private UserRepository userRepository;

    public User findById(String id) throws ExecutionException, InterruptedException {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public List<User> findAll() throws ExecutionException, InterruptedException {
        return userRepository.findAll();
    }

    public void setRole(String userId, String role) throws ExecutionException, InterruptedException {
        User user = findById(userId);
        user.setRole(role);
        userRepository.update(user);
    }

    public void updateProfile(User user) throws ExecutionException, InterruptedException {
        userRepository.update(user);
    }

    public User createUser(String name, String email, String password,
                            String role, String targetPosition)
            throws ExecutionException, InterruptedException {
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered");
        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(hashPassword(password))
                .role(role != null ? role.toUpperCase() : "USER")
                .targetPosition(targetPosition)
                .createdAt(java.time.Instant.now().toString())
                .build();
        return userRepository.save(user);
    }

    public void updateUser(String userId, String name, String email,
                            String targetPosition, String role)
            throws ExecutionException, InterruptedException {
        User user = findById(userId);
        user.setName(name);
        user.setEmail(email);
        user.setTargetPosition(targetPosition);
        if (role != null) user.setRole(role.toUpperCase());
        userRepository.update(user);
    }

    public void deleteUser(String userId) throws ExecutionException, InterruptedException {
        userRepository.delete(userId);
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
