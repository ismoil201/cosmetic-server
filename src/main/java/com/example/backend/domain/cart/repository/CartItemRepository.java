package com.example.backend.domain.cart.repository;

import com.example.backend.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {



    List<CartItem> findByUserIdAndIdIn(Long userId, List<Long> ids);


    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);


    List<CartItem> findByUserId(Long userId);
    Optional<CartItem> findByUserIdAndVariantId(Long userId, Long variantId);
    void deleteByUserId(Long userId);
}
