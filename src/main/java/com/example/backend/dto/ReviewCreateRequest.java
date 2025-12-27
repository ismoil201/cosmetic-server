package com.example.backend.dto;

import lombok.Data;

@Data
public class ReviewCreateRequest {

    private Long productId;
    private Long orderId;

    // 1 ~ 5
    private int rating;

    private String content;
}
