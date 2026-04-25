package com.example.backend.global.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response format for Android/iOS/Web clients
 *
 * Provides consistent error structure:
 * - timestamp: When the error occurred
 * - status: HTTP status code
 * - code: Machine-readable error code (e.g., OUT_OF_STOCK, VALIDATION_ERROR)
 * - message: Human-readable error message
 * - path: Request path that caused the error
 *
 * Example JSON:
 * {
 *   "timestamp": "2024-01-15T10:30:45",
 *   "status": 409,
 *   "code": "OUT_OF_STOCK",
 *   "message": "Not enough stock: Moisturizer 50ml",
 *   "path": "/api/orders"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;

    private String code;

    private String message;

    private String path;

    /**
     * Factory method for creating error responses
     */
    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                status,
                code,
                message,
                extractPath(path)
        );
    }

    /**
     * Extract clean path from WebRequest description
     * Converts "uri=/api/orders" to "/api/orders"
     */
    private static String extractPath(String webRequestDescription) {
        if (webRequestDescription == null) return null;
        if (webRequestDescription.startsWith("uri=")) {
            return webRequestDescription.substring(4);
        }
        return webRequestDescription;
    }
}
