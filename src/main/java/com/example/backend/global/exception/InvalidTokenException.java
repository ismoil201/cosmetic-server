package com.example.backend.global.exception;

/**
 * Thrown when JWT token is invalid, expired, or malformed
 *
 * Use cases:
 * - Expired JWT token
 * - Invalid JWT signature
 * - Malformed token format
 * - Token for different application
 *
 * HTTP Response: 401 UNAUTHORIZED
 * Error Code: INVALID_TOKEN
 *
 * Example:
 * throw new InvalidTokenException("JWT token has expired");
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
