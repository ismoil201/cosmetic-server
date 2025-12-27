package com.example.backend.repository;

import com.example.backend.entity.Order;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 👤 Order history
    List<Order> findByUserId(Long userId);
}
