package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = true)
    private String password; // firebase userlarda null bo‘ladi


    private String fullName;

    private String phone;              // 📱 buyurtma uchun
    private String profileImage;       // 👤 avatar

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;                // USER / ADMIN

    private boolean active = true;     // ❗ user bloklash uchun

    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    private AuthProvider provider; // GOOGLE, APPLE, PHONE

    private String providerId; // firebase uid

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
