package com.example.backend.security;

import com.example.backend.repository.*;
import com.example.backend.service.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProductServiceEmptyIdsTest {

    @Test
    void getProductsByIds_whenEmpty_returnsEmpty_andDoesNotCallRepo() {
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

        var out = service.getProductsByIds(List.of());

        assertThat(out).isEmpty();
        verify(productRepo, never()).findByIdInAndActiveTrue(anyList());
        verifyNoInteractions(favRepo);
        verifyNoInteractions(productImageRepo);
    }
}