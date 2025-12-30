package com.example.backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductCreateRequest {

    private String name;
    private String description;
    private String brand;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String category;
    private int stock;

    // 🔥 MAIN / SLIDER IMAGES
    private List<String> imageUrls;

    // 🔥 DETAIL PAGE IMAGES (Musinsa style)
    private List<ProductDetailImageRequest> detailImages;
}
