package com.example.backend.global.exception;

/**
 * Thrown when rate limit is exceeded
 *
 * HTTP Response: 429 TOO_MANY_REQUESTS
 * Error Code: RATE_LIMIT_EXCEEDED
 *
 * Example:
 * throw new RateLimitExceededException("Too many requests. Please try again later.");
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
