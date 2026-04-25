package com.example.backend.domain.seller.controller;

import com.example.backend.global.config.OpenApiConfig;
import com.example.backend.domain.product.dto.ProductCreateRequest;
import com.example.backend.domain.seller.dto.SellerProductDetailResponse;
import com.example.backend.domain.seller.dto.SellerProductListResponse;
import com.example.backend.domain.seller.service.SellerProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Seller Products", description = "Seller product management endpoints")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@RestController
@RequestMapping("/api/seller/products")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductService sellerProductService;

    // =============== LIST ===============

    @Operation(summary = "My products", description = "Get products owned by the authenticated seller")
    @GetMapping
    public Page<SellerProductListResponse> myProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerProductService.myProducts(PageRequest.of(page, size));
    }
    // =============== DETAIL ===============
    @GetMapping("/{id}")
    public SellerProductDetailResponse detail(@PathVariable Long id) {
        return sellerProductService.toDetailResponse(
                sellerProductService.myProductDetail(id)
        );
    }

    // =============== CREATE ===============
    @PostMapping
    public Long create(@RequestBody ProductCreateRequest req) {
        return sellerProductService.create(req);
    }

    // =============== UPDATE ===============
    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody ProductCreateRequest req) {
        sellerProductService.update(id, req);
    }

    // =============== DELETE (soft) ===============
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sellerProductService.delete(id);
    }
}