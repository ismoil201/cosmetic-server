package com.example.backend.controller;

import com.example.backend.dto.ProductDto;
import com.example.backend.dto.ProductResponse;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.repository.FavoriteRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final FavoriteRepository favRepo;

    // CREATE
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductDto dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setImageUrl(dto.getImageUrl());
        p.setCategory(dto.getCategory());

        return ResponseEntity.ok(productRepo.save(p));
    }

    // GET ALL (HOME)
    @GetMapping
    public List<Product> all() {
        return productRepo.findAll();
    }

    // ✅ GET BY ID + FAVORITE CHECK
    @GetMapping("/{id}")
    public ProductResponse getById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId
    ) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        boolean favorite = false;

        if (userId != null) {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            favorite = favRepo.findByUserAndProduct(user, product).isPresent();
        }

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory(),
                favorite
        );
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @RequestBody ProductDto dto
    ) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setImageUrl(dto.getImageUrl());
        p.setCategory(dto.getCategory());

        return ResponseEntity.ok(productRepo.save(p));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        productRepo.deleteById(id);
        return ResponseEntity.ok("Deleted!");
    }
}
