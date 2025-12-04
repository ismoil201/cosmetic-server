package com.example.backend.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Long userId;
    private String address;
    private double totalAmount;
}
