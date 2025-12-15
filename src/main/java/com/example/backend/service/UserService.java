package com.example.backend.service;

import com.example.backend.dto.UserProfileResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    public User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public UserProfileResponse getProfile() {
        User user = getCurrentUser();
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getProfileImage()
        );
    }
    public void updateProfile(UserProfileResponse req) {
        User user = getCurrentUser();
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setProfileImage(req.getProfileImage());
        userRepo.save(user);
    }

}
