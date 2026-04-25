package com.example.backend.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MyReviewKeyResponse {
    private Long orderId;
    private Long productId;
}
