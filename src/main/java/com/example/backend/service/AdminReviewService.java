package com.example.backend.service;

import com.example.backend.entity.Product;
import com.example.backend.entity.ProductReview;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ProductReviewRepository reviewRepo;
    private final ProductRepository productRepo;

    // 🛡️ BLOCK / UNBLOCK
    @Transactional
    public void setActive(Long reviewId, boolean active) {

        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Agar holat o‘zgarmasa – hech narsa qilmaymiz
        if (review.isActive() == active) return;

        Product product = review.getProduct();

        // ❗ Agar BLOCK qilinsa → ratingdan AYIRAMIZ
        if (!active) {
            int newCount = product.getReviewCount() - 1;
            double newAvg = newCount == 0 ? 0 :
                    (product.getRatingAvg() * product.getReviewCount() - review.getRating())
                            / newCount;

            product.setReviewCount(newCount);
            product.setRatingAvg(newAvg);
        }

        // ❗ Agar UNBLOCK qilinsa → ratingga QO‘SHAMIZ
        if (active) {
            int newCount = product.getReviewCount() + 1;
            double newAvg =
                    (product.getRatingAvg() * product.getReviewCount() + review.getRating())
                            / newCount;

            product.setReviewCount(newCount);
            product.setRatingAvg(newAvg);
        }

        review.setActive(active);
        productRepo.save(product);
        reviewRepo.save(review);
    }

    // ❌ DELETE (butunlay o‘chirish)
    @Transactional
    public void delete(Long reviewId) {

        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        Product product = review.getProduct();

        if (review.isActive()) {
            int newCount = product.getReviewCount() - 1;
            double newAvg = newCount == 0 ? 0 :
                    (product.getRatingAvg() * product.getReviewCount() - review.getRating())
                            / newCount;

            product.setReviewCount(newCount);
            product.setRatingAvg(newAvg);
            productRepo.save(product);
        }

        reviewRepo.delete(review);
    }
}
