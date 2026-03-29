package com.example.backend.dto;


import com.example.backend.entity.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Card Response DTO
 * <p>
 * Used by:
 * - Home feed (/api/home/feed)
 * - Product listing (/api/products)
 * - Search results (/api/products/search)
 * - Recommendations
 * <p>
 * Android Compatibility:
 * - All BigDecimal fields serialize as plain JSON numbers
 * - All lists serialize as plain JSON arrays
 * - Field names match Android model expectations
 * - No Java type metadata (@class, typed arrays)
 */
@Data
@AllArgsConstructor
public class ProductCardResponse {
    private Long id;
    private String name;
    private String brand;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Category category;

    private BigDecimal ratingAvg;
    private int reviewCount;
    private int soldCount;

    /**
     * ⚠️ CRITICAL: Field name must serialize as "todayDeal" for Android
     * <p>
     * Without @JsonProperty:
     * - Lombok @Data generates isTodayDeal() getter
     * - Jackson serializes as "isTodayDeal" (from getter name)
     * - Android expects "todayDeal" → parsing fails
     * <p>
     * With @JsonProperty("todayDeal"):
     * - Forces JSON field name to "todayDeal"
     * - Android Gson/Retrofit parses correctly
     */
    @JsonProperty("todayDeal")
    private boolean todayDeal;

    private boolean favorite;
    private int stock;

    private ProductImageResponse mainImageUrl;
    private List<ProductImageResponse> images;
}
