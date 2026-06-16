package com.pfa.interviewai.rest.dto;

import java.util.List;

public class AdminDashboardDto {
    private int totalUsers;
    private long completedCount;
    private long inProgressCount;
    private float platformAvgScore;
    private List<SessionRowDto> sessions;

    public AdminDashboardDto() {}

    public AdminDashboardDto(int totalUsers, long completedCount,
                             long inProgressCount, float platformAvgScore,
                             List<SessionRowDto> sessions) {
        this.totalUsers = totalUsers;
        this.completedCount = completedCount;
        this.inProgressCount = inProgressCount;
        this.platformAvgScore = platformAvgScore;
        this.sessions = sessions;
    }

    public int getTotalUsers()              { return totalUsers; }
    public void setTotalUsers(int v)        { this.totalUsers = v; }
    public long getCompletedCount()         { return completedCount; }
    public void setCompletedCount(long v)   { this.completedCount = v; }
    public long getInProgressCount()        { return inProgressCount; }
    public void setInProgressCount(long v)  { this.inProgressCount = v; }
    public float getPlatformAvgScore()      { return platformAvgScore; }
    public void setPlatformAvgScore(float v){ this.platformAvgScore = v; }
    public List<SessionRowDto> getSessions(){ return sessions; }
    public void setSessions(List<SessionRowDto> v){ this.sessions = v; }

    public static class SessionRowDto {
        private String id;
        private String userId;
        private String position;
        private String type;
        private String difficulty;
        private float overallScore;
        private String status;
        private String startedAt;

        public SessionRowDto() {}

        public SessionRowDto(String id, String userId, String position,
                             String type, String difficulty,
                             float overallScore, String status, String startedAt) {
            this.id = id;
            this.userId = userId;
            this.position = position;
            this.type = type;
            this.difficulty = difficulty;
            this.overallScore = overallScore;
            this.status = status;
            this.startedAt = startedAt;
        }

        public String getId()           { return id; }
        public void setId(String v)     { this.id = v; }
        public String getUserId()       { return userId; }
        public void setUserId(String v) { this.userId = v; }
        public String getPosition()         { return position; }
        public void setPosition(String v)   { this.position = v; }
        public String getType()             { return type; }
        public void setType(String v)       { this.type = v; }
        public String getDifficulty()       { return difficulty; }
        public void setDifficulty(String v) { this.difficulty = v; }
        public float getOverallScore()          { return overallScore; }
        public void setOverallScore(float v)    { this.overallScore = v; }
        public String getStatus()           { return status; }
        public void setStatus(String v)     { this.status = v; }
        public String getStartedAt()        { return startedAt; }
        public void setStartedAt(String v)  { this.startedAt = v; }
    }
}
