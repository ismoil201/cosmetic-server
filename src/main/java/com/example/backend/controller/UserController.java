package com.example.backend.controller;

import com.example.backend.dto.UserProfileResponse;
import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse me() {
        return userService.getProfile();
    }

    @PutMapping("/me")
    public ResponseEntity<?> update(@RequestBody UserProfileResponse req) {
        userService.updateProfile(req);
        return ResponseEntity.ok("Updated");
    }
}
