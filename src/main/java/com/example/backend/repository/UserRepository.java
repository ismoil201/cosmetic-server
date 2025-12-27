package com.example.backend.repository;

import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    //ADMIN FILTER
    Page<User> findByActive(Boolean active, Pageable pageable);

    Optional<User> findByPhone(String phone);


    boolean existsByEmail(String email);
}
