package com.example.backend.controller;

import com.example.backend.dto.ProductRecommendResponse;
import com.example.backend.service.ProductRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductRecommendController {

    private final ProductRecommendService recommendService;

    @GetMapping("/{id}/recommend")
    public ProductRecommendResponse recommend(
            @PathVariable Long id,
            @RequestParam(defaultValue = "12") int similar,
            @RequestParam(defaultValue = "24") int others,
            @RequestParam(required = false) String seed
    ) {
        // ✅ safe clamps
        if (similar < 0) similar = 0;
        if (others < 0) others = 0;
        if (similar > 60) similar = 60;
        if (others > 120) others = 120;

        // ✅ seed abuse prevention (DoSga qarshi)
        // - trim
        // - blank bo‘lsa null
        // - max 64 char
        if (seed != null) {
            seed = seed.trim();
            if (seed.isEmpty()) seed = null;
            else if (seed.length() > 64) seed = seed.substring(0, 64);
        }

        return recommendService.recommendForDetail(id, similar, others, seed);
    }
}