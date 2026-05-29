package com.pfa.interviewai.rest;

abstract class BaseRestController {

    protected String resolveToken(String cookie, String header) {
        if (cookie != null && !cookie.isBlank()) return cookie;
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }
}
