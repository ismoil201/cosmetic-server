package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.Data;

@Data
public class ProductCreateRequest {

    private String name;
    private String description;
    private String brand;

    private double price;
    private double discountPrice;

    private String imageUrl;
    private Category category;

    private int stock;
}
