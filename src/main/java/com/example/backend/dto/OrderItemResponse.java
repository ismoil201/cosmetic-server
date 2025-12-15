package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderItemResponse {

    private Long productId;
    private String name;
    private String imageUrl;
    private double price;
    private int quantity;
}
