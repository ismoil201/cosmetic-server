package com.example.backend.domain.user.service;

import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Long requireUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated");
        }

        // Ko‘pincha principal = username/email bo‘ladi
        String email;

        Object principal = auth.getPrincipal();
        if (principal instanceof String s) {
            // "anonymousUser" bo‘lsa demak filter ishlamagan
            if ("anonymousUser".equals(s)) {
                throw new RuntimeException("Unauthenticated (anonymousUser)");
            }
            email = s;
        } else if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = null;
        }

        if (email == null || email.isBlank()) {
            throw new RuntimeException("Cannot extract email from SecurityContext");
        }

        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found by email: " + email));
    }

    public String requireEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof String s) return s;
        if (principal instanceof UserDetails ud) return ud.getUsername();
        throw new RuntimeException("Cannot extract email");
    }
}