package com.example.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductCreateRequest {

    private String name;
    private String description;
    private String brand;
    private double price;
    private double discountPrice;
    private String category;
    private int stock;

    // 🔥 MAIN / SLIDER IMAGES
    private List<String> imageUrls;

    // 🔥 DETAIL PAGE IMAGES (Musinsa style)
    private List<ProductDetailImageRequest> detailImages;
}
