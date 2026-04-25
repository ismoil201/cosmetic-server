package com.example.backend.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants",
        indexes = {
                @Index(name="idx_variant_product", columnList = "product_id"),
                @Index(name="idx_variant_active", columnList = "active")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProductVariant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_id", nullable=false)
    private Product product;

    @Column(nullable = false, length = 80)
    private String label; // "30ml", "50ml", "80ml"

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name="discount_price", precision = 15, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    private int stock = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name="sort_order")
    private int sortOrder = 0;

    // 🔒 CRITICAL: Optimistic locking for concurrent stock updates
    @Version
    private Long version;
}