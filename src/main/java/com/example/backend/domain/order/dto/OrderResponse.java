package com.example.backend.domain.order.dto;

import com.example.backend.domain.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private OrderStatus status;

    // 💰 BIGDECIMAL (ENG MUHIM FIX)
    private BigDecimal totalAmount;

    private LocalDateTime createdAt;

    // 📍 SNAPSHOT
    private String address;
    private Double latitude;
    private Double longitude;

    private String fullName;
    // 📞 SNAPSHOT
    private String phone;

    private List<OrderItemResponse> items;
}
