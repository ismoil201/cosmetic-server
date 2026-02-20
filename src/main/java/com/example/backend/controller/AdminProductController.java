package com.example.backend.controller;

import com.example.backend.dto.AdminProductDetailResponse;
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



    @GetMapping
    public Page<AdminProductResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean todayDeal,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        return adminProductService.list(
                active, category, brand, todayDeal, keyword, pageable
        );
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductCreateRequest req) {
        adminProductService.create(req);
        return ResponseEntity.ok("Created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody ProductCreateRequest req
    ) {
        adminProductService.update(id, req);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDelete(@PathVariable Long id) {
        adminProductService.softDelete(id);
        return ResponseEntity.ok("Deactivated");
    }


    @GetMapping("/{id}")
    public AdminProductDetailResponse detail(@PathVariable Long id) {
        return adminProductService.detail(id);
    }
    @PutMapping("/{id}/active")
    public ResponseEntity<?> setActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        adminProductService.setActive(id, active);
        return ResponseEntity.ok("Active updated");
    }

    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        adminProductService.toggleActive(id); // yangi method
        return ResponseEntity.ok("Active toggled");
    }



    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        adminProductService.restore(id);
        return ResponseEntity.ok("Restored");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<?> hardDelete(@PathVariable Long id) {
        adminProductService.hardDelete(id);
        return ResponseEntity.ok("Product permanently deleted");
    }
    // 🔥 TODAY DEAL (BITTA)
    @PutMapping("/{id}/today-deal")
    public ResponseEntity<?> setTodayDeal(@PathVariable Long id) {
        adminProductService.setSingleTodayDeal(id);
        return ResponseEntity.ok("Today deal updated");
    }
}
