package com.example.backend.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
public class HomeResponse {
    private List<ProductCardResponse> hits;
    private List<ProductCardResponse> discounts;
    private List<ProductCardResponse> newArrivals;
    private Page<ProductCardResponse> popular;
}
