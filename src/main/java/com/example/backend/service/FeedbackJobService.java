package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.EventLogRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedbackJobService {

    private final UserService userService;
    private final EventLogRepository eventRepo;
    private final ProductRepository productRepo;
    private final InterestService interestService;


    @Transactional
    public void applyNegativeFeedback(User user) {

        LocalDateTime after = LocalDateTime.now().minusHours(24);

        // impressionCount map (masalan IMPRESSION eventType bo'lsa)
        Map<Long, Long> impr = new HashMap<>();
        for (Object[] row : eventRepo.countByProductAfter(user, EventType.IMPRESSION, after)) {
            Long productId = (Long) row[0];
            Long count = (Long) row[1];
            impr.put(productId, count);
        }

        // click/view count map
        Map<Long, Long> clk = new HashMap<>();
        var types = java.util.List.of(EventType.CLICK, EventType.VIEW);

        for (Object[] row : eventRepo.countByProductAfterTypes(user, types, after)) {
            Long productId = (Long) row[0];
            Long count = (Long) row[1];
            clk.put(productId, count);
        }

        // penalize: impr >= 3 and click/view == 0
        for (var entry : impr.entrySet()) {
            Long productId = entry.getKey();
            long imprCount = entry.getValue();
            long clickCount = clk.getOrDefault(productId, 0L);

            if (imprCount >= 3 && clickCount == 0) {
                Product p = productRepo.findById(productId).orElse(null);
                if (p == null) continue;

                interestService.bump(user, InterestType.CATEGORY, p.getCategory().name(), -0.3);
                if (p.getBrand() != null && !p.getBrand().isBlank()) {
                    interestService.bump(user, InterestType.BRAND, p.getBrand().trim().toLowerCase(), -0.2);
                }
            }
        }
    }
}