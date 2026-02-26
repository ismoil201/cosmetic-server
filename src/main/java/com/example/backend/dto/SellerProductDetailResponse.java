package com.example.backend.dto;

import com.example.backend.dto.ProductDetailImageResponse;
import com.example.backend.dto.ProductImageResponse;
import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class SellerProductDetailResponse {

    private Long id;

    private String name;
    private String description;
    private String brand;

    private BigDecimal price;
    private BigDecimal discountPrice;

    private Category category;

    private int stock;
    private int soldCount;

    private BigDecimal ratingAvg;
    private int reviewCount;

    private boolean active;

    private List<ProductImageResponse> images;
    private List<ProductDetailImageResponse> detailImages;

    private LocalDateTime createdAt;
}