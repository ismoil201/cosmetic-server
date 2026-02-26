package com.example.backend.controller;

import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.dto.SellerProductDetailResponse;
import com.example.backend.dto.SellerProductListResponse;
import com.example.backend.entity.Product;
import com.example.backend.service.SellerProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductService sellerProductService;

    // =============== LIST ===============
    @GetMapping
    public Page<SellerProductListResponse> myProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return sellerProductService.myProducts(PageRequest.of(page, size))
                .map(sellerProductService::toListResponse);
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