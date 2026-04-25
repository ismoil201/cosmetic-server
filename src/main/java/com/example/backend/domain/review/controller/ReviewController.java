package com.example.backend.domain.review.controller;

import com.example.backend.global.response.ApiResponse;
import com.example.backend.domain.review.dto.MyReviewKeyResponse;
import com.example.backend.domain.review.dto.ReviewCreateRequest;
import com.example.backend.domain.review.dto.ReviewResponse;
import com.example.backend.domain.product.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ProductReviewService reviewService;

    // 🔐 REVIEW YOZISH (faqat login qilingan user)
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@RequestBody ReviewCreateRequest req) {
        reviewService.create(req);
        return ResponseEntity.ok(ApiResponse.okMessage("Review created"));
    }


    // 🔓 PRODUCT REVIEW LIST (public)
    @GetMapping("/product/{productId}")
    public List<ReviewResponse> byProduct(@PathVariable Long productId) {
        return reviewService.getByProduct(productId);


    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MyReviewKeyResponse>>> my() {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getMyReviewKeys()));
    }


}
