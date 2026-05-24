package com.pfa.interviewai.rest.dto;

public class StartSessionRequest {
    private String type;
    private String position;
    private String difficulty;
    private int questionCount;

    public StartSessionRequest() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
}
