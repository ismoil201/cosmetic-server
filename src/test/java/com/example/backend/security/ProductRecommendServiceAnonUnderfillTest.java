package com.example.backend.security;

import com.example.backend.domain.product.dto.ProductCardResponse;
import com.example.backend.domain.recommendation.dto.ProductRecommendResponse;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.event.repository.EventLogRepository;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.recommendation.repository.UserInterestRepository;
import com.example.backend.domain.recommendation.service.NegativeFeedbackService;
import com.example.backend.domain.recommendation.service.ProductRecommendService;
import com.example.backend.domain.product.service.ProductService;
import com.example.backend.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductRecommendServiceAnonUnderfillTest {

    @Test
    void anonUser_others30_returns30_whenCatalogEnough() {
        UserService userService = mock(UserService.class);
        ProductRepository productRepo = mock(ProductRepository.class);
        ProductService productService = mock(ProductService.class);
        EventLogRepository eventRepo = mock(EventLogRepository.class);
        UserInterestRepository interestRepo = mock(UserInterestRepository.class);
        NegativeFeedbackService negativeFeedbackService = mock(NegativeFeedbackService.class);

        when(userService.getCurrentUserOrNull()).thenReturn(null);

        Product base = new Product();
        base.setId(1L);
        base.setActive(true);
        base.setPrice(new BigDecimal("100000"));
        base.setBrand("brandA");

        when(productRepo.findById(1L)).thenReturn(Optional.of(base));

        // similar (we pass similarLimit=0 anyway, but keep safe)
        when(productRepo.activeByCategoryAndPriceBand(any(), any(), any(), anyList(), anyBoolean(), any()))
                .thenReturn(List.of());
        when(productRepo.activeByCategories(anyList(), anyList(), anyBoolean(), any()))
                .thenReturn(List.of());
        when(productRepo.activeByBrands(anyList(), anyList(), anyBoolean(), any()))
                .thenReturn(List.of());

        List<Product> pool = buildProducts(200, 2L);

        // IMPORTANT: service calls these with perBucket=30 (limit=30)
        when(productRepo.hitsShuffledExclude(anyString(), anyList(), anyInt(), anyInt()))
                .thenReturn(pool.subList(0, 60));
        when(productRepo.discountsShuffledExclude(anyString(), anyList(), anyInt(), anyInt()))
                .thenReturn(pool.subList(60, 120));
        when(productRepo.newArrivalsExclude(anyList(), anyInt(), anyInt()))
                .thenReturn(pool.subList(120, 160));
        when(productRepo.popularExclude(anyList(), anyInt(), any()))
                .thenReturn(new PageImpl<>(pool.subList(160, 200)));

        // ✅ CRITICAL: ProductRecommendService uses toCardsPublic(), not toCardPublic()
        when(productService.toCardsPublic(anyList(), isNull()))
                .thenAnswer(inv -> {
                    List<?> in = inv.getArgument(0);
                    List<ProductCardResponse> out = new ArrayList<>(in.size());
                    for (int i = 0; i < in.size(); i++) out.add(mock(ProductCardResponse.class));
                    return out;
                });

        ProductRecommendService svc = new ProductRecommendService(
                userService,
                productRepo,
                productService,
                eventRepo,
                interestRepo,
                negativeFeedbackService
        );

        ProductRecommendResponse res = svc.recommendForDetail(1L, 0, 30, "seed");

        // record accessor
        assertThat(res.others()).hasSize(30);

        // ✅ verify: agar stub urmasa, shu yerda darrov bilinadi
        verify(productRepo, atLeastOnce()).hitsShuffledExclude(anyString(), anyList(), anyInt(), anyInt());
        verify(productRepo, atLeastOnce()).discountsShuffledExclude(anyString(), anyList(), anyInt(), anyInt());
        verify(productRepo, atLeastOnce()).newArrivalsExclude(anyList(), anyInt(), anyInt());
        verify(productRepo, atLeastOnce()).popularExclude(anyList(), anyInt(), any());
        verify(productService, atLeastOnce()).toCardsPublic(anyList(), isNull());
    }

    private static List<Product> buildProducts(int count, long startId) {
        ArrayList<Product> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Product p = new Product();
            p.setId(startId + i);
            p.setActive(true);
            p.setBrand("brand" + (i % 50)); // diversity
            p.setPrice(new BigDecimal("100000"));
            p.setSoldCount(i);
            p.setViewCount(i * 2);
            p.setStock(10);
            list.add(p);
        }
        return list;
    }
}