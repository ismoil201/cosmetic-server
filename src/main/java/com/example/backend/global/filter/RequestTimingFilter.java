package com.example.backend.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ✅ PRODUCTION: Global Request Performance Monitoring Filter
 *
 * Measures total request time for ALL endpoints.
 * Logs only slow requests to avoid production log spam.
 *
 * Logging Levels:
 * - WARN: totalMs > 1000ms (very slow, requires immediate attention)
 * - INFO: totalMs > 300ms (slow, monitor for trends)
 * - DEBUG: totalMs <= 300ms (normal, not logged in production)
 *
 * Security:
 * - Logs method, path, status, timing, userId (if authenticated)
 * - Does NOT log: tokens, passwords, request bodies, email, names
 *
 * Order: Highest priority (Ordered.HIGHEST_PRECEDENCE)
 * - Runs first to measure complete request time
 * - Includes auth filter, rate limit, business logic, response serialization
 *
 * Performance: O(1), no DB/Redis calls, minimal overhead (~1-2ms)
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTimingFilter extends OncePerRequestFilter {

    /**
     * Slow request threshold (ms) - log INFO for monitoring
     */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 300;

    /**
     * Very slow request threshold (ms) - log WARN for investigation
     */
    private static final long VERY_SLOW_REQUEST_THRESHOLD_MS = 1000;

    /**
     * Feature flag: Enable/disable performance profiling
     * Set to false in production if log volume too high
     */
    @Value("${performance.profiling-enabled:true}")
    private boolean profilingEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip profiling if disabled (emergency kill switch)
        if (!profilingEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip profiling for actuator/health endpoints (reduce noise)
        String path = request.getRequestURI();
        if (shouldSkipProfiling(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        int status = 200;

        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();

        } catch (Exception e) {
            status = 500;
            throw e;

        } finally {
            long totalMs = System.currentTimeMillis() - startTime;

            // Log performance based on severity
            logRequestPerformance(request, status, totalMs);
        }
    }

    /**
     * Skip profiling for non-business endpoints to reduce log noise
     */
    private boolean shouldSkipProfiling(String path) {
        return path != null && (
                path.startsWith("/actuator") ||
                path.startsWith("/health") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/favicon.ico") ||
                path.equals("/error")
        );
    }

    /**
     * ✅ PRODUCTION: Production-safe performance logging
     *
     * Tiered logging based on performance:
     * - WARN: totalMs > 1000ms (very slow, requires investigation)
     * - INFO: totalMs > 300ms (slow, monitor for patterns)
     * - DEBUG: totalMs <= 300ms (normal, hidden in production)
     *
     * Security:
     * - Logs userId from request attribute (set by JwtFilter if authenticated)
     * - Does NOT log: Authorization header, tokens, request bodies, PII
     *
     * @param request HTTP request
     * @param status HTTP response status
     * @param totalMs Total request time in milliseconds
     */
    private void logRequestPerformance(HttpServletRequest request, int status, long totalMs) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // Extract userId safely (set by JwtFilter if authenticated)
        // Do NOT log tokens or sensitive headers
        String userId = extractUserId(request);

        String perfMsg = String.format(
                "[request] method=%s path=%s status=%d totalMs=%d%s",
                method, path, status, totalMs,
                userId != null ? " userId=" + userId : ""
        );

        // ✅ Tiered logging based on performance
        if (totalMs > VERY_SLOW_REQUEST_THRESHOLD_MS) {
            // Very slow request - WARN level for immediate attention
            log.warn("[PERF][VERY_SLOW] {}", perfMsg);

            // Log additional diagnostic info for very slow requests
            if (totalMs > 3000) {
                log.warn("[PERF] CRITICAL SLOWNESS: Request took {}ms - possible timeout risk", totalMs);
            }

        } else if (totalMs > SLOW_REQUEST_THRESHOLD_MS) {
            // Slow request - INFO level for monitoring trends
            log.info("[PERF] {}", perfMsg);

        } else {
            // Normal request - DEBUG level (disabled in production by default)
            log.debug("[PERF] {}", perfMsg);
        }
    }

    /**
     * Extract userId from request attributes (set by authentication filter)
     *
     * Security: Returns only userId (Long), NOT user email/name/token
     *
     * @param request HTTP request
     * @return userId as String, or null if not authenticated
     */
    private String extractUserId(HttpServletRequest request) {
        try {
            // JwtFilter typically sets authenticated user in request attribute
            Object userAttr = request.getAttribute("userId");
            if (userAttr != null) {
                return userAttr.toString();
            }

            // Alternative: Check Principal if using Spring Security
            if (request.getUserPrincipal() != null) {
                String principal = request.getUserPrincipal().getName();
                // If principal is userId (not email), return it
                // Otherwise return "authenticated" to avoid logging email
                try {
                    Long.parseLong(principal);
                    return principal;
                } catch (NumberFormatException e) {
                    return "authenticated";
                }
            }

            return null;
        } catch (Exception e) {
            // Never fail request due to logging error
            return null;
        }
    }
}
