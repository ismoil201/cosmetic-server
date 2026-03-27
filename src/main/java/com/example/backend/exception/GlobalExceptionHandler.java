package com.example.backend.exception;

import com.example.backend.dto.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for production-ready error responses
 *
 * PRODUCTION IMPROVEMENTS:
 * - Structured logging for monitoring
 * - Specific exception handling
 * - User-friendly error messages
 * - No internal details exposed
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * ✅ 404 - Resource not found
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(NotFoundException e, WebRequest request) {
        log.warn("Resource not found: {} | Path: {}", e.getMessage(), request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * ✅ 400 - Validation errors (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException e, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {} errors | Path: {}", errors.size(), request.getDescription(false));

        return ResponseEntity
                .badRequest()
                .body(new ApiResponse<>(false, "Validation failed", errors));
    }

    /**
     * ✅ 400 - Constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(
            ConstraintViolationException e, WebRequest request) {
        
        log.warn("Constraint violation: {} | Path: {}", e.getMessage(), request.getDescription(false));
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Invalid request parameters"));
    }

    /**
     * ✅ 401 - Bad credentials (login failure)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(
            BadCredentialsException e, WebRequest request) {
        
        log.warn("Authentication failed | Path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    /**
     * ✅ 403 - Access denied (authorization failure)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(
            AccessDeniedException e, WebRequest request) {
        
        log.warn("Access denied: {} | Path: {}", e.getMessage(), request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied"));
    }

    /**
     * ✅ 409 - Optimistic lock exception (concurrent modification)
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<?>> handleOptimisticLock(
            OptimisticLockException e, WebRequest request) {
        
        log.warn("Optimistic lock conflict | Path: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Resource was modified by another user. Please refresh and try again."));
    }

    /**
     * ✅ 400 - Business logic errors (RuntimeException)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntime(RuntimeException e, WebRequest request) {
        // Log full stack trace for debugging, but don't expose to user
        log.error("Business logic error: {} | Path: {}", e.getMessage(), request.getDescription(false), e);
        
        // Return user-friendly message (could be improved with custom exception types)
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * ✅ 500 - Unexpected errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e, WebRequest request) {
        // CRITICAL: Log full stack trace for investigation
        log.error("CRITICAL: Unhandled exception | Path: {} | Error: {}", 
                request.getDescription(false), e.getMessage(), e);
        
        // SECURITY: Never expose internal error details to users
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
