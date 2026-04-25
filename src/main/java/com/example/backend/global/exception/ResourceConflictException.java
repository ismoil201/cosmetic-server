package com.example.backend.global.exception;

/**
 * Thrown when a resource conflict occurs
 *
 * Use cases:
 * - Duplicate resource creation (e.g., email already exists)
 * - State conflict (e.g., order already confirmed, cannot modify)
 * - Business rule violation requiring conflict resolution
 *
 * HTTP Response: 409 CONFLICT
 * Error Code: RESOURCE_CONFLICT
 *
 * Example:
 * throw new ResourceConflictException("Email already registered");
 */
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
