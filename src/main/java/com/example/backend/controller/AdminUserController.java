package com.example.backend.controller;

import com.example.backend.dto.AdminUserResponse;
import com.example.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // 👥 USER LIST
    // /api/admin/users
    // /api/admin/users?active=true
    // /api/admin/users?active=false
    @GetMapping
    public Page<AdminUserResponse> list(
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return adminUserService.list(active, pageable);
    }

    // 🚫 BLOCK
    @PutMapping("/{id}/block")
    public ResponseEntity<?> block(@PathVariable Long id) {
        adminUserService.block(id);
        return ResponseEntity.ok("User blocked");
    }

    // ✅ UNBLOCK
    @PutMapping("/{id}/unblock")
    public ResponseEntity<?> unblock(@PathVariable Long id) {
        adminUserService.unblock(id);
        return ResponseEntity.ok("User unblocked");
    }

    // 🔁 ROLE CHANGE
    // ?role=ADMIN yoki ?role=USER
    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeRole(
            @PathVariable Long id,
            @RequestParam String role
    ) {
        adminUserService.changeRole(id, role);
        return ResponseEntity.ok("Role updated");
    }
}
