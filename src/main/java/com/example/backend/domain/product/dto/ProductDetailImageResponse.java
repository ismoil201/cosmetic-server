package com.example.backend.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductDetailImageResponse {
    private String imageUrl;
    private int sortOrder;
}
