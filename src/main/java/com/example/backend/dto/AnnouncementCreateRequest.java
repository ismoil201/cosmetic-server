package com.example.backend.dto;

import com.example.backend.entity.NotificationType;

public record AnnouncementCreateRequest(
        String title,
        String content,
        NotificationType type
) {}
