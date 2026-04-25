package com.example.backend.domain.product.entity;

import com.example.backend.domain.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_category", columnList = "category"),
                @Index(name = "idx_products_brand", columnList = "brand"),
                @Index(name = "idx_products_sold_count", columnList = "sold_count"),
                @Index(name = "idx_products_created_at", columnList = "created_at"),
                @Index(name = "idx_products_active", columnList = "active"),
                // ✅ CRITICAL: Search text index (helps LIKE queries)
                @Index(name = "idx_products_search_text", columnList = "search_text"),
                // ✅ CRITICAL: Composite index for search query optimization
                @Index(name = "idx_products_active_sold_view", columnList = "active,sold_count,view_count")
        }
)
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

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_price", precision = 15, scale = 2)
    private BigDecimal discountPrice;

    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private int stock;

    private boolean active = true;

    @Column(name = "view_count")
    private int viewCount = 0;

    // 🔥 DB DA BOR, OLDIN YO‘Q EDI
    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;


    @Column(name = "review_count")
    private int reviewCount = 0;

    @Column(name = "sold_count")
    private int soldCount = 0;

    @Column(name = "is_today_deal")
    private boolean isTodayDeal = false;

    @Column(name = "search_text", length = 500)
    private String searchText;

    // 🔗 Marketplace: product qaysi sellerga tegishli
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    // 🔥 OPTIMISTIC LOCKING (ENG MUHIM QATOR)
    @Version
    private Long version;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

