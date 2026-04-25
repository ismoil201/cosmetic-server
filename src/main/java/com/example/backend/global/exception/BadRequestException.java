package com.example.backend.global.exception;

/**
 * Thrown when client request is invalid but not a validation error
 *
 * Use cases:
 * - Invalid request format (not caught by @Valid)
 * - Missing required query parameters
 * - Malformed request body
 * - Invalid business operation request
 *
 * HTTP Response: 400 BAD_REQUEST
 * Error Code: BAD_REQUEST
 *
 * Example:
 * throw new BadRequestException("Cart is empty, cannot create order");
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
