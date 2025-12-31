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


    List<Product> findByIdInAndActiveTrue(List<Long> ids);


    // 🔥 MUHIM: todayDeal bilan ishlaydigan QUERY
    @Modifying
    @Transactional
    @Query("update Product p set p.isTodayDeal = false where p.isTodayDeal = true")
    void clearTodayDeals();

    List<Product> findByIsTodayDealTrueAndActiveTrue();

    // HIT (bugun xit)
    List<Product> findTop10ByIsTodayDealTrueAndActiveTrueOrderByCreatedAtDesc();

    // limit dinamik bo‘lishi uchun Pageable bilan:
    Page<Product> findByIsTodayDealTrueAndActiveTrue(Pageable pageable);

    // DISCOUNT (discount_price bor)
    Page<Product> findByDiscountPriceIsNotNullAndActiveTrue(Pageable pageable);

    // NEW ARRIVALS (eng yangi)
    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    // POPULAR (view_count yoki sold_count bo‘yicha)
    Page<Product> findByActiveTrue(Pageable pageable);



    @Query("""
    select p from Product p
    where p.active=true
      and p.discountPrice is not null
      and p.discountPrice < p.price
""")
    Page<Product> findDiscounted(Pageable pageable);



    @Query("""
    select p from Product p
    where (:active is null or p.active = :active)
      and (:category is null or p.category = :category)
      and (:brand is null or lower(p.brand) like lower(concat('%', :brand, '%')))
      and (:todayDeal is null or p.isTodayDeal = :todayDeal)
      and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
""")
    Page<Product> adminSearch(
            Boolean active,
            Category category,
            String brand,
            Boolean todayDeal,
            String keyword,
            Pageable pageable
    );

}
