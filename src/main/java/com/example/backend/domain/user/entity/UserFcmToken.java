package com.example.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_fcm_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = "token")
)@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 255, unique = true)
    private String token;

    private LocalDateTime createdAt = LocalDateTime.now();
}
