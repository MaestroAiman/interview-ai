package com.pfa.interviewai.rest.dto;

public class HealthStatusDto {
    private String provider;
    private String status;
    private String message;
    private long latencyMs;
    private String model;
    private String url;

    public HealthStatusDto() {}

    public HealthStatusDto(String provider, String status, String message,
                           long latencyMs, String model, String url) {
        this.provider = provider;
        this.status = status;
        this.message = message;
        this.latencyMs = latencyMs;
        this.model = model;
        this.url = url;
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
