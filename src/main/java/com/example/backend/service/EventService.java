package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.repository.EventLogRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventLogRepository eventRepo;
    private final UserService userService;
    private final ProductRepository productRepo;
    private final InterestService interestService;

    @Transactional
    public void log(EventType type, Long productId, String screen, Integer position, String queryText, String sessionId) {

        User user = userService.getCurrentUserOrNull();
        Product p = null;
        if (productId != null) {
            p = productRepo.findById(productId).orElse(null);
        }

        EventLog e = new EventLog();
        e.setUser(user);
        e.setSessionId(sessionId);
        e.setEventType(type);
        e.setProduct(p);
        e.setScreen(screen);
        e.setPosition(position);
        e.setQueryText(queryText);

        if (p != null) {
            e.setCategory(p.getCategory().name());
            e.setBrand(p.getBrand());
        }

        eventRepo.save(e);

        if (user != null && p != null) {
            switch (type) {
                case VIEW -> interestService.onView(user, p);
                case CLICK -> interestService.onClick(user, p);
                case FAVORITE_ADD -> interestService.onFavorite(user, p, true);
                case FAVORITE_REMOVE -> interestService.onFavorite(user, p, false);
                case ADD_TO_CART -> interestService.onAddToCart(user, p);
                case PURCHASE -> interestService.onPurchase(user, p);
            }
        }
    }
}
