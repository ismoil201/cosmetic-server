package com.example.backend.dto;

import com.example.backend.entity.SellerOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerOrderDetailResponse(
        Long id,
        String status,
        Long orderId,
        Long sellerId,
        String customerName,
        String phone,
        String address,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
    public static SellerOrderDetailResponse from(SellerOrder so) {
        BigDecimal subtotal = so.getSubtotalAmount() != null ? so.getSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal shipping = so.getShippingFee() != null ? so.getShippingFee() : BigDecimal.ZERO;

        return new SellerOrderDetailResponse(
                so.getId(),
                so.getStatus().name(),
                so.getOrder() != null ? so.getOrder().getId() : null,
                so.getSeller() != null ? so.getSeller().getId() : null,
                (so.getOrder() != null && so.getOrder().getUser() != null)
                        ? so.getOrder().getUser().getFullName()
                        : null,
                so.getOrder() != null ? so.getOrder().getPhone() : null,
                so.getOrder() != null ? so.getOrder().getAddress() : null,
                subtotal,
                shipping,
                subtotal.add(shipping),
                so.getCreatedAt()
        );
    }
}