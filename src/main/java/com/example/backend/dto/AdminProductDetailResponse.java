package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class AdminProductDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String brand;

    private BigDecimal price;
    private BigDecimal discountPrice;
    private Category category;
    private int stock;

    private boolean active;
    private boolean isTodayDeal;

    private List<String> imageUrls;
    private List<ProductDetailImageResponse> detailImages;

    private List<ProductVariantResponse> variants;

    private SellerShortResponse seller;
}