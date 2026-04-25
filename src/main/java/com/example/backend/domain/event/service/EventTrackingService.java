package com.example.backend.domain.event.service;

import com.example.backend.domain.event.entity.EventLog;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.event.repository.EventLogRepository;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.recommendation.service.InterestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventTrackingService {

    private final EventLogRepository eventRepo;
    private final ProductRepository productRepo;

    // 🔥 YANGI
    private final InterestService interestService;

    /**
     * ✅ PERFORMANCE: Feature flags to control expensive operations
     * Set to false to recover from production slowness
     */
    @Value("${analytics.event-logging-enabled:true}")
    private boolean eventLoggingEnabled;

    @Value("${recommendation.user-interest-update-enabled:true}")
    private boolean userInterestUpdateEnabled;

    @Value("${app.fast-mode:false}")
    private boolean fastMode;

    /** VIEW: detail ochilganda */
    @Transactional
    public void logView(User user, Product product) {
        if (user == null || user.getId() == null) return;
        if (product == null || product.getId() == null) return;

        // ✅ EMERGENCY: Skip event logging if disabled (fast mode)
        if (!eventLoggingEnabled || fastMode) {
            log.debug("Event logging disabled (fast mode or feature flag)");
            return;
        }

        save(user, product, EventType.VIEW);

        // ✅ PERFORMANCE: User interest update can be disabled separately
        // This is the SLOW operation (REQUIRES_NEW transaction × 2 per view)
        if (userInterestUpdateEnabled && !fastMode) {
            try {
                interestService.onView(user, product);
            } catch (Exception e) {
                // Never crash event logging if interest update fails
                log.warn("Interest update failed for VIEW: userId={}, productId={}, error={}",
                        user.getId(), product.getId(), e.getMessage());
            }
        }
    }

    /** CLICK: card bosilganda */
    @Transactional
    public void logClick(User user, Long productId) {
        if (user == null || user.getId() == null) return;
        if (productId == null) return;

        // ✅ EMERGENCY: Skip if disabled
        if (!eventLoggingEnabled || fastMode) {
            log.debug("Event logging disabled (fast mode or feature flag)");
            return;
        }

        Product p = productRepo.findById(productId).orElse(null);
        if (p == null) return;

        save(user, p, EventType.CLICK);

        // ✅ PERFORMANCE: Interest update can be disabled
        if (userInterestUpdateEnabled && !fastMode) {
            try {
                interestService.onClick(user, p);
            } catch (Exception e) {
                log.warn("Interest update failed for CLICK: userId={}, productId={}, error={}",
                        user.getId(), productId, e.getMessage());
            }
        }
    }

    /** IMPRESSION: feed/list API response */
    @Transactional
    public void logImpressions(User user, List<Long> productIds) {
        if (user == null || user.getId() == null) return;
        if (productIds == null || productIds.isEmpty()) return;

        LinkedHashSet<Long> uniq = new LinkedHashSet<>(productIds);
        List<Long> ids = uniq.stream().limit(120).toList();

        List<Product> products = productRepo.findByIdInAndActiveTrue(ids);

        List<EventLog> logs = new ArrayList<>(products.size());
        for (Product p : products) {
            EventLog e = new EventLog();
            e.setUser(user);
            e.setProduct(p);
            e.setEventType(EventType.IMPRESSION);
            logs.add(e);
        }
        eventRepo.saveAll(logs);
    }

    private void save(User user, Product p, EventType type) {
        EventLog e = new EventLog();
        e.setUser(user);
        e.setProduct(p);
        e.setEventType(type);
        eventRepo.save(e);
    }
}