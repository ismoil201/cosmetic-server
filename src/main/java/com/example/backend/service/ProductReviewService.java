package com.example.backend.service;

import com.example.backend.dto.ReviewCreateRequest;
import com.example.backend.dto.ReviewResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        User user = userService.getCurrentUser();

        Order order = orderRepo.findById(req.getOrderId())
                .filter(o -> o.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Invalid order"));

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setOrder(order);
        review.setRating(req.getRating());
        review.setContent(req.getContent());

        reviewRepo.save(review);

        // 🔥 REVIEW RASMLAR
        if (req.getImageUrls() != null) {
            for (String url : req.getImageUrls()) {
                ReviewImage img = new ReviewImage();
                img.setReview(review);
                img.setImageUrl(url);
                reviewImageRepo.save(img);
            }
        }

        // ⭐ rating / review_count update
        int newCount = product.getReviewCount() + 1;
        double newAvg =
                (product.getRatingAvg() * product.getReviewCount() + req.getRating())
                        / newCount;

        product.setReviewCount(newCount);
        product.setRatingAvg(newAvg);
        productRepo.save(product);
    }


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
