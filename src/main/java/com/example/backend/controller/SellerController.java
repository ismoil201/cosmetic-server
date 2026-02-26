package com.example.backend.controller;

import com.example.backend.entity.Seller;
import com.example.backend.service.SellerService;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/me")
    public Seller me() {
        return sellerService.requireCurrentSeller();
    }
}