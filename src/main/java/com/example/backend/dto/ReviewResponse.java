package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private String userName;      // review yozgan user
    private int rating;
    private String content;
    private LocalDateTime createdAt;
}
