package com.example.backend.dto;

import lombok.Data;

@Data
public class ProductDetailImageRequest {
    private String imageUrl;
    private int sortOrder;
}
