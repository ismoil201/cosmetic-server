package com.example.backend.service;

import com.example.backend.entity.InterestType;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.entity.UserInterest;
import com.example.backend.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InterestService {

    private static final double DECAY = 0.98;

    private final UserInterestRepository interestRepo;

    public void onView(User user, Product p) {
        bump(user, InterestType.CATEGORY, p.getCategory().name(), 1.0);
        bumpBrand(user, p, 0.6);
    }

    public void onClick(User user, Product p) {
        bump(user, InterestType.CATEGORY, p.getCategory().name(), 2.0);
        bumpBrand(user, p, 1.0);
    }

    public void onFavorite(User user, Product p, boolean added) {
        double d = added ? 5.0 : -3.0;
        bump(user, InterestType.CATEGORY, p.getCategory().name(), d);
        bumpBrand(user, p, added ? 2.0 : -1.5);
    }

    public void onAddToCart(User user, Product p) {
        bump(user, InterestType.CATEGORY, p.getCategory().name(), 7.0);
        bumpBrand(user, p, 3.0);
    }

    public void onPurchase(User user, Product p) {
        bump(user, InterestType.CATEGORY, p.getCategory().name(), 15.0);
        bumpBrand(user, p, 6.0);
    }

    private void bumpBrand(User user, Product p, double delta) {
        if (p.getBrand() == null || p.getBrand().isBlank()) return;
        bump(user, InterestType.BRAND, p.getBrand().trim().toLowerCase(), delta);
    }

    public void bump(User user, InterestType type, String key, double delta) {
        if (key == null || key.isBlank()) return;
        String k = key.trim();

        UserInterest row = interestRepo.findByUserAndTypeAndKey(user, type, k)
                .orElseGet(() -> {
                    UserInterest ui = new UserInterest();
                    ui.setUser(user);
                    ui.setType(type);
                    ui.setKey(k);
                    ui.setScore(0);
                    ui.setUpdatedAt(LocalDateTime.now());
                    return ui;
                });

        row.setScore(row.getScore() * DECAY + delta);
        row.setUpdatedAt(LocalDateTime.now());
        interestRepo.save(row);
    }
}