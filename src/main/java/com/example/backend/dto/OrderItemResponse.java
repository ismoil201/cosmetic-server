package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemResponse {

    private Long productId;
    private String name;
    private String imageUrl;
    // 💰 BIGDECIMAL (ENG MUHIM FIX)
    private BigDecimal price;
    private int quantity;
}
