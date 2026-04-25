package com.example.backend.domain.recommendation.service;

import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.recommendation.entity.InterestType;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.recommendation.entity.UserInterest;
import com.example.backend.domain.recommendation.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService {

    // ✅ STEP 8: Event weight configuration (as per requirements)
    private static final double WEIGHT_IMPRESSION = 0.1;
    private static final double WEIGHT_CLICK = 1.0;
    private static final double WEIGHT_VIEW = 2.0;
    private static final double WEIGHT_FAVORITE = 4.0;
    private static final double WEIGHT_ADD_CART = 6.0;
    private static final double WEIGHT_ORDER = 10.0;
    private static final double WEIGHT_SEARCH = 2.0;

    // Decay factor: older interests lose strength over time (0.98 = 2% decay per event)
    private static final double DECAY = 0.98;

    private final UserInterestRepository interestRepo;

    // =====================================================
    // Event handlers - update interests based on user actions
    // =====================================================

    public void onImpression(User user, Product p) {
        bumpWithEvent(user, InterestType.CATEGORY, p.getCategory().name(), WEIGHT_IMPRESSION, EventType.IMPRESSION);
        bumpBrandWithEvent(user, p, WEIGHT_IMPRESSION, EventType.IMPRESSION);
    }

    public void onView(User user, Product p) {
        bumpWithEvent(user, InterestType.CATEGORY, p.getCategory().name(), WEIGHT_VIEW, EventType.VIEW);
        bumpBrandWithEvent(user, p, WEIGHT_VIEW, EventType.VIEW);
    }

    public void onClick(User user, Product p) {
        bumpWithEvent(user, InterestType.CATEGORY, p.getCategory().name(), WEIGHT_CLICK, EventType.CLICK);
        bumpBrandWithEvent(user, p, WEIGHT_CLICK, EventType.CLICK);
    }

    public void onFavorite(User user, Product p, boolean added) {
        double weight = added ? WEIGHT_FAVORITE : -WEIGHT_FAVORITE * 0.5;
        EventType eventType = added ? EventType.FAVORITE_ADD : EventType.FAVORITE_REMOVE;
        bumpWithEvent(user, InterestType.CATEGORY, p.getCategory().name(), weight, eventType);
        bumpBrandWithEvent(user, p, weight, eventType);
    }

    public void onAddToCart(User user, Product p) {
        bumpWithEvent(user, InterestType.CATEGORY, p.getCategory().name(), WEIGHT_ADD_CART, EventType.ADD_TO_CART);
        bumpBrandWithEvent(user, p, WEIGHT_ADD_CART, EventType.ADD_TO_CART);
    }

    public void onPurchase(User user, Product p) {
        bumpWithEvent(user, InterestType.CATEGORY, p.getCategory().name(), WEIGHT_ORDER, EventType.PURCHASE);
        bumpBrandWithEvent(user, p, WEIGHT_ORDER, EventType.PURCHASE);
    }

    /**
     * Track search query as user interest
     * @param user authenticated user
     * @param queryText search query (will be normalized: trimmed and lowercased)
     */
    public void onSearch(User user, String queryText) {
        if (queryText == null || queryText.isBlank()) return;
        String normalized = queryText.trim().toLowerCase();
        bumpWithEvent(user, InterestType.QUERY, normalized, WEIGHT_SEARCH, EventType.SEARCH);
    }

    // =====================================================
    // Helper methods
    // =====================================================

    private void bumpBrandWithEvent(User user, Product p, double delta, EventType eventType) {
        if (p.getBrand() == null || p.getBrand().isBlank()) return;
        // Brand keys are stored in lowercase for consistency
        bumpWithEvent(user, InterestType.BRAND, p.getBrand().trim().toLowerCase(), delta, eventType);
    }

    /**
     * Update user interest score with event tracking
     *
     * @param user authenticated user
     * @param type interest type (CATEGORY, BRAND, QUERY)
     * @param key interest key (category name, brand name, search query)
     * @param delta score change (positive for engagement, negative for negative feedback)
     * @param eventType the event that triggered this interest update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void bumpWithEvent(User user, InterestType type, String key, double delta, EventType eventType) {
        try {
            if (key == null || key.isBlank()) return;
            String k = key.trim();

            UserInterest row = interestRepo.findByUserAndTypeAndKey(user, type, k)
                    .orElseGet(() -> {
                        UserInterest ui = new UserInterest();
                        ui.setUser(user);
                        ui.setType(type);
                        ui.setKey(k);
                        ui.setScore(0);
                        ui.setCreatedAt(LocalDateTime.now());
                        ui.setUpdatedAt(LocalDateTime.now());
                        return ui;
                    });

            // ✅ STEP 8: Score update with decay
            // New score = (old score × decay factor) + event weight
            row.setScore(row.getScore() * DECAY + delta);
            row.setLastEventType(eventType != null ? eventType.name() : null);
            row.setUpdatedAt(LocalDateTime.now());
            interestRepo.save(row);

        } catch (Exception e) {
            // ⚠️ CRITICAL: Do not crash event logging if interest update fails
            // This ensures Android compatibility and graceful degradation
            log.warn("Failed to update user interest: user={}, type={}, key={}, error={}",
                    user.getId(), type, key, e.getMessage());
        }
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use bumpWithEvent instead to track event types
     */
    @Deprecated
    public void bump(User user, InterestType type, String key, double delta) {
        bumpWithEvent(user, type, key, delta, null);
    }

    // =====================================================
    // TODO: Future decay support
    // =====================================================
    // Future enhancement: Scheduled job to apply time-based decay
    // e.g., every week, multiply all scores by 0.95
    // This prevents stale interests from dominating recommendations
    //
    // @Scheduled(cron = "0 0 2 * * SUN") // Every Sunday at 2 AM
    // public void applyTimeBasedDecay() {
    //     // Update all interests: score = score * 0.95
    // }
}