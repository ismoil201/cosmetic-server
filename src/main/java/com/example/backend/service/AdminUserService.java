package com.example.backend.service;

import com.example.backend.dto.AdminUserResponse;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepo;

    // ✅ USER LIST
    public Page<AdminUserResponse> list(Boolean active, Pageable pageable) {

        Page<User> page = (active == null)
                ? userRepo.findAll(pageable)
                : userRepo.findByActive(active, pageable);

        return page.map(u -> new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getRole(),
                u.isActive(),
                u.getCreatedAt()
        ));
    }

    // 🚫 BLOCK
    public void block(Long userId) {
        User user = getUser(userId);
        checkSelf(user);
        user.setActive(false);
        userRepo.save(user);
    }

    // ✅ UNBLOCK
    public void unblock(Long userId) {
        User user = getUser(userId);
        user.setActive(true);
        userRepo.save(user);
    }

    // 🔁 ROLE CHANGE
    public void changeRole(Long userId, String role) {
        User user = getUser(userId);
        checkSelf(user);

        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid role: " + role);
        }

        userRepo.save(user);
    }

    // ===== HELPERS =====

    private User getUser(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ❗ ADMIN o‘zini block / role change qila olmasin
    private void checkSelf(User target) {
        String currentEmail =
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getName();

        if (target.getEmail().equals(currentEmail)) {
            throw new RuntimeException("You cannot modify your own account");
        }
    }
}
