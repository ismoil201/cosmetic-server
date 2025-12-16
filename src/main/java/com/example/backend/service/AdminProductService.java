package com.example.backend.service;

import com.example.backend.dto.AdminProductResponse;
import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepo;

    // ✅ ADMIN LIST (active filter optional)
    public Page<AdminProductResponse> list(Boolean active, Pageable pageable) {

        Page<Product> page = (active == null)
                ? productRepo.findAll(pageable)
                : productRepo.findByActive(active, pageable);

        return page.map(p -> new AdminProductResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getStock(),
                p.isActive(),
                p.getCategory()
        ));
    }

    // ✅ ADMIN CREATE
    public void create(ProductCreateRequest req) {
        Product p = new Product();
        map(req, p);
        productRepo.save(p);
    }

    // ✅ ADMIN UPDATE
    public void update(Long id, ProductCreateRequest req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        map(req, p);
        productRepo.save(p);
    }

    // ✅ ADMIN SOFT DELETE (active=false)
    public void softDelete(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(false);
        productRepo.save(p);
    }

    // ✅ ADMIN RESTORE (active=true)
    public void restore(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(true);
        productRepo.save(p);
    }

    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setImageUrl(req.getImageUrl());
        p.setStock(req.getStock());

        try {
            p.setCategory(Category.valueOf(req.getCategory().toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid category: " + req.getCategory());
        }
    }
}
