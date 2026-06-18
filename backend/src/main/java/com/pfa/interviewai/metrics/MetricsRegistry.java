package com.pfa.interviewai.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.DefaultExports;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.logging.Logger;

@ApplicationScoped
public class MetricsRegistry {

    private static final Logger log = Logger.getLogger(MetricsRegistry.class.getName());

    private static final double[] LATENCY_BUCKETS =
        {0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0, 60.0, 120.0};

    private static final double[] PROMPT_BUCKETS =
        {100, 250, 500, 1000, 2000, 4000, 8000, 12000};

    private Histogram requestDuration;
    private Counter   requestsTotal;
    private Histogram promptLength;

    @PostConstruct
    public void init() {
        DefaultExports.initialize();

        requestDuration = registerHistogram(
            "ai_request_duration_seconds",
            "AI provider HTTP call duration in seconds",
            LATENCY_BUCKETS,
            "provider", "operation");

        requestsTotal = registerCounter(
            "ai_requests_total",
            "Total AI provider HTTP calls",
            "provider", "operation", "status");

        promptLength = registerHistogram(
            "ai_prompt_length_chars",
            "Character length of the user prompt sent to AI",
            PROMPT_BUCKETS,
            "provider", "operation");

        log.info("MetricsRegistry initialized — Prometheus collectors registered.");
    }

    public Histogram getRequestDuration() { return requestDuration; }
    public Counter   getRequestsTotal()   { return requestsTotal;   }
    public Histogram getPromptLength()    { return promptLength;    }

    private Histogram registerHistogram(String name, String help,
                                        double[] buckets, String... labelNames) {
        try {
            return Histogram.build()
                .name(name).help(help)
                .labelNames(labelNames)
                .buckets(buckets)
                .register(CollectorRegistry.defaultRegistry);
        } catch (IllegalArgumentException e) {
            // Already registered — happens only on Cargo hot-redeploy (never in Docker).
            // Fall back to a private registry so the app keeps running; metrics won't be scraped.
            log.warning("Histogram already registered (hot-redeploy?): " + name);
            return Histogram.build()
                .name(name).help(help)
                .labelNames(labelNames)
                .buckets(buckets)
                .register(new CollectorRegistry());
        }
    }

    private Counter registerCounter(String name, String help, String... labelNames) {
        try {
            return Counter.build()
                .name(name).help(help)
                .labelNames(labelNames)
                .register(CollectorRegistry.defaultRegistry);
        } catch (IllegalArgumentException e) {
            log.warning("Counter already registered (hot-redeploy?): " + name);
            return Counter.build()
                .name(name).help(help)
                .labelNames(labelNames)
                .register(new CollectorRegistry());
        }
    }
}
