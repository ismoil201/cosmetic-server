package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SellerShortResponse {
    private Long id;
    private String name;
    private String phone;
    private String status;
    private Long ownerUserId;
}