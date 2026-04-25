package com.example.backend.domain.admin.dto;

import com.example.backend.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record AdminAnnouncementResponse(
        Long id,
        String title,
        String content,
        NotificationType type,
        boolean active,
        LocalDateTime createdAt
) {}
