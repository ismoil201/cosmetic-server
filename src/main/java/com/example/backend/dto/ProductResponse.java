package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String brand;
    private double price;
    private double discountPrice;
    private Category category;
    private double ratingAvg;
    private int reviewCount;
    private boolean favorite;


    private List<ProductImageResponse> images;
}
