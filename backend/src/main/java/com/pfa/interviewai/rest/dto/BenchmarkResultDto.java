package com.pfa.interviewai.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BenchmarkResultDto {

    private String operation;
    private String prompt;
    private ProviderResult claude;
    private ProviderResult ollama;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProviderResult {
        private String provider;
        private long   latencyMs;
        private String response;  // truncated to 1000 chars
        private String status;    // "success" or "error"
        private String error;     // null on success
    }
}
