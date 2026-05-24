package com.pfa.interviewai.rest.dto;

public class HealthStatusDto {
    private String status;
    private String message;
    private long latencyMs;
    private String model;

    public HealthStatusDto() {}

    public HealthStatusDto(String status, String message, long latencyMs, String model) {
        this.status = status;
        this.message = message;
        this.latencyMs = latencyMs;
        this.model = model;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
