package com.example.backend.domain.banner.service;

import com.example.backend.domain.banner.dto.BannerRequest;
import com.example.backend.domain.banner.dto.BannerResponse;
import com.example.backend.domain.banner.entity.Banner;
import com.example.backend.domain.banner.entity.BannerLinkType;
import com.example.backend.domain.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository repo;

    // Android: faqat ko‘rinadigan bannerlar
    public List<BannerResponse> activeBanners() {
        LocalDateTime now = LocalDateTime.now();

        // startAt/endAt null bo‘lsa -> cheklov yo‘q deb hisoblaymiz
        return repo.findByActiveTrueOrderByPositionAsc().stream()
                .filter(b -> (b.getStartAt() == null || !now.isBefore(b.getStartAt())))
                .filter(b -> (b.getEndAt() == null || !now.isAfter(b.getEndAt())))
                .map(this::toResponse)
                .toList();
    }

    // Admin: hammasi
    public List<Banner> all() {
        return repo.findAll();
    }

    public Banner create(BannerRequest req) {
        BannerLinkType linkType = req.getLinkType() == null ? BannerLinkType.NONE : req.getLinkType();

        // PRODUCT/CATEGORY bo‘lsa linkId majburiy
        if (linkType != BannerLinkType.NONE && req.getLinkId() == null) {
            throw new IllegalArgumentException("linkId majburiy (linkType PRODUCT/CATEGORY bo‘lsa)");
        }

        Banner banner = Banner.builder()
                .title(req.getTitle())
                .subtitle(req.getSubtitle())
                .imageUrl(req.getImageUrl())
                .linkType(linkType)
                .linkId(req.getLinkId())
                .position(req.getPosition() == null ? 0 : req.getPosition())
                .active(req.getActive() == null ? true : req.getActive())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .createdAt(LocalDateTime.now())
                .build();

        return repo.save(banner);
    }

    public Banner update(Long id, BannerRequest req) {
        Banner b = repo.findById(id).orElseThrow(() -> new RuntimeException("Banner topilmadi: " + id));

        if (req.getTitle() != null) b.setTitle(req.getTitle());
        if (req.getSubtitle() != null) b.setSubtitle(req.getSubtitle());
        if (req.getImageUrl() != null) b.setImageUrl(req.getImageUrl());

        if (req.getLinkType() != null) {
            b.setLinkType(req.getLinkType());
        }
        if (req.getLinkId() != null) {
            b.setLinkId(req.getLinkId());
        }

        if (b.getLinkType() != BannerLinkType.NONE && b.getLinkId() == null) {
            throw new IllegalArgumentException("linkId majburiy (linkType PRODUCT/CATEGORY bo‘lsa)");
        }

        if (req.getPosition() != null) b.setPosition(req.getPosition());
        if (req.getActive() != null) b.setActive(req.getActive());

        if (req.getStartAt() != null) b.setStartAt(req.getStartAt());
        if (req.getEndAt() != null) b.setEndAt(req.getEndAt());

        return repo.save(b);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private BannerResponse toResponse(Banner b) {
        return BannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .subtitle(b.getSubtitle())
                .imageUrl(b.getImageUrl())
                .linkType(b.getLinkType())
                .linkId(b.getLinkId())
                .position(b.getPosition())
                .build();
    }
}
