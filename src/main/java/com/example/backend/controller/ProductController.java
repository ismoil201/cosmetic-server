package com.example.backend.controller;

import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.dto.ProductDetailResponse;
import com.example.backend.dto.ProductResponse;
import com.example.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> list(Pageable pageable) {
        return productService.getHomeProducts(pageable).getContent();
    }

    @GetMapping("/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.getDetail(id);
    }
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductCreateRequest req) {
        productService.create(req);
        return ResponseEntity.ok("Created");
    }
//
//    // ✅ HOME (ANDROIDGA MOS)
//    @GetMapping
//    public List<ProductResponse> list(Pageable pageable) {
//        return productService
//                .getHomeProducts(pageable)
//                .getContent(); // ⭐ MUHIM
//    }
//
//    // DETAIL
//    @GetMapping("/{id}")
//    public ProductDetailResponse detail(@PathVariable Long id) {
//        return productService.getDetail(id);
//    }

    // CREATE (ADMIN)
//    @PostMapping
//    public ResponseEntity<?> create(@RequestBody ProductCreateRequest req) {
//        productService.create(req);
//        return ResponseEntity.ok("Created");
//    }

    // UPDATE (ADMIN)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody ProductCreateRequest req
    ) {
        productService.update(id, req);
        return ResponseEntity.ok("Updated");
    }

    // DELETE (ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok("Deleted");
    }
}
