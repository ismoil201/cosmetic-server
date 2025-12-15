package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private double price;              // original price
    private double discountPrice;      // sale price

    private String imageUrl;

    private String brand;              // ⭐ Musinsa core

    @Enumerated(EnumType.STRING)
    private Category category;

    private int stock;                 // ❗ sold out check
    private boolean active = true;     // admin hide/show

    private int viewCount = 0;         // popular products

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
