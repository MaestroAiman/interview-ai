package com.pfa.interviewai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Answer {
    private String id;
    private String sessionId;
    private String questionId;
    private String text;
    private String submittedAt;
}
