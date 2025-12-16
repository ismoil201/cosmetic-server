package com.example.backend.controller;

import com.example.backend.dto.AdminProductResponse;
import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final AdminProductService adminProductService;

    // ✅ LIST (active filter optional)
    // /api/admin/products
    // /api/admin/products?active=true
    // /api/admin/products?active=false
    @GetMapping
    public Page<AdminProductResponse> list(
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return adminProductService.list(active, pageable);
    }

    // ✅ CREATE
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductCreateRequest req) {
        adminProductService.create(req);
        return ResponseEntity.ok("Created");
    }

    // ✅ UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody ProductCreateRequest req) {
        adminProductService.update(id, req);
        return ResponseEntity.ok("Updated");
    }

    // ✅ SOFT DELETE (active=false)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        adminProductService.softDelete(id);
        return ResponseEntity.ok("Deactivated");
    }

    // ✅ RESTORE (active=true)
    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        adminProductService.restore(id);
        return ResponseEntity.ok("Restored");
    }
}
