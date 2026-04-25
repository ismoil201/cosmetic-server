package com.example.backend.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class VariantTierResponse {

    private int minQty;
    private BigDecimal totalPrice;
}