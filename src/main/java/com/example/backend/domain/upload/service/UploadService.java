package com.example.backend.domain.upload.service;

import com.example.backend.domain.upload.dto.PresignUploadRequest;
import com.example.backend.domain.upload.dto.PresignUploadResponse;
import com.example.backend.global.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Secure upload service with validation
 *
 * Security features:
 * - Content type validation (images only)
 * - File size validation (max 10MB)
 * - Folder/path validation (safe folders only)
 * - Path traversal prevention
 * - Authentication required (enforced by SecurityConfig)
 */
@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Presigner presigner;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.public-base-url}")
    private String publicBaseUrl;

    // Security constraints
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "products",
            "reviews",
            "banners",
            "profiles"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;  // 10MB in bytes
    private static final int MAX_FILENAME_LENGTH = 255;

    public PresignUploadResponse presign(PresignUploadRequest req) {
        // Validate all security constraints
        validateContentType(req.getContentType());
        validateFileSize(req.getFileSizeBytes());
        validateFileName(req.getFileName());
        String validatedFolder = validateAndGetFolder(req.getFolder());

        // Generate safe object key
        String ext = extractExtension(req.getFileName());
        String fileName = UUID.randomUUID() + ext;
        String objectKey = validatedFolder + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(req.getContentType())
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(putObjectRequest)
                        .build()
        );

        String publicUrl = publicBaseUrl + "/" + objectKey;

        return new PresignUploadResponse(
                presigned.url().toString(),
                publicUrl,
                objectKey
        );
    }

    /**
     * Validate content type (images only)
     */
    private void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Content type is required");
        }

        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid content type. Allowed: image/jpeg, image/png, image/webp"
            );
        }
    }

    /**
     * Validate file size (max 10MB)
     */
    private void validateFileSize(Long fileSizeBytes) {
        if (fileSizeBytes == null) {
            throw new BadRequestException("File size is required");
        }

        if (fileSizeBytes <= 0) {
            throw new BadRequestException("File size must be greater than 0");
        }

        if (fileSizeBytes > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    String.format("File size exceeds maximum allowed size of %d MB",
                            MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }

    /**
     * Validate file name (prevent path traversal and malicious names)
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name is required");
        }

        if (fileName.length() > MAX_FILENAME_LENGTH) {
            throw new BadRequestException("File name is too long");
        }

        // Prevent path traversal
        if (fileName.contains("..") ||
            fileName.contains("/") ||
            fileName.contains("\\") ||
            fileName.startsWith(".")) {
            throw new BadRequestException("Invalid file name: path traversal detected");
        }

        // Prevent suspicious patterns
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.contains("~") ||
            lowerFileName.contains(";") ||
            lowerFileName.contains("|") ||
            lowerFileName.contains("&")) {
            throw new BadRequestException("Invalid file name: contains forbidden characters");
        }
    }

    /**
     * Validate and return safe folder name
     */
    private String validateAndGetFolder(String folder) {
        // Default to reviews if not specified
        if (folder == null || folder.isBlank()) {
            return "reviews";
        }

        String normalizedFolder = folder.toLowerCase().trim();

        // Check against whitelist
        if (!ALLOWED_FOLDERS.contains(normalizedFolder)) {
            throw new BadRequestException(
                    "Invalid folder. Allowed: products, reviews, banners, profiles"
            );
        }

        return normalizedFolder;
    }

    /**
     * Extract file extension safely
     */
    private String extractExtension(String fileName) {
        if (fileName == null) return ".jpg";
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) return ".jpg";

        String ext = fileName.substring(idx).toLowerCase();

        // Whitelist extensions based on content type
        if (ext.equals(".jpg") || ext.equals(".jpeg") ||
            ext.equals(".png") || ext.equals(".webp")) {
            return ext;
        }

        // Default to .jpg for safety
        return ".jpg";
    }
}
