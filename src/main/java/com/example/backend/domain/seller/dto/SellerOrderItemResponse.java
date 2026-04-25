package com.example.backend.domain.seller.dto;


import com.example.backend.domain.order.dto.OrderItemResponse;

public record SellerOrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        java.math.BigDecimal price
) {

    public static SellerOrderItemResponse from(OrderItemResponse item) {
        return new SellerOrderItemResponse(
                item.getProductId(),
                item.getName(),
                item.getQuantity(),
                item.getPrice()
        );
    }
}