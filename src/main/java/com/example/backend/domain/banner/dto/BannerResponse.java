package com.example.backend.domain.banner.dto;

import com.example.backend.domain.banner.entity.BannerLinkType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private BannerLinkType linkType;
    private Long linkId;
    private Integer position;
}
