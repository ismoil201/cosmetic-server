package com.example.backend.dto;


import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SellerProductListResponse {

    private Long id;

    private String name;
    private String brand;

    private BigDecimal price;
    private BigDecimal discountPrice;

    private Category category;

    private int stock;
    private int soldCount;

    private BigDecimal ratingAvg;
    private int reviewCount;

    private boolean active;

    private String mainImageUrl;

    private LocalDateTime createdAt;
}