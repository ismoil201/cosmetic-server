package com.example.backend.dto;

import lombok.Data;

@Data
public class OrderCreateRequest {

    private Long receiverId;
    private Long addressId;
}

