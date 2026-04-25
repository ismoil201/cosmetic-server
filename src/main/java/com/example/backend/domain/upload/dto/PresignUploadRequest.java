package com.example.backend.domain.upload.dto;

import lombok.Data;

/**
 * Request DTO for presigned upload URL generation
 *
 * Security constraints:
 * - fileName: Required, validated for path traversal
 * - contentType: Required, must be image/jpeg, image/png, or image/webp
 * - folder: Optional, must be products/reviews/banners/profiles
 * - fileSizeBytes: Required, max 10MB (10,485,760 bytes)
 */
@Data
public class PresignUploadRequest {
    private String fileName;
    private String contentType;
    private String folder;
    private Long fileSizeBytes;  // File size in bytes for validation
}