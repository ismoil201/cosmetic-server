package com.example.backend.controller;

import com.example.backend.dto.PresignUploadRequest;
import com.example.backend.dto.PresignUploadResponse;
import com.example.backend.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/presign")
    public PresignUploadResponse presign(@RequestBody PresignUploadRequest req) {
        return uploadService.presign(req);
    }
}