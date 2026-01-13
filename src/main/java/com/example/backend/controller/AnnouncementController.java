package com.example.backend.controller;

import com.example.backend.entity.Announcement;
import com.example.backend.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementRepository repo;

    @GetMapping
    public List<Announcement> activeAnnouncements() {
        return repo.findByActiveTrueOrderByCreatedAtDesc();
    }
}
