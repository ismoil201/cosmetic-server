package com.example.backend.domain.user.repository;

import com.example.backend.domain.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdAndActiveTrue(Long userId);
}
