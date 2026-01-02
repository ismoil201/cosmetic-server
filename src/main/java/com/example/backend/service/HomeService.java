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

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ProductRepository productRepo;
    private final ProductService productService; // sizning toCard mapping ishlatish uchun
    private final UserService userService;

    public HomeResponse home(int limit, Pageable pageable, String seed) {

        User user = userService.getCurrentUserOrNull();
        List<Long> used = new java.util.ArrayList<>();

        // 1) Hits
        List<Product> hitsP = productRepo.hitsShuffledExclude(seed, used, used.isEmpty()?1:0, limit);
        used.addAll(hitsP.stream().map(Product::getId).toList());
        var hits = hitsP.stream().map(p -> productServiceToCard(p, user)).toList();

        // 2) Discounts
        List<Product> discP = productRepo.discountsShuffledExclude(seed, used, used.isEmpty()?1:0, limit);
        used.addAll(discP.stream().map(Product::getId).toList());
        var discounts = discP.stream().map(p -> productServiceToCard(p, user)).toList();

        // 3) New
        List<Product> newP = productRepo.newArrivalsExclude(used, used.isEmpty()?1:0, limit);
        used.addAll(newP.stream().map(Product::getId).toList());
        var newArrivals = newP.stream().map(p -> productServiceToCard(p, user)).toList();

        // 4) Popular (page)
        // 4) Popular (page) - EXCLUDE QILMAYMIZ ✅
        Page<Product> popP = productRepo.findByActiveTrueOrderBySoldCountDesc(pageable);
        var popular = popP.map(p -> productServiceToCard(p, user));

        return new HomeResponse(hits, discounts, newArrivals, popular);
    }

    // sizning ProductService ichidagi toCard private, shuni ko‘chiring yoki public qiling:
    public ProductCardResponse productServiceToCard(Product p, User user) {
        return productService.toCardPublic(p, user); // pastda aytaman
    }
}
