package com.pfa.interviewai.metrics;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

// Forces eager initialization of MetricsRegistry (CDI @ApplicationScoped beans are lazy by default).
// Weld's own listener fires before this one, so CDI injection is active here.
@WebListener
public class MetricsServletContextListener implements ServletContextListener {

    @Inject
    private MetricsRegistry metricsRegistry;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        metricsRegistry.getRequestsTotal(); // triggers @PostConstruct
    }
}
