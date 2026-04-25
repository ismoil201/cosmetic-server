package com.example.backend.domain.order.dto;

import com.example.backend.domain.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OrderStatusHistoryResponse {
    private OrderStatus status;
    private LocalDateTime createdAt;
}
