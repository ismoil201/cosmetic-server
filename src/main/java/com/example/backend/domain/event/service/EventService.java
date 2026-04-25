package com.example.backend.domain.event.service;

import com.example.backend.domain.event.entity.EventLog;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.event.repository.EventLogRepository;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.recommendation.service.InterestService;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventLogRepository eventRepo;
    private final UserService userService;
    private final ProductRepository productRepo;
    private final InterestService interestService;

    @Transactional
    public void log(EventType type, Long productId, String screen, Integer position, String queryText, String sessionId) {

        // ✅ VALIDATION: Ensure required fields are present per event type
        validateEventRequest(type, productId, queryText);

        User user = userService.getCurrentUserOrNull();
        Product p = null;
        if (productId != null) {
            p = productRepo.findById(productId).orElse(null);

            // ⚠️ WARNING: If productId provided but product not found, log warning but continue
            // This prevents Android crashes if product was deleted after being displayed
            if (p == null) {
                // Log warning but don't throw - graceful degradation
                System.err.println("⚠️ Event logged for non-existent productId: " + productId);
            }
        }

        EventLog e = new EventLog();
        e.setUser(user);
        e.setSessionId(sessionId);
        e.setEventType(type);
        e.setProduct(p);
        e.setScreen(screen);
        e.setPosition(position);
        e.setQueryText(queryText);

        // ✅ CRITICAL: Auto-populate category and brand from Product when available
        // This ensures analytics/recommendations work even if Android doesn't send these fields
        if (p != null) {
            e.setCategory(p.getCategory().name());
            e.setBrand(p.getBrand());
        }

        eventRepo.save(e);

        // ✅ STEP 8: Update user interests based on event type
        // Only update interests for authenticated users
        if (user != null) {
            updateUserInterests(user, type, p, queryText);
        }
    }

    /**
     * Update user interest profile based on event
     *
     * STEP 8: Personalization engine
     * - Product-based events → update CATEGORY and BRAND interests
     * - SEARCH events → update QUERY interests
     */
    private void updateUserInterests(User user, EventType type, Product product, String queryText) {
        switch (type) {
            case IMPRESSION -> {
                if (product != null) {
                    interestService.onImpression(user, product);
                }
            }
            case VIEW -> {
                if (product != null) {
                    interestService.onView(user, product);
                }
            }
            case CLICK -> {
                if (product != null) {
                    interestService.onClick(user, product);
                }
            }
            case FAVORITE_ADD -> {
                if (product != null) {
                    interestService.onFavorite(user, product, true);
                }
            }
            case FAVORITE_REMOVE -> {
                if (product != null) {
                    interestService.onFavorite(user, product, false);
                }
            }
            case ADD_TO_CART -> {
                if (product != null) {
                    interestService.onAddToCart(user, product);
                }
            }
            case PURCHASE -> {
                if (product != null) {
                    interestService.onPurchase(user, product);
                }
            }
            case SEARCH -> {
                // ✅ STEP 8: Track search queries as user interests
                if (queryText != null && !queryText.isBlank()) {
                    interestService.onSearch(user, queryText);
                }
            }
        }
    }

    /**
     * Validates event request based on event type requirements
     *
     * Business rules:
     * - VIEW/CLICK/IMPRESSION events require productId (analytics need product context)
     * - SEARCH events require queryText (need to know what user searched for)
     * - Other event types (FAVORITE_ADD, ADD_TO_CART, PURCHASE) are flexible
     *
     * @throws BadRequestException if validation fails
     */
    private void validateEventRequest(EventType type, Long productId, String queryText) {
        switch (type) {
            case VIEW, CLICK, IMPRESSION -> {
                if (productId == null) {
                    throw new BadRequestException(
                            String.format("%s event requires productId", type.name())
                    );
                }
            }
            case SEARCH -> {
                if (queryText == null || queryText.isBlank()) {
                    throw new BadRequestException("SEARCH event requires queryText");
                }
            }
            // Other event types (FAVORITE_ADD, FAVORITE_REMOVE, ADD_TO_CART, PURCHASE)
            // have flexible validation - they're usually triggered by user actions
            // that already have context
        }
    }
}
