package com.example.backend.domain.event.service;

import com.example.backend.domain.event.entity.EventLog;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.event.repository.EventLogRepository;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.recommendation.service.InterestService;
import com.example.backend.domain.user.service.UserService;
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
