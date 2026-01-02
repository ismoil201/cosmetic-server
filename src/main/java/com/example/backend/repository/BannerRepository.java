package com.example.backend.repository;
import com.example.backend.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    // Android uchun: active + (start/end) valid bo‘lgan bannerlar
    List<Banner> findByActiveTrueOrderByPositionAsc();

    // Agar start/end ham tekshirmoqchi bo‘lsangiz (custom query tavsiya):
    // pastda Service’da @Query bilan qilamiz
}
