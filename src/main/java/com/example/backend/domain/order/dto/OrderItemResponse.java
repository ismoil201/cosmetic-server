package com.example.backend.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemResponse {

    private Long productId;
    private String name;

    private Long variantId;
    private String variantLabel;

    private String imageUrl;

    // ✅ BU LINE TOTAL (tier applied)
    private BigDecimal price;

    private int quantity;
}