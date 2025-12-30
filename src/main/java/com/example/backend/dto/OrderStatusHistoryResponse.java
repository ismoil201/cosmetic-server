package com.example.backend.dto;

import com.example.backend.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderStatusHistoryResponse {
    private OrderStatus status;
    private LocalDateTime createdAt;
}
