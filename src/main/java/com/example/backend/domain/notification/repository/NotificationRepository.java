package com.example.backend.domain.notification.repository;

import com.example.backend.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ✅ TO‘G‘RI
    long countByUserIdAndIsReadFalse(Long userId);
}

