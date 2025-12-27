package com.example.backend.repository;

import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ ADMIN
    Page<Product> findByActive(boolean active, Pageable pageable);

    // 🏠 HOME
    Page<Product> findByActiveTrue(Pageable pageable);

    // 🔎 CATEGORY
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    // 🔥 BRAND
    Page<Product> findByBrandAndActiveTrue(String brand, Pageable pageable);

    // 💰 SALE (discount bor)
    Page<Product> findByDiscountPriceGreaterThanAndActiveTrue(double discountPrice, Pageable pageable);
}
