package com.example.backend.global.security;

import com.example.backend.global.exception.RateLimitExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter using Bucket4j (in-memory)
 *
 * IMPORTANT: This uses in-memory buckets for simplicity and safety.
 * For distributed deployment (multiple instances), consider Redis-backed buckets.
 *
 * Rate Limits (marketplace-friendly):
 * - AUTH: Login (10/15min), Register (5/hour), Firebase (30/15min)
 * - PUBLIC: Home/Products (300/min), Search (120/min)
 * - AUTHENTICATED: Uploads (30/min), Events (3000/min), Cart/Orders/Favorites (30-120/min)
 * - SELLER/ADMIN: Standard limits per role
 *
 * Exemptions:
 * - /actuator/health
 * - /actuator/prometheus
 * - /v3/api-docs/**
 * - /swagger-ui/**
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // In-memory bucket storage (simple and safe for single-instance deployment)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for exempted endpoints
        if (isExempted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine rate limit key (userId if authenticated, else IP)
        String key = getRateLimitKey(request, path);

        // Get or create bucket for this key+path
        Bucket bucket = resolveBucket(key, path, method);

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            // Rate limit OK - proceed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded - throw exception (handled by GlobalExceptionHandler)
            log.warn("Rate limit exceeded: {} {} | Key: {}", method, path, key);
            throw new RateLimitExceededException("Too many requests. Please try again later.");
        }
    }

    /**
     * Check if endpoint is exempted from rate limiting
     */
    private boolean isExempted(String path) {
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/prometheus") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/error");
    }

    /**
     * Determine rate limit key (userId if authenticated, else IP address)
     */
    private String getRateLimitKey(HttpServletRequest request, String path) {
        // For authenticated endpoints, use userId
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName() + ":" + normalizePath(path);
        }

        // For public endpoints, use IP address
        String ip = getClientIP(request);
        return "ip:" + ip + ":" + normalizePath(path);
    }

    /**
     * Normalize path for rate limiting (group similar paths)
     */
    private String normalizePath(String path) {
        // Auth endpoints
        if (path.startsWith("/api/auth/login")) return "auth:login";
        if (path.startsWith("/api/auth/register")) return "auth:register";
        if (path.startsWith("/api/auth/firebase")) return "auth:firebase";

        // Public browsing
        if (path.startsWith("/api/home")) return "home";
        if (path.startsWith("/api/products/search")) return "products:search";
        if (path.startsWith("/api/products")) return "products";
        if (path.startsWith("/api/reviews/product")) return "reviews";
        if (path.startsWith("/api/banners")) return "banners";

        // Authenticated user
        if (path.startsWith("/api/uploads")) return "uploads";
        if (path.startsWith("/api/events")) return "events";
        if (path.startsWith("/api/cart")) return "cart";
        if (path.startsWith("/api/orders")) return "orders";
        if (path.startsWith("/api/favorites")) return "favorites";
        if (path.startsWith("/api/notifications")) return "notifications";

        // Seller
        if (path.startsWith("/api/seller/products")) return "seller:products";
        if (path.startsWith("/api/seller/orders")) return "seller:orders";
        if (path.startsWith("/api/seller")) return "seller";

        // Admin
        if (path.startsWith("/api/admin")) return "admin";

        // Default
        return "other";
    }

    /**
     * Get or create bucket with appropriate limits
     */
    private Bucket resolveBucket(String key, String path, String method) {
        return buckets.computeIfAbsent(key, k -> createBucket(path, method));
    }

    /**
     * Create bucket with rate limits based on endpoint
     */
    private Bucket createBucket(String path, String method) {
        Bandwidth limit;

        // AUTH LIMITS (by IP)
        if (path.startsWith("/api/auth/login") && "POST".equals(method)) {
            // 10 login attempts / 15 minutes
            limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(15)));
        } else if (path.startsWith("/api/auth/register") && "POST".equals(method)) {
            // 5 registrations / 1 hour
            limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1)));
        } else if (path.startsWith("/api/auth/firebase") && "POST".equals(method)) {
            // 30 firebase auth / 15 minutes
            limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(15)));
        }

        // PUBLIC BROWSING LIMITS (by IP)
        else if (path.startsWith("/api/home")) {
            // 300 requests / minute
            limit = Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/products/search")) {
            // 120 searches / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/products")) {
            // 300 product views / minute
            limit = Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/reviews/product")) {
            // 120 review fetches / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/banners")) {
            // 300 banner requests / minute
            limit = Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1)));
        }

        // AUTHENTICATED USER LIMITS (by userId)
        else if (path.startsWith("/api/uploads")) {
            // 30 upload requests / minute
            limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/events")) {
            // 3000 event tracking / minute (high limit for analytics)
            limit = Bandwidth.classic(3000, Refill.intervally(3000, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/cart")) {
            // 120 cart operations / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/orders")) {
            // 30 order operations / minute
            limit = Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/favorites")) {
            // 120 favorite operations / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/notifications")) {
            // 120 notification requests / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        }

        // SELLER LIMITS (by userId)
        else if (path.startsWith("/api/seller/products")) {
            // 120 seller product operations / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/seller/orders")) {
            // 120 seller order operations / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        } else if (path.startsWith("/api/seller")) {
            // 120 other seller operations / minute
            limit = Bandwidth.classic(120, Refill.intervally(120, Duration.ofMinutes(1)));
        }

        // ADMIN LIMITS (by userId)
        else if (path.startsWith("/api/admin")) {
            // 300 admin operations / minute
            limit = Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1)));
        }

        // DEFAULT LIMIT (conservative for unknown endpoints)
        else {
            // 60 requests / minute
            limit = Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1)));
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Extract client IP address (handles X-Forwarded-For for proxies)
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }
}
