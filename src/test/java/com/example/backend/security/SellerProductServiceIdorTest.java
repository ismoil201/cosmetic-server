package com.example.backend.security;

import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import com.example.backend.service.SellerProductService;
import com.example.backend.service.SellerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerProductServiceIdorTest {

    @Test
    void myProductDetail_whenNotOwned_throwsNotFound() {
        ProductRepository productRepo = mock(ProductRepository.class);
        ProductImageRepository productImageRepo = mock(ProductImageRepository.class);
        ProductDetailImageRepository detailImageRepo = mock(ProductDetailImageRepository.class);
        ProductVariantRepository variantRepo = mock(ProductVariantRepository.class);
        VariantTierPriceRepository tierRepo = mock(VariantTierPriceRepository.class);
        SellerService sellerService = mock(SellerService.class);

        SellerProductService service = new SellerProductService(
                productRepo,
                productImageRepo,
                detailImageRepo,
                variantRepo,
                tierRepo,
                sellerService
        );

        when(sellerService.requireCurrentSellerId()).thenReturn(10L);
        when(productRepo.findByIdAndSellerId(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.myProductDetail(999L))
                .isInstanceOf(NotFoundException.class);

        verify(productRepo).findByIdAndSellerId(999L, 10L);
    }

    @Test
    void myProducts_usesSellerIdFilter() {
        ProductRepository productRepo = mock(ProductRepository.class);
        ProductImageRepository productImageRepo = mock(ProductImageRepository.class);
        ProductDetailImageRepository detailImageRepo = mock(ProductDetailImageRepository.class);
        ProductVariantRepository variantRepo = mock(ProductVariantRepository.class);
        VariantTierPriceRepository tierRepo = mock(VariantTierPriceRepository.class);
        SellerService sellerService = mock(SellerService.class);

        SellerProductService service = new SellerProductService(
                productRepo,
                productImageRepo,
                detailImageRepo,
                variantRepo,
                tierRepo,
                sellerService
        );

        when(sellerService.requireCurrentSellerId()).thenReturn(10L);
        service.myProducts(Pageable.ofSize(20));

        verify(productRepo).findBySellerIdAndActiveTrueOrderByCreatedAtDesc(eq(10L), any(Pageable.class));
    }
}