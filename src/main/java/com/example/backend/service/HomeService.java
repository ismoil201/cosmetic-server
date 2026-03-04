package com.example.backend.service;

import com.example.backend.dto.HomeResponse;
import com.example.backend.dto.ProductCardResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ProductRepository productRepo;
    private final ProductService productService;
    private final UserService userService;

    public HomeResponse home(int limit, Pageable pageable, String seed) {

        User user = userService.getCurrentUserOrNull();
        List<Long> used = new ArrayList<>();

        // 1) Hits
        List<Product> hitsP = productRepo.hitsShuffledExclude(seed, used, used.isEmpty() ? 1 : 0, limit);
        used.addAll(hitsP.stream().map(Product::getId).toList());
        List<ProductCardResponse> hits = productService.toCardsPublic(hitsP, user);

        // 2) Discounts
        List<Product> discP = productRepo.discountsShuffledExclude(seed, used, used.isEmpty() ? 1 : 0, limit);
        used.addAll(discP.stream().map(Product::getId).toList());
        List<ProductCardResponse> discounts = productService.toCardsPublic(discP, user);

        // 3) New
        List<Product> newP = productRepo.newArrivalsExclude(used, used.isEmpty() ? 1 : 0, limit);
        used.addAll(newP.stream().map(Product::getId).toList());
        List<ProductCardResponse> newArrivals = productService.toCardsPublic(newP, user);

        // 4) Popular (page) - EXCLUDE QILMAYMIZ ✅
        Page<Product> popP = productRepo.findByActiveTrueOrderBySoldCountDesc(pageable);

        // Page -> List -> batch -> PageImpl
        List<ProductCardResponse> popularCards = productService.toCardsPublic(popP.getContent(), user);
        Page<ProductCardResponse> popular = new org.springframework.data.domain.PageImpl<>(
                popularCards, popP.getPageable(), popP.getTotalElements()
        );

        return new HomeResponse(hits, discounts, newArrivals, popular);
    }
}