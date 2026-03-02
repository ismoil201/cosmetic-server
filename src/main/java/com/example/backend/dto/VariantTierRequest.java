package com.example.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantTierRequest {
    private int minQty;
    private BigDecimal totalPrice;
}