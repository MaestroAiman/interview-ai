package com.pfa.interviewai.rest;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api")
public class JaxRsActivator extends ResourceConfig {

    public JaxRsActivator() {
        packages("com.pfa.interviewai.rest");
        register(MultiPartFeature.class);
    }
}
