package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Data
@Table(name = "cart_items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"password"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER) // 🔥 MUHIM
    private Product product;

    private int quantity;
}

