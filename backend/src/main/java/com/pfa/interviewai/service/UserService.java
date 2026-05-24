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
}
