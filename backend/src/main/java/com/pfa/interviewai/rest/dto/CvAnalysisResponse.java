package com.pfa.interviewai.rest.dto;

public class CvAnalysisResponse {

    private String type;
    private String position;
    private String difficulty;
    private String reasoning;

    public CvAnalysisResponse() {}

    public String getType()             { return type; }
    public void setType(String type)    { this.type = type; }

    public String getPosition()               { return position; }
    public void setPosition(String position)  { this.position = position; }

    public String getDifficulty()                  { return difficulty; }
    public void setDifficulty(String difficulty)   { this.difficulty = difficulty; }

    public String getReasoning()               { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}
