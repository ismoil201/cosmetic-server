package com.example.backend.controller;

import com.example.backend.dto.ProductCardResponse;
import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.dto.ProductDetailResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.Category;
import com.example.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    /**
     * PUBLIC: Product list (pagination)
     * Example: /api/products?page=0&size=20
     */
    // 🔓 PUBLIC (TOKEN KERAK EMAS)
    @GetMapping
    public Page<ProductResponse> list(Pageable pageable) {
        return productService.getHomeProducts(pageable);
    }
    /**
     * PUBLIC: Product detail
     * Example: /api/products/12
     */
    // 🔓 PUBLIC (TOKEN KERAK EMAS)
    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.getDetail(id);
    }

    @GetMapping("/category")
    public Page<ProductCardResponse> byCategory(
            @RequestParam String category,
            Pageable pageable
    ) {
        Category cat = Category.valueOf(category.toUpperCase());
        return productService.getByCategoryCards(cat, pageable);
    }


    /**
     * PUBLIC: Today deals list
     * Example: /api/products/today-deals
     */
    @GetMapping("/today-deals")
    public List<ProductResponse> todayDeals() {
        return productService.getTodayDeals();
    }

    /**
     * PUBLIC: Get products by ids (for cart/favorites local list)
     * Example: POST /api/products/by-ids
     * Body: [1,2,3]
     */
    @PostMapping("/by-ids")
    public List<ProductResponse> getByIds(@RequestBody List<Long> ids) {
        return productService.getProductsByIds(ids);
    }

    // ================= ADMIN =================

    /**
     * ADMIN: Create product
     */

    // 🔐 ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductCreateRequest req) {
        productService.create(req);
        return ResponseEntity.ok("Created");
    }

    // 🔐 ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody ProductCreateRequest req
    ) {
        productService.update(id, req);
        return ResponseEntity.ok("Updated");
    }

    @GetMapping("/search")
    public Page<ProductCardResponse> search(
            @RequestParam String q,
            Pageable pageable
    ) {
        return productService.search(q, pageable);
    }

    // 🔐 ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok("Deleted");
    }


}
