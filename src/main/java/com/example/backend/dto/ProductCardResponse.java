package com.example.backend.dto;


import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ProductCardResponse {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Category category;

    private BigDecimal ratingAvg;
    private int reviewCount;
    private int soldCount;

    private boolean isTodayDeal;
    private boolean favorite;
    private int stock;
    private ProductImageResponse mainImageUrl;

}
