package com.example.backend.dto;

import com.example.backend.entity.BannerLinkType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerRequest {
    private String title;
    private String subtitle;
    private String imageUrl;

    private BannerLinkType linkType; // NONE/PRODUCT/CATEGORY
    private Long linkId;

    private Integer position;
    private Boolean active;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
