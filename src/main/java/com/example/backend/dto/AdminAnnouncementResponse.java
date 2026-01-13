package com.example.backend.dto;

import com.example.backend.entity.NotificationType;

import java.time.LocalDateTime;

public record AdminAnnouncementResponse(
        Long id,
        String title,
        String content,
        NotificationType type,
        boolean active,
        LocalDateTime createdAt
) {}
