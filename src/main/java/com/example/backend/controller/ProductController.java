package com.example.backend.controller;

import com.example.backend.dto.ProductDto;
import com.example.backend.entity.Product;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repo;

    // CREATE
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody ProductDto dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setImageUrl(dto.getImageUrl());
        p.setCategory(dto.getCategory());

        return ResponseEntity.ok(repo.save(p));
    }

    // GET ALL
    @GetMapping
    public List<Product> all() {
        return repo.findAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return ResponseEntity.ok(p);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @RequestBody ProductDto dto
    ) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setImageUrl(dto.getImageUrl());
        p.setCategory(dto.getCategory());

        return ResponseEntity.ok(repo.save(p));
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok("Deleted!");
    }

}
