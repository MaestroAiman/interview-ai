package com.pfa.interviewai.rest.dto;

import com.pfa.interviewai.model.Question;

public class StartSessionResponse {
    private String sessionId;
    private Question firstQuestion;

    public StartSessionResponse() {}

    public StartSessionResponse(String sessionId, Question firstQuestion) {
        this.sessionId = sessionId;
        this.firstQuestion = firstQuestion;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public Question getFirstQuestion() { return firstQuestion; }
    public void setFirstQuestion(Question firstQuestion) { this.firstQuestion = firstQuestion; }
}
