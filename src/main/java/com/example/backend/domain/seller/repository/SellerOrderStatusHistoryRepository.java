package com.example.backend.domain.seller.repository;

import com.example.backend.domain.seller.entity.SellerOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerOrderStatusHistoryRepository
        extends JpaRepository<SellerOrderStatusHistory, Long> {

    // Bitta seller_order tarixini vaqt bo'yicha chiqarish
    List<SellerOrderStatusHistory> findBySellerOrderIdOrderByCreatedAtAsc(Long sellerOrderId);

    // Eng oxirgi status (kerak bo'lib qoladi)
    SellerOrderStatusHistory findTop1BySellerOrderIdOrderByCreatedAtDesc(Long sellerOrderId);
}
