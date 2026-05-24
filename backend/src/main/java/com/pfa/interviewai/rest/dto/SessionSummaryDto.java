package com.pfa.interviewai.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class SessionSummaryDto {
    private float overallScore;
    private String globalAssessment;
    private List<String> topStrengths = new ArrayList<>();
    private List<String> priorityImprovements = new ArrayList<>();
    private List<String> recommendedResources = new ArrayList<>();
    private String readinessLevel;

    public SessionSummaryDto() {}

    public float getOverallScore() { return overallScore; }
    public void setOverallScore(float overallScore) { this.overallScore = overallScore; }

    public String getGlobalAssessment() { return globalAssessment; }
    public void setGlobalAssessment(String globalAssessment) { this.globalAssessment = globalAssessment; }

    public List<String> getTopStrengths() { return topStrengths; }
    public void setTopStrengths(List<String> topStrengths) { this.topStrengths = topStrengths; }

    public List<String> getPriorityImprovements() { return priorityImprovements; }
    public void setPriorityImprovements(List<String> priorityImprovements) { this.priorityImprovements = priorityImprovements; }

    public List<String> getRecommendedResources() { return recommendedResources; }
    public void setRecommendedResources(List<String> recommendedResources) { this.recommendedResources = recommendedResources; }

    public String getReadinessLevel() { return readinessLevel; }
    public void setReadinessLevel(String readinessLevel) { this.readinessLevel = readinessLevel; }
}
