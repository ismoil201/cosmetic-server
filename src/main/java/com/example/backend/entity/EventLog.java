package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_logs",
        indexes = {
                @Index(name = "idx_event_user", columnList = "user_id"),
                @Index(name = "idx_event_product", columnList = "product_id"),
                @Index(name = "idx_event_type", columnList = "event_type"),
                @Index(name = "idx_event_created", columnList = "created_at")
        }
)
@Getter
@Setter
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user; // nullable

    @Column(name="session_id")
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name="event_type", nullable=false, length=32)
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="product_id")
    private Product product; // nullable

    @Column(length=32)
    private String category; // product bo‘lmasa ham log qilish mumkin

    @Column(length=128)
    private String brand;

    @Column(name="query_text", length=255)
    private String queryText;

    private Integer position;

    @Column(length=32)
    private String screen; // HOME/SEARCH/DETAIL

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();
}