package com.example.backend.service;

import com.example.backend.dto.ReviewCreateRequest;
import com.example.backend.dto.ReviewResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final UserService userService;
    private final ReviewImageRepository reviewImageRepo;

    @Transactional
    public void create(ReviewCreateRequest req) {

        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        User user = userService.getCurrentUser();

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 🔐 Order egasi tekshiriladi
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Order does not belong to user");
        }

        // 🔐 Order ichida shu product bormi?
        boolean hasProduct = order.getItems().stream()
                .anyMatch(i -> i.getProduct().getId().equals(product.getId()));

        if (!hasProduct) {
            throw new RuntimeException("Product not found in this order");
        }

        // ❌ Bir marta review
        if (reviewRepo.existsByUserAndProduct(user, product)) {
            throw new RuntimeException("You already reviewed this product");
        }

        // 1️⃣ Review saqlash
        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setOrder(order); // 🔥 juda muhim
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        reviewRepo.save(review);

        // 2️⃣ Review rasmlari
        if (req.getImageUrls() != null) {
            for (String url : req.getImageUrls()) {
                ReviewImage img = new ReviewImage();
                img.setReview(review);
                img.setImageUrl(url);
                reviewImageRepo.save(img);
            }
        }

        // 3️⃣ ⭐ RATING AVG UPDATE
        BigDecimal total = product.getRatingAvg()
                .multiply(BigDecimal.valueOf(product.getReviewCount()));

        BigDecimal newAvg = total
                .add(BigDecimal.valueOf(req.getRating()))
                .divide(
                        BigDecimal.valueOf(product.getReviewCount() + 1),
                        2,
                        RoundingMode.HALF_UP
                );

        product.setRatingAvg(newAvg);
        product.setReviewCount(product.getReviewCount() + 1);
        productRepo.save(product);
    }


    @Transactional(readOnly = true)
    public List<ReviewResponse> getByProduct(Long productId) {

        return reviewRepo.findByProductIdAndActiveTrue(productId)
                .stream()
                .map(r -> {

                    List<String> imageUrls =
                            reviewImageRepo.findByReviewId(r.getId())
                                    .stream()
                                    .map(ReviewImage::getImageUrl)
                                    .toList();

                    return new ReviewResponse(
                            r.getId(),
                            r.getUser().getFullName(),
                            r.getRating(),
                            r.getContent(),
                            r.getCreatedAt(),
                            imageUrls
                    );
                })
                .toList();
    }

}
