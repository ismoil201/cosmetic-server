package com.example.backend.domain.auth.dto;

import lombok.Data;

@Data
public class FirebaseLoginRequest {
    private String idToken;
}
