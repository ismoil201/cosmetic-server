package com.example.backend.domain.upload.controller;

import com.example.backend.domain.upload.dto.PresignUploadRequest;
import com.example.backend.domain.upload.dto.PresignUploadResponse;
import com.example.backend.domain.upload.service.UploadService;
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