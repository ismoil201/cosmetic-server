package com.example.backend.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductImageResponse {
    private String imageUrl;
    private boolean main;
}

