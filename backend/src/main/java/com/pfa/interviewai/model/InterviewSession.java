package com.pfa.interviewai.model;

import com.pfa.interviewai.model.enums.Difficulty;
import com.pfa.interviewai.model.enums.InterviewType;
import com.pfa.interviewai.model.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {
    private String id;
    private String userId;
    private InterviewType type;
    private String position;
    private Difficulty difficulty;
    private SessionStatus status;
    private int questionCount;
    private float overallScore;
    private float relevanceAvg;
    private float clarityAvg;
    private float sentimentAvg;
    private String startedAt;
    private String endedAt;

    // --- session summary fields (populated at session completion) ---
    private String globalAssessment;
    @Builder.Default
    private List<String> topStrengths = new ArrayList<>();
    @Builder.Default
    private List<String> priorityImprovements = new ArrayList<>();
    @Builder.Default
    private List<String> recommendedResources = new ArrayList<>();
    private String readinessLevel;
}
