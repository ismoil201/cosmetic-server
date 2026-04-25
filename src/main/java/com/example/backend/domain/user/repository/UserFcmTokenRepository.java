package com.example.backend.domain.user.repository;

import com.example.backend.domain.user.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFcmTokenRepository
        extends JpaRepository<UserFcmToken, Long> {

    List<UserFcmToken> findByUserId(Long userId);

    void deleteByToken(String token);
}
