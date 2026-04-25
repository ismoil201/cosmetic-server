package com.example.backend.security;

import com.example.backend.domain.recommendation.service.InterestService;
import com.example.backend.domain.event.service.EventTrackingService;
import com.example.backend.domain.favorite.repository.FavoriteRepository;
import com.example.backend.domain.product.repository.*;
import com.example.backend.domain.product.service.ProductService;
import com.example.backend.domain.search.repository.SearchLogRepository;
import com.example.backend.domain.seller.service.SellerService;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.domain.product.dto.ProductResponse;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HomeBatchRegressionTest {

    @Test
    void getHomeProducts_shouldUseBatchQueries_notNPlus1() {
        ProductRepository productRepo = mock(ProductRepository.class);
        FavoriteRepository favRepo = mock(FavoriteRepository.class);
        UserService userService = mock(UserService.class);
        ProductImageRepository productImageRepo = mock(ProductImageRepository.class);
        ProductDetailImageRepository detailImageRepo = mock(ProductDetailImageRepository.class);
        InterestService interestService = mock(InterestService.class);
        EventTrackingService eventTrackingService = mock(EventTrackingService.class);
        SellerService sellerService = mock(SellerService.class);
        SearchLogRepository searchLogRepo = mock(SearchLogRepository.class);
        ProductVariantRepository variantRepo = mock(ProductVariantRepository.class);
        VariantTierPriceRepository tierRepo = mock(VariantTierPriceRepository.class);

        ProductService service = new ProductService(
                productRepo, favRepo, userService, productImageRepo, detailImageRepo,
                interestService, eventTrackingService, sellerService, searchLogRepo, variantRepo, tierRepo
        );

        User u = new User();
        u.setId(1L);
        when(userService.getCurrentUserOrNull()).thenReturn(u);

        Product p1 = new Product(); p1.setId(10L); p1.setActive(true);
        Product p2 = new Product(); p2.setId(11L); p2.setActive(true);

        when(productRepo.findByActiveTrue(any()))
                .thenReturn(new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 20), 2));

        when(favRepo.findFavoriteProductIds(eq(u), anyList()))
                .thenReturn(List.of(10L));

        when(productImageRepo.findByProductIdInOrderByMainDescIdAsc(anyList()))
                .thenReturn(List.of()); // not needed for this assert

        var page = service.getHomeProducts(PageRequest.of(0, 20));

        List<ProductResponse> content = page.getContent();
        assertThat(content).hasSize(2);

        verify(favRepo, times(1)).findFavoriteProductIds(eq(u), anyList());
        verify(productImageRepo, times(1)).findByProductIdInOrderByMainDescIdAsc(anyList());

        // N+1 signals must NOT happen:
        verify(favRepo, never()).existsByUserAndProduct(any(), any());
        verify(productImageRepo, never()).findByProductId(anyLong());
    }
}