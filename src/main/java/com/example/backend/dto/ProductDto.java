package com.example.backend.dto;

import lombok.Data;

@Data
public class ProductDto {
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private String category;
}
