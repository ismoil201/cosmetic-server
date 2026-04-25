package com.example.backend.domain.cart.dto;

import lombok.Data;

@Data
public class CartAddRequest {
    private Long variantId;

    private int quantity;
}
