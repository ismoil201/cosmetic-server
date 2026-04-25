package com.example.backend.domain.upload.dto;

import lombok.Data;

@Data
public class PresignUploadRequest {
    private String fileName;
    private String contentType;
    private String folder;
}