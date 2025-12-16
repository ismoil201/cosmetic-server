package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminProductResponse {
    private Long id;
    private String name;
    private String brand;
    private double price;
    private double discountPrice;
    private int stock;
    private boolean active;
    private Category category;
}
