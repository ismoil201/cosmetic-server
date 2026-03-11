package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sellers",
        uniqueConstraints = {
                // Agar 1 user = 1 seller bo'lsa juda foydali
                @UniqueConstraint(name = "uk_sellers_owner_user", columnNames = {"owner_user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 seller egasi (account)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerStatus status = SellerStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum SellerStatus {
        PENDING, ACTIVE, BLOCKED
    }
}