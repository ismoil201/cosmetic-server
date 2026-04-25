package com.example.backend.domain.admin.controller;


import com.example.backend.domain.admin.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    // 🛡️ BLOCK
    @PutMapping("/{id}/block")
    public String block(@PathVariable Long id) {
        adminReviewService.setActive(id, false);
        return "Review blocked";
    }

    // 🛡️ UNBLOCK
    @PutMapping("/{id}/unblock")
    public String unblock(@PathVariable Long id) {
        adminReviewService.setActive(id, true);
        return "Review unblocked";
    }

    // ❌ DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        adminReviewService.delete(id);
        return "Review deleted";
    }
}
