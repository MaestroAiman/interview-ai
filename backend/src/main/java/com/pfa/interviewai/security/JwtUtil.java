package com.pfa.interviewai.security;

import com.pfa.interviewai.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.SecretKey;
import java.util.Date;

@ApplicationScoped
public class JwtUtil {

    @Inject
    private AppConfig appConfig;

    public String generateToken(String email, String userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(appConfig.getJwtSecret()));
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(
                    System.currentTimeMillis() + appConfig.getJwtExpirationMs()))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return getClaims(token).get("userId", String.class);
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try { getClaims(token); return true; }
        catch (Exception e) { return false; }
    }

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(appConfig.getJwtSecret()));
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
