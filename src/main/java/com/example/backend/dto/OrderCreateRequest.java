package com.example.backend.dto;

import lombok.Data;

@Data
public class OrderCreateRequest {
    private String address;
    private Double latitude;   // optional
    private Double longitude;
    private String phone;
}
