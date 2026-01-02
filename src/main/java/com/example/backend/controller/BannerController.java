package com.example.backend.controller;

import com.example.backend.dto.BannerResponse;
import com.example.backend.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService service;

    // Android: faqat active + valid vaqt oralig‘idagi bannerlar
    @GetMapping("/banners")
    public List<BannerResponse> banners() {
        return service.activeBanners();
    }
}