package com.example.backend.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subtitle;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false)
    private BannerLinkType linkType = BannerLinkType.NONE;

    @Column(name = "link_id")
    private Long linkId;

    @Column(nullable = false)
    private Integer position = 0;

    @Column(nullable = false)
    private Boolean active = true;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
