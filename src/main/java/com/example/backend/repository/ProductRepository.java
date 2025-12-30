package com.example.backend.repository;

import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Long> {

    // ✅ ADMIN
    Page<Product> findByActive(boolean active, Pageable pageable);

    // 🏠 HOME
    Page<Product> findByActiveTrue(Pageable pageable);


    List<Product> findByIdInAndActiveTrue(List<Long> ids);


    // 🔥 MUHIM: todayDeal bilan ishlaydigan QUERY
    @Modifying
    @Transactional
    @Query("update Product p set p.isTodayDeal = false where p.isTodayDeal = true")
    void clearTodayDeals();

    List<Product> findByIsTodayDealTrueAndActiveTrue();



    // 🔎 CATEGORY
    Page<Product> findByCategoryAndActiveTrue(Category category, Pageable pageable);

    // 🔥 BRAND
    Page<Product> findByBrandAndActiveTrue(String brand, Pageable pageable);

    // 💰 SALE (discount bor)
    Page<Product> findByDiscountPriceGreaterThanAndActiveTrue(double discountPrice, Pageable pageable);
}
