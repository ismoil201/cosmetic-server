package com.example.backend.domain.seller.dto;

import com.example.backend.domain.seller.entity.SellerOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellerOrderListResponse(
        Long id,
        String status,
        Long orderId,
        Long sellerId,
        String customerName,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
    public static SellerOrderListResponse from(SellerOrder so) {
        BigDecimal subtotal = so.getSubtotalAmount() != null ? so.getSubtotalAmount() : BigDecimal.ZERO;
        BigDecimal shipping = so.getShippingFee() != null ? so.getShippingFee() : BigDecimal.ZERO;

        return new SellerOrderListResponse(
                so.getId(),
                so.getStatus().name(),
                so.getOrder() != null ? so.getOrder().getId() : null,
                so.getSeller() != null ? so.getSeller().getId() : null,
                (so.getOrder() != null && so.getOrder().getUser() != null)
                        ? so.getOrder().getUser().getFullName()
                        : null,
                subtotal,
                shipping,
                subtotal.add(shipping),
                so.getCreatedAt()
        );
    }
}