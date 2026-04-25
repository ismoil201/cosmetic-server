package com.example.backend.domain.banner.repository;
import com.example.backend.domain.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Android uchun: active + (start/end) valid bo‘lgan bannerlar
    List<Banner> findByActiveTrueOrderByPositionAsc();

    // Agar start/end ham tekshirmoqchi bo‘lsangiz (custom query tavsiya):
    // pastda Service’da @Query bilan qilamiz
}
