package com.example.backend.domain.order.entity;

import com.example.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 👤 Kim buyurtma qildi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 🔗 FK (history / analytics uchun)
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    // 📍 SNAPSHOT
    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    // 📞 SNAPSHOT
    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    // 💰 MONEY
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
