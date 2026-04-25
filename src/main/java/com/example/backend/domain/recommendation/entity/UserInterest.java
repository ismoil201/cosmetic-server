package com.example.backend.domain.recommendation.entity;
import com.example.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="user_interest",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","type","key_name"}),
        indexes = {
                @Index(name = "idx_user_interest_user_score", columnList = "user_id,score"),
                @Index(name = "idx_user_interest_user_type_score", columnList = "user_id,type,score")
        })
@Getter @Setter
public class UserInterest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=32)
    private InterestType type;

    @Column(name="key_name", nullable=false, length=128)
    private String key;

    @Column(nullable=false)
    private double score;

    // ✅ STEP 8: Track last event type that updated this interest
    @Column(name="last_event_type", length=32)
    private String lastEventType;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}