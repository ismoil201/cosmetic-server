package com.example.backend.controller;

import com.example.backend.dto.ApiResponse;
import com.example.backend.dto.MyReviewKeyResponse;
import com.example.backend.dto.ReviewCreateRequest;
import com.example.backend.dto.ReviewResponse;
import com.example.backend.service.ProductReviewService;
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
    public List<MyReviewKeyResponse> my() {
        return reviewService.getMyReviewKeys();
    }

}
