package com.example.backend.controller;

import com.example.backend.dto.AdminAnnouncementResponse;
import com.example.backend.dto.AnnouncementCreateRequest;
import com.example.backend.service.AdminAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {

    private final AdminAnnouncementService service;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AnnouncementCreateRequest req) {

        service.publish(
                req.title(),
                req.content(),
                req.type()
        );

        return ResponseEntity.ok("Announcement sent");
    }

    // 📋 LIST
    @GetMapping
    public Page<AdminAnnouncementResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String type,
            Pageable pageable
    ) {
        return service.list(active, type, pageable);
    }

    // ❌ SOFT DELETE
    @PutMapping("/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }

    // ♻️ RESTORE
    @PutMapping("/{id}/restore")
    public void restore(@PathVariable Long id) {
        service.restore(id);
    }

    // 🗑 HARD DELETE (agar kerak bo‘lsa)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
