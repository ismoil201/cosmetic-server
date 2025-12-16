package com.example.backend.repository;

import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 🏠 HOME (faqat active productlar)
    Page<Product> findByActiveTrue(Pageable pageable);

    // 🔎 Category filter
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    // 🔥 Brand filter
    Page<Product> findByBrandAndActiveTrue(String brand, Pageable pageable);

    // 💰 Sale products
    Page<Product> findByDiscountPriceGreaterThanAndActiveTrue(double price, Pageable pageable);
}
