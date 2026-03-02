package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.EventLogRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EventTrackingService {

    private final EventLogRepository eventRepo;
    private final ProductRepository productRepo;

    // 🔥 YANGI
    private final InterestService interestService;

    /** VIEW: detail ochilganda */
    @Transactional
    public void logView(User user, Product product) {
        if (user == null || user.getId() == null) return;
        if (product == null || product.getId() == null) return;

        save(user, product, EventType.VIEW);

        // 🔥 interest update
        interestService.onView(user, product);
    }

    /** CLICK: card bosilganda */
    @Transactional
    public void logClick(User user, Long productId) {
        if (user == null || user.getId() == null) return;
        if (productId == null) return;

        Product p = productRepo.findById(productId).orElse(null);
        if (p == null) return;

        save(user, p, EventType.CLICK);

        // 🔥 ENG MUHIM QATOR
        interestService.onClick(user, p);
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