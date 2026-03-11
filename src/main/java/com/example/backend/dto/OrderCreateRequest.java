package com.example.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreateRequest {

    private Long receiverId;
    private Long addressId;
    private List<Long> cartItemIds;
}