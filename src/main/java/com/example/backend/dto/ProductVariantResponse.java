package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;
    private String label;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private int stock;
    private List<VariantTierResponse> tiers;
}