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
    private float depthScore;
    private float vocabularyScore;
    private float examplesScore;
    private float globalScore;
    private String levelAssessment;
    @Builder.Default
    private List<String> keyStrengths = new ArrayList<>();
    @Builder.Default
    private List<String> criticalGaps = new ArrayList<>();
    private String positivePoints;
    private String improvementPoints;
    private String concreteAdvice;
    private String exampleAnswer;
    private String nextDifficulty;

    public String getFormattedOverallScore()   { return String.format("%.1f", overallScore); }
    public String getFormattedRelevanceScore() { return String.format("%.1f", relevanceScore); }
    public String getFormattedClarityScore()   { return String.format("%.1f", clarityScore); }
    public String getFormattedSentimentScore() { return String.format("%.1f", sentimentScore); }
}
