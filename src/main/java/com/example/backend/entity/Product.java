package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private double price;

    @Column(name = "discount_price")
    private double discountPrice;


    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private int stock;

    private boolean active = true;

    @Column(name = "view_count")
    private int viewCount = 0;

    // 🔥 DB DA BOR, OLDIN YO‘Q EDI
    @Column(name = "rating_avg")
    private double ratingAvg = 0.0;

    @Column(name = "review_count")
    private int reviewCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

