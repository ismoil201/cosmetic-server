package com.example.backend.domain.product.service;

import com.example.backend.domain.order.entity.Order;
import com.example.backend.domain.order.repository.OrderRepository;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.review.repository.ProductReviewRepository;
import com.example.backend.domain.review.entity.ProductReview;
import com.example.backend.domain.review.dto.MyReviewKeyResponse;
import com.example.backend.domain.review.dto.ReviewCreateRequest;
import com.example.backend.domain.review.dto.ReviewResponse;
import com.example.backend.domain.review.entity.ReviewImage;
import com.example.backend.domain.review.repository.ReviewImageRepository;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.service.UserService;
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
        // ✅ Bir order uchun 1 marta review
        if (reviewRepo.existsByUserAndProductAndOrder(user, product, order)) {
            throw new RuntimeException("You already reviewed this product for this order");
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

    @Transactional(readOnly = true)
    public List<MyReviewKeyResponse> getMyReviewKeys() {
        User user = userService.getCurrentUser();
        return reviewRepo.findByUserId(user.getId())
                .stream()
                .map(r -> new MyReviewKeyResponse(
                        r.getOrder().getId(),
                        r.getProduct().getId()
                ))
                .toList();
    }



}
