package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "variant_tier_prices",
        uniqueConstraints = @UniqueConstraint(name="uk_variant_min_qty", columnNames = {"variant_id","min_qty"}),
        indexes = @Index(name="idx_tier_variant", columnList = "variant_id"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VariantTierPrice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="variant_id", nullable=false)
    private ProductVariant variant;

    @Column(name="min_qty", nullable=false)
    private int minQty; // 1,2,3...

    @Column(name="total_price", precision=15, scale=2, nullable=false)
    private BigDecimal totalPrice; // qty >= minQty bo‘lsa shu TOTAL ishlaydi
}