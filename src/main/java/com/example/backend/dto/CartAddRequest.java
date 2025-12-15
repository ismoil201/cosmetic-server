package com.example.backend.dto;

import lombok.Data;

@Data
public class CartAddRequest {
    private Long productId;
    private int quantity;
}
