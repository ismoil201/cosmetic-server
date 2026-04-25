package com.example.backend.domain.announcement.dto;

import com.example.backend.domain.notification.entity.NotificationType;

public record AnnouncementCreateRequest(
        String title,
        String content,
        NotificationType type
) {}
