package com.example.backend.domain.product.dto;

import lombok.Data;

@Data
public class ProductDetailImageRequest {
    private String imageUrl;
    private int sortOrder;
}
