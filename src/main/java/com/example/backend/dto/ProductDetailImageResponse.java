package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductDetailImageResponse {
    private String imageUrl;
    private int sortOrder;
}
