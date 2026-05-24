package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.User;
import com.pfa.interviewai.model.enums.SessionStatus;
import com.pfa.interviewai.service.SessionService;
import com.pfa.interviewai.service.UserService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
public class AdminBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private UserService userService;
    @Inject private AuthBean authBean;

    private List<InterviewSession> allSessions = List.of();
    private List<User> allUsers = List.of();

    private long completedCount;
    private long inProgressCount;
    private long abandonedCount;
    private double platformAvgScore;

    private String currentUserId;
    private String errorMessage;

    public void init() {
        if (!isAdmin()) return;
        User cu = authBean.getCurrentUser();
        currentUserId = (cu != null) ? cu.getId() : null;
        try {
            allSessions = sessionService.findAll();
            allUsers    = userService.findAll();
            completedCount  = count(SessionStatus.COMPLETED);
            inProgressCount = count(SessionStatus.IN_PROGRESS);
            abandonedCount  = count(SessionStatus.ABANDONED);
            platformAvgScore = allSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                    .mapToDouble(InterviewSession::getOverallScore)
                    .average().orElse(0.0);
        } catch (Exception e) {
            // keep empty lists on error
        }
    }

    public void promoteUser(String userId) {
        try {
            userService.setRole(userId, "ADMIN");
            errorMessage = null;
            init();
        } catch (Exception e) {
            errorMessage = "Failed to promote user: " + e.getMessage();
        }
    }

    public void demoteUser(String userId) {
        if (userId.equals(currentUserId)) {
            errorMessage = "You cannot demote yourself.";
            return;
        }
        try {
            userService.setRole(userId, "USER");
            errorMessage = null;
            init();
        } catch (Exception e) {
            errorMessage = "Failed to demote user: " + e.getMessage();
        }
    }

    public boolean canDemote(User u) {
        return "ADMIN".equals(u.getRole()) && !u.getId().equals(currentUserId);
    }

    public long sessionCountFor(String userId) {
        return allSessions.stream()
                .filter(s -> userId.equals(s.getUserId()))
                .count();
    }

    private boolean isAdmin() {
        return authBean.getCurrentUser() != null
                && "ADMIN".equals(authBean.getCurrentUser().getRole());
    }

    private long count(SessionStatus status) {
        return allSessions.stream().filter(s -> status == s.getStatus()).count();
    }

    public List<InterviewSession> getAllSessions() { return allSessions; }
    public List<User> getAllUsers()               { return allUsers; }
    public long getCompletedCount()              { return completedCount; }
    public long getInProgressCount()             { return inProgressCount; }
    public long getAbandonedCount()              { return abandonedCount; }
    public double getPlatformAvgScore()          { return platformAvgScore; }
    public String getCurrentUserId()             { return currentUserId; }
    public String getErrorMessage()              { return errorMessage; }
}
