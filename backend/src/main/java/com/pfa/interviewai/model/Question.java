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
public class Question {
    private String id;
    private String sessionId;
    private String content;
    private int order;
    private String category;
    private String aiDifficulty;
    private int estimatedDurationSeconds;
    @Builder.Default
    private List<String> expectedKeywords = new ArrayList<>();
}
