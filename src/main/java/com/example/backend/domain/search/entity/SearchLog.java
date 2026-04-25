package com.example.backend.domain.search.entity;

import com.example.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs")
@Getter
@Setter
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;
    private String normalizedKeyword;

    private Integer resultCount;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();
}

