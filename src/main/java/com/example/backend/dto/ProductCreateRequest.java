package com.example.backend.dto;

import com.example.backend.entity.Category;
import lombok.Data;

import java.util.List;

@Data
public class ProductCreateRequest {

    private String name;
    private String description;
    private String brand;
    private double price;
    private double discountPrice;
    private String category;
    private int stock;

    // 🔥 Firebase’dan kelgan URL’lar
    private List<String> imageUrls;
}
