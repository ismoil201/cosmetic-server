package com.example.backend.domain.recommendation.dto;

import com.example.backend.domain.product.dto.ProductCardResponse;

import java.util.List;

public record ProductRecommendResponse(
        List<ProductCardResponse> similar,
        List<ProductCardResponse> others
) {}