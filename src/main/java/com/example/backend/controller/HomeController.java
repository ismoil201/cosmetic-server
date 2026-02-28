package com.example.backend.controller;

import com.example.backend.dto.HomeResponse;
import com.example.backend.dto.ProductCardResponse;
import com.example.backend.service.HomeFeedService;
import com.example.backend.service.HomeService;
import com.example.backend.service.ProductService;
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
        if (seed == null || seed.isBlank()) {
            // seed bo‘lmasa ham ishlasin
            seed = String.valueOf(System.currentTimeMillis());
        }
        return homeService.home(limit, pageable, seed);
    }

    @GetMapping("/feed")
    public List<ProductCardResponse> feed(@RequestParam(defaultValue="30") int limit) {
        return feedService.buildFeed(limit);
    }
}
