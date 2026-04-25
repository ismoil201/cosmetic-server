package com.example.backend.domain.auth.controller;

import com.example.backend.domain.auth.dto.FirebaseLoginRequest;
import com.example.backend.domain.auth.dto.LoginRequest;
import com.example.backend.domain.auth.dto.LoginResponse;
import com.example.backend.domain.auth.dto.RegisterRequest;
import com.example.backend.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok("Registered");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
    @PostMapping("/firebase")
    public LoginResponse firebase(@RequestBody FirebaseLoginRequest req) {
        return authService.firebaseLogin(req.getIdToken());
    }

}
