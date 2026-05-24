package com.pfa.interviewai.rest.dto;

public class SubmitAnswerRequest {
    private String sessionId;
    private String questionId;
    private String answerText;

    public String getSessionId()  { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String v) { this.questionId = v; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String v) { this.answerText = v; }
}
