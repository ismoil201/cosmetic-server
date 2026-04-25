package com.example.backend.domain.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductVariantRequest {

    private String label;

    private BigDecimal price;
    private BigDecimal discountPrice;

    private int stock;

    private int sortOrder;

    private List<VariantTierRequest> tiers;
}