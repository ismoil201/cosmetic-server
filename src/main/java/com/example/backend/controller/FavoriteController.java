package com.example.backend.controller;

import com.example.backend.dto.ProductResponse;
import com.example.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}/toggle")
    public Map<String, Boolean> toggle(@PathVariable Long productId) {
        return Map.of("favorite", favoriteService.toggle(productId));
    }

    @GetMapping
    public List<ProductResponse> myFavorites() {
        return favoriteService.myFavorites();
    }
}
