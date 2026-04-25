package com.example.backend.global.exception;

import com.example.backend.global.response.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
 * - Specific exception handling with error codes
 * - Android/iOS/Web friendly ErrorResponse format
 * - User-friendly error messages
 * - No internal details exposed
 * - Consistent error codes for client-side handling
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== BUSINESS LOGIC EXCEPTIONS (4xx) ====================

    /**
     * ✅ 429 TOO_MANY_REQUESTS - Rate limit exceeded
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException e, WebRequest request) {

        log.warn("Rate limit exceeded: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "RATE_LIMIT_EXCEEDED",
                e.getMessage(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error);
    }

    /**
     * ✅ 409 CONFLICT - Insufficient stock for order
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException e, WebRequest request) {

        log.warn("Insufficient stock: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "OUT_OF_STOCK",
                e.getMessage(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    /**
     * ✅ 409 CONFLICT - Resource conflict
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(
            ResourceConflictException e, WebRequest request) {

        log.warn("Resource conflict: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "RESOURCE_CONFLICT",
                e.getMessage(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    /**
     * ✅ 400 BAD_REQUEST - Bad request exception
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException e, WebRequest request) {

        log.warn("Bad request: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                e.getMessage(),
                request.getDescription(false)
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    /**
     * ✅ 404 - Resource not found
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e, WebRequest request) {
        log.warn("Resource not found: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                e.getMessage(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // ==================== VALIDATION EXCEPTIONS ====================

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
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException e, WebRequest request) {

        log.warn("Constraint violation: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Invalid request parameters",
                request.getDescription(false)
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    // ==================== AUTHENTICATION & AUTHORIZATION ====================

    /**
     * ✅ 401 - Invalid token
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException e, WebRequest request) {

        log.warn("Invalid token: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_TOKEN",
                e.getMessage(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    /**
     * ✅ 401 - Bad credentials (login failure)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException e, WebRequest request) {

        log.warn("Authentication failed | Path: {}", request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Invalid email or password",
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    /**
     * ✅ 403 - Access denied (authorization failure)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException e, WebRequest request) {

        log.warn("Access denied: {} | Path: {}", e.getMessage(), request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Access denied",
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    // ==================== CONCURRENCY EXCEPTIONS ====================

    /**
     * ✅ 409 - Optimistic lock exception (JPA)
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException e, WebRequest request) {

        log.warn("Optimistic lock conflict | Path: {}", request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "CONCURRENT_MODIFICATION",
                "Resource was modified by another user. Please refresh and try again.",
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    /**
     * ✅ 409 - Optimistic lock exception (Spring)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleObjectOptimisticLock(
            ObjectOptimisticLockingFailureException e, WebRequest request) {

        log.warn("Optimistic lock conflict | Path: {}", request.getDescription(false));

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "CONCURRENT_MODIFICATION",
                "Resource was modified by another user. Please refresh and try again.",
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    // ==================== FALLBACK HANDLERS ====================

    /**
     * ✅ 400 - Business logic errors (RuntimeException fallback)
     * Catches RuntimeExceptions not handled by specific handlers above
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException e, WebRequest request) {
        // Log full stack trace for debugging, but don't expose to user
        log.error("Business logic error: {} | Path: {}", e.getMessage(), request.getDescription(false), e);

        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "BUSINESS_LOGIC_ERROR",
                e.getMessage() != null ? e.getMessage() : "An error occurred",
                request.getDescription(false)
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }

    /**
     * ✅ 500 - Unexpected errors (final safety net)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, WebRequest request) {
        // CRITICAL: Log full stack trace for investigation
        log.error("CRITICAL: Unhandled exception | Path: {} | Error: {}",
                request.getDescription(false), e.getMessage(), e);

        // SECURITY: Never expose internal error details to users
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
