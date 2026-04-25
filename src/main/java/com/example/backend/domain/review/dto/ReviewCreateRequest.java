package com.example.backend.domain.review.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReviewCreateRequest {

    private Long productId;
    private Long orderId;
    private int rating;
    private String content;

    // 🔥 Review rasm URL’lari
    private List<String> imageUrls;
}
