package com.example.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
public class HomeResponse {
    // 🔥 Bugun xit
    private List<ProductCardResponse> hits;

    // ⚡ Chegirmalar
    private List<ProductCardResponse> discounts;

    // 🆕 Yangi kelganlar
    private List<ProductCardResponse> newArrivals;

    // 🎯 Siz uchun / Popular grid
    private Page<ProductCardResponse> popular;
}
