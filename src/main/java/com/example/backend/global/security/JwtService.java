package com.example.backend.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Service for token generation and validation
 *
 * PRODUCTION SECURITY:
 * - JWT secret loaded from environment variable
 * - Secret must be 256+ bits for HS256 algorithm
 * - Token expiration: 24 hours
 * - Role-based claims for authorization
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long tokenValidityMs = 86400000; // 24 hours

    /**
     * Constructor with environment-based secret injection
     *
     * @param jwtSecret JWT secret from environment (JWT_SECRET env var)
     * @throws IllegalArgumentException if secret is too short (< 256 bits)
     */
    public JwtService(@Value("${app.jwt.secret}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must be configured via app.jwt.secret property");
        }

        // SECURITY: Enforce minimum secret length (256 bits = 32 bytes)
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 characters)");
        }

        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * Generate JWT token with email and role claims
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + tokenValidityMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extract email from JWT token
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extract role from JWT token
     */
    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}
