package com.example.backend.dto;



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