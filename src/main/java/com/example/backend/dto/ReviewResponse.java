package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private String userName;
    private int rating;
    private String content;
    private LocalDateTime createdAt;

    // 🔥 Review rasmlari
    private List<String> imageUrls;
}

