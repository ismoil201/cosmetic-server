package com.example.backend.repository;

import com.example.backend.entity.Order;
import com.example.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);
    List<OrderItem> findByOrderId(Long orderId);
    
    // ✅ Batch fetch order items for multiple orders
    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    // ✅ seller cancel bo'lganda faqat o'sha sub-order itemlari kerak
    List<OrderItem> findBySellerOrderId(Long sellerOrderId);

}
