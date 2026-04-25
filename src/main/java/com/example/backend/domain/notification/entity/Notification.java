package com.example.backend.domain.notification.entity;

import com.example.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 👤 Qaysi userga
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 📌 Sarlavha
    @Column(nullable = false)
    private String title;

    // 📝 Matn
    @Column(nullable = false, length = 1000)
    private String message;

    // 🔔 TUR
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 📦 Bog‘langan obyekt (orderId, productId)
    @Column(name = "ref_id")
    private Long refId;

    // 👁 O‘qilganmi
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
