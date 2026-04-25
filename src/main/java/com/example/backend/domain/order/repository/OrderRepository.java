package com.example.backend.domain.order.repository;

import com.example.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 👤 Order history
    List<Order> findByUserId(Long userId);
}
