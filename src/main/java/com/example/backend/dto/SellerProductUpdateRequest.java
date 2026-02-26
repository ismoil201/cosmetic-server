package com.example.backend.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SellerProductUpdateRequest {

    private String name;
    private String description;
    private String brand;
    private String category;

    private BigDecimal price;
    private BigDecimal discountPrice;
    private int stock;

    private List<String> imageUrls;

    private List<ProductDetailImageRequest> detailImages;
}