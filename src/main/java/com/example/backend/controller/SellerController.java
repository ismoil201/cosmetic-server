package com.example.backend.controller;

import com.example.backend.entity.Seller;
import com.example.backend.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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