package com.example.backend.domain.user.repository;

import com.example.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Page<User> findByActive(boolean active, Pageable pageable);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);
}

