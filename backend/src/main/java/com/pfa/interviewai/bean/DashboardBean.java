package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.enums.InterviewType;
import com.pfa.interviewai.service.SessionService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class DashboardBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private AuthBean authBean;

    private List<InterviewSession> recentSessions = Collections.emptyList();
    private DashboardStats stats = new DashboardStats();

    public void init() {
        try {
            if (authBean.getCurrentUser() == null) return;
            List<InterviewSession> all = sessionService.findByUserId(
                authBean.getCurrentUser().getId());

            recentSessions = all.stream().limit(5).toList();

            stats.totalSessions = all.size();
            if (!all.isEmpty()) {
                double avg = all.stream()
                    .mapToDouble(InterviewSession::getOverallScore).average().orElse(0);
                stats.averageScore = (float) avg;
                stats.averageScoreFormatted = String.format("%.1f", avg);

                Map<InterviewType, Long> typeCounts = all.stream()
                    .filter(s -> s.getType() != null)
                    .collect(Collectors.groupingBy(InterviewSession::getType, Collectors.counting()));
                typeCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(e -> stats.bestType = e.getKey().getLabel());
            }
        } catch (Exception e) {
        }
    }

    public List<InterviewSession> getRecentSessions() { return recentSessions; }
    public DashboardStats getStats()                   { return stats; }

    public static class DashboardStats implements Serializable {
        int totalSessions;
        float averageScore;
        String averageScoreFormatted = "0.0";
        String bestType = "—";
        int streakDays;

        public int getTotalSessions()           { return totalSessions; }
        public float getAverageScore()          { return averageScore; }
        public String getAverageScoreFormatted(){ return averageScoreFormatted; }
        public String getBestType()             { return bestType; }
        public int getStreakDays()              { return streakDays; }
    }
}
