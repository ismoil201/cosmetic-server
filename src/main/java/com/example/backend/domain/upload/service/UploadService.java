package com.example.backend.domain.upload.service;

import com.example.backend.domain.upload.dto.PresignUploadRequest;
import com.example.backend.domain.upload.dto.PresignUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Presigner presigner;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.public-base-url}")
    private String publicBaseUrl;

    public PresignUploadResponse presign(PresignUploadRequest req) {
        String folder = (req.getFolder() == null || req.getFolder().isBlank()) ? "reviews" : req.getFolder();
        String ext = extractExtension(req.getFileName());
        String fileName = UUID.randomUUID() + ext;
        String objectKey = folder + "/" + fileName;

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

    private String extractExtension(String fileName) {
        if (fileName == null) return ".jpg";
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) return ".jpg";
        return fileName.substring(idx);
    }
}