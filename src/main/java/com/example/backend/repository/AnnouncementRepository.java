package com.example.backend.repository;

import com.example.backend.entity.Announcement;
import com.example.backend.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();

    Page<Announcement> findByActive(Boolean active, Pageable pageable);

    Page<Announcement> findByType(NotificationType type, Pageable pageable);

    Page<Announcement> findByActiveAndType(
            Boolean active,
            NotificationType type,
            Pageable pageable
    );
}
