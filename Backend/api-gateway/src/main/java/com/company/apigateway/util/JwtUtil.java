package com.company.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // ─── Validate Token ────────────────────────────────

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            // If no exception thrown → token is valid
            return true;
        } catch (Exception e) {
            // Expired, malformed, wrong signature → invalid
            return false;
        }
    }

    // ─── Extract Claims ────────────────────────────────

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token)
                .get("role", String.class);
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token)
                .get("userId", Long.class);
    }

    // ─── Private Helpers ───────────────────────────────

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}