package com.example.backend.controller;

import com.example.backend.dto.ProductDto;
import com.example.backend.entity.Product;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @PostMapping("/toggle")
    public Map<String, Boolean> toggle(@RequestBody CartRequest req) {

        User user = userRepo.findById(req.getUserId()).orElseThrow();
        Product product = productRepo.findById(req.getProductId()).orElseThrow();

        return favRepo.findByUserAndProduct(user, product)
                .map(fav -> {
                    favRepo.delete(fav);
                    return Map.of("favorite", false);
                })
                .orElseGet(() -> {
                    Favorite f = new Favorite();
                    f.setUser(user);
                    f.setProduct(product);
                    favRepo.save(f);
                    return Map.of("favorite", true);
                });
    }
}

