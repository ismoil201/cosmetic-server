package com.example.backend.domain.home.controller;

import com.example.backend.domain.home.dto.HomeResponse;
import com.example.backend.domain.product.dto.ProductCardResponse;
import com.example.backend.domain.home.service.HomeFeedService;
import com.example.backend.domain.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    private final HomeFeedService feedService;


    @GetMapping
    public HomeResponse home(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String seed,
            Pageable pageable
    ) {
        // ✅ Limit validation: prevent DoS
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50;  // Max 50 per block

        if (seed == null || seed.isBlank()) {
            // seed bo'lmasa ham ishlasin
            seed = String.valueOf(System.currentTimeMillis());
        }
        return homeService.home(limit, pageable, seed);
    }

    @GetMapping("/feed")
    public List<ProductCardResponse> feed(@RequestParam(defaultValue="30") int limit) {
        // ✅ Limit validation: prevent DoS
        if (limit < 1) limit = 30;
        if (limit > 100) limit = 100;  // Max 100 for infinite scroll

        return feedService.buildFeed(limit);
    }
}
