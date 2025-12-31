package com.example.backend.controller;

import com.example.backend.dto.HomeResponse;
import com.example.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    /**
     * HOME PAGE API:
     * - hits (bugun xit)
     * - discounts (chegirmalar)
     * - newArrivals (yangi kelganlar)
     * - popular (grid/pagination)
     *
     * Example:
     * /api/home?limit=10&page=0&size=20
     */
    @GetMapping
    public HomeResponse home(
            @RequestParam(defaultValue = "10") int limit,
            Pageable pageable // popular grid uchun
    ) {
        return new HomeResponse(
                productService.getHits(limit),
                productService.getDiscounts(limit),
                productService.getNewArrivals(limit),
                productService.getPopular(pageable)
        );
    }
}
