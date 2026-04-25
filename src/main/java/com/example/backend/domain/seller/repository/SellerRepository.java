package com.example.backend.domain.seller.repository;

import com.example.backend.domain.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    // 1 user = 1 seller bo'lsa (owner_user_id unique)
    Optional<Seller> findByOwnerUserId(Long ownerUserId);

    // status bo'yicha topish (admin panel uchun)
// user'ning seller profili bor-yo'qligini tekshirish
    boolean existsByOwnerUserId(Long ownerUserId);
}