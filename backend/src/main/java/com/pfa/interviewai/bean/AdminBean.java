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
import java.util.ArrayList;
import java.util.Comparator;
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
    private String successMessage;

    private String sortField = "startedAt";
    private boolean sortAscending = false;

    // ── User CRUD form state ──────────────────────────────────────────────────
    private boolean showCreateForm = false;
    private boolean showEditForm   = false;
    private String  editUserId;

    private String formName;
    private String formEmail;
    private String formPassword;
    private String formRole = "USER";
    private String formTargetPosition;

    // ── Init ─────────────────────────────────────────────────────────────────

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
            applySorting();
        } catch (Exception e) {
        }
    }

    // ── Session actions ───────────────────────────────────────────────────────

    public void deleteSession(String sessionId) {
        if (!isAdmin()) return;
        try {
            sessionService.deleteSessionById(sessionId);
            errorMessage   = null;
            successMessage = null;
            init();
        } catch (Exception e) {
            errorMessage = "Failed to delete session: " + e.getMessage();
        }
    }

    public void sortBy(String field) {
        if (field.equals(sortField)) {
            sortAscending = !sortAscending;
        } else {
            sortField = field;
            sortAscending = true;
        }
        applySorting();
    }

    private void applySorting() {
        Comparator<InterviewSession> cmp;
        switch (sortField) {
            case "userId":
                cmp = Comparator.comparing(s -> s.getUserId() != null ? s.getUserId() : "");
                break;
            default: // "startedAt"
                cmp = Comparator.comparing(s -> s.getStartedAt() != null ? s.getStartedAt() : "");
                break;
        }
        if (!sortAscending) cmp = cmp.reversed();
        allSessions = new ArrayList<>(allSessions);
        allSessions.sort(cmp);
    }

    // ── Role management ───────────────────────────────────────────────────────

    public void promoteUser(String userId) {
        try {
            userService.setRole(userId, "ADMIN");
            errorMessage   = null;
            successMessage = null;
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
            errorMessage   = null;
            successMessage = null;
            init();
        } catch (Exception e) {
            errorMessage = "Failed to demote user: " + e.getMessage();
        }
    }

    public boolean canDemote(User u) {
        return "ADMIN".equals(u.getRole()) && !u.getId().equals(currentUserId);
    }

    // ── User CRUD ─────────────────────────────────────────────────────────────

    public void openCreateForm() {
        clearForm();
        showCreateForm = true;
        showEditForm   = false;
        errorMessage   = null;
        successMessage = null;
    }

    public void cancelCreateForm() {
        showCreateForm = false;
        clearForm();
    }

    public void saveNewUser() {
        if (!isAdmin()) return;
        try {
            userService.createUser(formName, formEmail, formPassword,
                                   formRole, formTargetPosition);
            successMessage = "User \"" + formName + "\" created successfully.";
            errorMessage   = null;
            showCreateForm = false;
            clearForm();
            allUsers = userService.findAll();
        } catch (Exception e) {
            errorMessage   = "Failed to create user: " + e.getMessage();
            successMessage = null;
        }
    }

    public void openEditUser(User u) {
        editUserId         = u.getId();
        formName           = u.getName();
        formEmail          = u.getEmail();
        formRole           = u.getRole() != null ? u.getRole() : "USER";
        formTargetPosition = u.getTargetPosition();
        formPassword       = null;
        showEditForm       = true;
        showCreateForm     = false;
        errorMessage       = null;
        successMessage     = null;
    }

    public void cancelEditForm() {
        showEditForm = false;
        clearForm();
    }

    public void saveEditUser() {
        if (!isAdmin()) return;
        try {
            userService.updateUser(editUserId, formName, formEmail,
                                   formTargetPosition, formRole);
            successMessage = "User updated successfully.";
            errorMessage   = null;
            showEditForm   = false;
            clearForm();
            allUsers = userService.findAll();
        } catch (Exception e) {
            errorMessage   = "Failed to update user: " + e.getMessage();
            successMessage = null;
        }
    }

    public void deleteUser(String userId) {
        if (!isAdmin()) return;
        if (userId.equals(currentUserId)) {
            errorMessage = "You cannot delete your own account.";
            return;
        }
        try {
            userService.deleteUser(userId);
            successMessage = "User deleted successfully.";
            errorMessage   = null;
            allUsers = userService.findAll();
            allSessions = sessionService.findAll();
            applySorting();
        } catch (Exception e) {
            errorMessage   = "Failed to delete user: " + e.getMessage();
            successMessage = null;
        }
    }

    private void clearForm() {
        formName = null;
        formEmail = null;
        formPassword = null;
        formTargetPosition = null;
        formRole = "USER";
        editUserId = null;
    }

    // ── Score formatting helpers ──────────────────────────────────────────────

    public String getPlatformAvgScoreFormatted() {
        return platformAvgScore > 0 ? String.format("%.1f", platformAvgScore) : "—";
    }

    public String formatScore(float score, SessionStatus status) {
        return status == SessionStatus.COMPLETED
                ? String.format("%.1f", score)
                : "—";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public List<InterviewSession> getAllSessions() { return allSessions; }
    public List<User>  getAllUsers()               { return allUsers; }
    public long getCompletedCount()               { return completedCount; }
    public long getInProgressCount()              { return inProgressCount; }
    public long getAbandonedCount()               { return abandonedCount; }
    public double getPlatformAvgScore()           { return platformAvgScore; }
    public String getCurrentUserId()              { return currentUserId; }
    public String getErrorMessage()               { return errorMessage; }
    public String getSuccessMessage()             { return successMessage; }
    public String getSortField()                  { return sortField; }
    public boolean isSortAscending()              { return sortAscending; }

    public boolean isShowCreateForm()             { return showCreateForm; }
    public boolean isShowEditForm()               { return showEditForm; }
    public String  getEditUserId()                { return editUserId; }

    public String getFormName()                   { return formName; }
    public void   setFormName(String v)           { formName = v; }
    public String getFormEmail()                  { return formEmail; }
    public void   setFormEmail(String v)          { formEmail = v; }
    public String getFormPassword()               { return formPassword; }
    public void   setFormPassword(String v)       { formPassword = v; }
    public String getFormRole()                   { return formRole; }
    public void   setFormRole(String v)           { formRole = v; }
    public String getFormTargetPosition()         { return formTargetPosition; }
    public void   setFormTargetPosition(String v) { formTargetPosition = v; }
}
