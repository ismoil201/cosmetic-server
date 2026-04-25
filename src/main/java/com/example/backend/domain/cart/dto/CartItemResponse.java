package com.example.backend.domain.cart.dto;

import com.example.backend.domain.product.dto.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CartItemResponse {

    private Long id;

    private ProductResponse product;

    private Long variantId;
    private String variantLabel;

    // savatdagi hozirgi quantity uchun effective bitta dona narx
    private BigDecimal unitPrice;

    // savatdagi hozirgi quantity uchun final total narx
    private BigDecimal lineTotal;

    private int quantity;
}