package com.example.backend.domain.seller.dto;

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