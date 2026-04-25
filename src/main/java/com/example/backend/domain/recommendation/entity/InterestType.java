package com.example.backend.domain.recommendation.entity;

public enum InterestType {
    CATEGORY,      // Product category (SKINCARE, MAKEUP, etc.)
    BRAND,         // Brand name (COSRX, Etude House, etc.)
    QUERY,         // Search query text (sunscreen, moisturizer, etc.)
    TOKEN,         // Individual search tokens (deprecated, use QUERY)
    PRICE_BUCKET   // Price range preference
}