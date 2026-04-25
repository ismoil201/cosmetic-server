package com.example.backend.domain.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresignUploadResponse {
    private String uploadUrl;
    private String publicUrl;
    private String objectKey;
}


