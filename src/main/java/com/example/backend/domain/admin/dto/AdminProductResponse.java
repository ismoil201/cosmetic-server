package com.example.backend.domain.admin.dto;

import com.example.backend.domain.product.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminProductResponse {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private int stock;
    private boolean active;
    private Category category;
    private boolean isTodayDeal;
    private int soldCount;
    // 🔥
    private String mainImageUrl;
    private Long sellerId;
    private String sellerName;

}
