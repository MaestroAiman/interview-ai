package com.pfa.interviewai.rest;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Exposes Prometheus metrics at GET /api/metrics.
 * Scraped by Prometheus at http://app:8080/interview-ai/api/metrics
 */
@Path("/metrics")
@RequestScoped
public class MetricsRestController {

    @GET
    public Response metrics() throws IOException {
        StringWriter writer = new StringWriter();
        TextFormat.write004(writer,
            CollectorRegistry.defaultRegistry.metricFamilySamples());
        return Response.ok(writer.toString())
            .header("Content-Type", TextFormat.CONTENT_TYPE_004)
            .build();
    }
}
