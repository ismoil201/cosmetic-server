package com.example.backend.domain.admin.controller;
import com.example.backend.domain.banner.dto.BannerRequest;
import com.example.backend.domain.banner.entity.Banner;
import com.example.backend.domain.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBannerController {

    private final BannerService service;

    @GetMapping
    public List<Banner> all() {
        return service.all();
    }

    @PostMapping
    public Banner create(@RequestBody BannerRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public Banner update(@PathVariable Long id, @RequestBody BannerRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Deleted";
    }
}
