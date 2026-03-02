package com.example.backend.dto;

import java.util.List;

public record ProductRecommendResponse(
        List<ProductCardResponse> similar,
        List<ProductCardResponse> others
) {}