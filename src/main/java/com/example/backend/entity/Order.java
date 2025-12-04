package com.example.backend.entity;

import com.example.backend.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String address;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private double totalAmount;
}
