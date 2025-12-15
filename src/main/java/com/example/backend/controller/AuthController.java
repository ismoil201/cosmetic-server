package com.example.backend.controller;

import com.example.backend.dto.LoginDto;
import com.example.backend.dto.RegisterDto;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.JwtService;
import com.example.backend.dto.LoginResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDto dto) {

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists!");
        }

        User u = new User();
        u.setEmail(dto.getEmail());
        u.setPassword(encoder.encode(dto.getPassword()));
        u.setFullName(dto.getFullName());

        // ⭐ Yangi user har doim USER bo'ladi
        u.setRole(Role.USER);

        userRepository.save(u);

        return ResponseEntity.ok("Registered!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto dto) {

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Wrong password");
        }

        String token = jwtService.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                token
        );

        return ResponseEntity.ok(response);
    }

}
