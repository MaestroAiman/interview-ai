package com.pfa.interviewai.model;

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
public class Feedback {
    // --- existing fields (kept for JSF backward compatibility) ---
    private String id;
    private String sessionId;
    private String questionId;
    private String answerText;
    private float relevanceScore;
    private float clarityScore;
    private float sentimentScore;
    private float overallScore;
    private String strengths;
    private String improvements;
    private String suggestedAnswer;
    private String shortComment;

    // --- new dimension scores (0–100 scale) ---
    private float depthScore;
    private float vocabularyScore;
    private float examplesScore;
    private float globalScore;

    // --- new analysis fields ---
    private String levelAssessment;
    @Builder.Default
    private List<String> keyStrengths = new ArrayList<>();
    @Builder.Default
    private List<String> criticalGaps = new ArrayList<>();

    // --- new feedback fields ---
    private String positivePoints;
    private String improvementPoints;
    private String concreteAdvice;
    private String exampleAnswer;
    private String nextDifficulty;
}
