package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.EventLogRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NegativeFeedbackService {

    private final EventLogRepository eventRepo;
    private final ProductRepository productRepo;
    private final InterestService interestService;

    // simple in-memory throttle: userId -> lastRun
    private final Map<Long, LocalDateTime> lastRun = new ConcurrentHashMap<>();

    @Transactional
    public void applyIfNeeded(User user) {
        if (user == null || user.getId() == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = lastRun.get(user.getId());

        // 6 soatda 1 marta
        if (last != null && last.isAfter(now.minusHours(6))) return;

        apply(user);
        lastRun.put(user.getId(), now);
    }

    private void apply(User user) {

        LocalDateTime after = LocalDateTime.now().minusHours(24);

        Map<Long, Long> impr = toCountMap(eventRepo.countByProductAfter(user, EventType.IMPRESSION, after));
        Map<Long, Long> clk = toCountMap(eventRepo.countByProductAfterTypes(
                user,
                List.of(EventType.CLICK, EventType.VIEW),
                after
        ));

        // ✅ FIX: Collect negative product IDs first
        List<Long> negativeProductIds = new ArrayList<>();
        for (var e : impr.entrySet()) {
            Long productId = e.getKey();
            long imprCount = e.getValue();
            long clickCount = clk.getOrDefault(productId, 0L);

            // 3+ marta ko'rdi, lekin umuman bosmadi => qiziqmas ekan
            if (imprCount >= 3 && clickCount == 0) {
                negativeProductIds.add(productId);
            }
        }

        // ✅ FIX: Batch load products (1 query instead of N)
        if (negativeProductIds.isEmpty()) return;

        List<Product> products = productRepo.findAllById(negativeProductIds);

        // ✅ Apply penalties
        for (Product p : products) {
            if (p == null) continue;

            // kichik penalty: "bu yoqmayapti"
            if (p.getCategory() != null) {
                interestService.bump(user, InterestType.CATEGORY, p.getCategory().name(), -0.3);
            }
            if (p.getBrand() != null && !p.getBrand().isBlank()) {
                interestService.bump(user, InterestType.BRAND, p.getBrand().trim().toLowerCase(), -0.2);
            }
        }
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] r : rows) {
            if (r == null || r.length < 2) continue;
            Long id = (Long) r[0];
            Long cnt = (Long) r[1];
            if (id != null && cnt != null) m.put(id, cnt);
        }
        return m;
    }
}