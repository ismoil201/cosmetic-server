package com.example.backend.service;

import com.example.backend.dto.HomeResponse;
import com.example.backend.dto.ProductCardResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ProductRepository productRepo;
    private final ProductService productService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public HomeResponse home(int limit, Pageable pageable, String seed) {

        User user = userService.getCurrentUserOrNull();
        List<Long> used = new ArrayList<>();

        // ✅ CACHED: Home blocks (hits/discounts/new)
        // Cache key includes limit + seed for correct exclusion behavior
        List<Product> hitsP = getHitsBlock(limit, seed, used);
        used.addAll(hitsP.stream().map(Product::getId).toList());

        List<Product> discP = getDiscountsBlock(limit, seed, used);
        used.addAll(discP.stream().map(Product::getId).toList());

        List<Product> newP = getNewArrivalsBlock(limit, used);
        used.addAll(newP.stream().map(Product::getId).toList());

        // 4) Popular (page) - NOT cached (varies by page)
        Page<Product> popP = productRepo.findByActiveTrueOrderBySoldCountDesc(pageable);

        // ✅ CRITICAL OPTIMIZATION: Collect ALL products first, then batch convert once
        List<Product> allProducts = new ArrayList<>();
        allProducts.addAll(hitsP);
        allProducts.addAll(discP);
        allProducts.addAll(newP);
        allProducts.addAll(popP.getContent());

        // ✅ Single batch conversion: 2 queries total (favorites + images) instead of 8
        List<ProductCardResponse> allCards = productService.toCardsPublic(allProducts, user);

        // ✅ Split back into sections using index ranges
        int hitsSize = hitsP.size();
        int discSize = discP.size();
        int newSize = newP.size();
        int popSize = popP.getContent().size();

        List<ProductCardResponse> hits = allCards.subList(0, hitsSize);
        List<ProductCardResponse> discounts = allCards.subList(hitsSize, hitsSize + discSize);
        List<ProductCardResponse> newArrivals = allCards.subList(hitsSize + discSize, hitsSize + discSize + newSize);
        List<ProductCardResponse> popularCards = allCards.subList(hitsSize + discSize + newSize, hitsSize + discSize + newSize + popSize);

        Page<ProductCardResponse> popular = new org.springframework.data.domain.PageImpl<>(
                popularCards, popP.getPageable(), popP.getTotalElements()
        );

        return new HomeResponse(hits, discounts, newArrivals, popular);
    }

    // ==================== CACHED BLOCK METHODS ====================

    /**
     * ✅ CACHED: Hits (Today Deals) Block
     *
     * Cache: home:blocks:hits:{limit}:{seed}:{excludeIds}
     * TTL: 5 minutes
     * Why: Same data for all users, expensive shuffle query
     *
     * Note: Exclusion list is part of cache key to preserve correctness
     */
    @Cacheable(
        value = "home:blocks",
        key = "'hits:' + #limit + ':' + #seed + ':' + (#excludeIds.isEmpty() ? 'none' : #excludeIds.hashCode())",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<Product> getHitsBlock(int limit, String seed, List<Long> excludeIds) {
        return productRepo.hitsShuffledExclude(
            seed,
            excludeIds,
            excludeIds.isEmpty() ? 1 : 0,
            limit
        );
    }

    /**
     * ✅ CACHED: Discounts Block
     *
     * Cache: home:blocks:discounts:{limit}:{seed}:{excludeIds}
     * TTL: 5 minutes
     * Why: Same data for all users, expensive shuffle + filter query
     */
    @Cacheable(
        value = "home:blocks",
        key = "'discounts:' + #limit + ':' + #seed + ':' + (#excludeIds.isEmpty() ? 'none' : #excludeIds.hashCode())",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<Product> getDiscountsBlock(int limit, String seed, List<Long> excludeIds) {
        return productRepo.discountsShuffledExclude(
            seed,
            excludeIds,
            excludeIds.isEmpty() ? 1 : 0,
            limit
        );
    }

    /**
     * ✅ CACHED: New Arrivals Block
     *
     * Cache: home:blocks:new:{limit}:{excludeIds}
     * TTL: 5 minutes
     * Why: Same data for all users, frequently accessed
     */
    @Cacheable(
        value = "home:blocks",
        key = "'new:' + #limit + ':' + (#excludeIds.isEmpty() ? 'none' : #excludeIds.hashCode())",
        unless = "#result == null || #result.isEmpty()"
    )
    public List<Product> getNewArrivalsBlock(int limit, List<Long> excludeIds) {
        return productRepo.newArrivalsExclude(
            excludeIds,
            excludeIds.isEmpty() ? 1 : 0,
            limit
        );
    }
}