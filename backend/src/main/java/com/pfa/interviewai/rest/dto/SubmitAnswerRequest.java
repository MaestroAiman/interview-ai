package com.pfa.interviewai.rest.dto;

public class SubmitAnswerRequest {
    private String sessionId;
    private String questionId;
    private String answerText;

    public String getSessionId()                 { return sessionId; }
    public void setSessionId(String sessionId)   { this.sessionId = sessionId; }
    public String getQuestionId()                { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getAnswerText()                { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
}
