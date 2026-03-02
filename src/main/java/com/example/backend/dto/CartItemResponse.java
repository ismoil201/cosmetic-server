package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CartItemResponse {
    private Long id;

    private ProductResponse product;

    private Long variantId;
    private String variantLabel;

    private BigDecimal unitPrice;  // fallback uchun
    private BigDecimal lineTotal;  // tier bo‘lsa total shu bo‘ladi

    private int quantity;
}