package com.example.backend.repository;

import com.example.backend.entity.SellerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerOrderRepository extends JpaRepository<SellerOrder, Long> {

    // Seller o'z orderlarini ko'rishi uchun
    Page<SellerOrder> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    // Masalan: faqat NEW/CONFIRMED larni ko'rsatish
    Page<SellerOrder> findBySellerIdAndStatusOrderByCreatedAtDesc(
            Long sellerId,
            SellerOrder.SellerOrderStatus status,
            Pageable pageable
    );

    // Bir master order ichida ma'lum seller sub-orderini topish (split yaratishda ham kerak bo'ladi)
    Optional<SellerOrder> findByOrderIdAndSellerId(Long orderId, Long sellerId);

    // Master orderga tegishli hamma seller_orders
    List<SellerOrder> findByOrderId(Long orderId);

    // Security: seller o'ziga tegishli orderni olishyapti-mi tekshirish
    boolean existsByIdAndSellerId(Long sellerOrderId, Long sellerId);
}