package com.example.backend.domain.admin.dto;

import com.example.backend.domain.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
}
