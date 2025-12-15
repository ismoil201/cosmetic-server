package com.example.backend.dto;

import com.example.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private double totalAmount;
    private LocalDateTime createdAt;

    private List<OrderItemResponse> items;
}
