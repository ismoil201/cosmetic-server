package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String brand;

    // original narx
    private BigDecimal price;

    // bazaviy discount narx (quantity ga bog‘liq emas)
    private BigDecimal discountPrice;

    private Category category;
    private BigDecimal ratingAvg;

    private int reviewCount;
    private int soldCount;
    private boolean isTodayDeal;
    private boolean favorite;

    // variant stock
    private int stock;

    private List<ProductImageResponse> images;
}