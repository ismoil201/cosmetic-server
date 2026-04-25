package com.example.backend.domain.user.repository;

import com.example.backend.domain.user.entity.Receiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiverRepository extends JpaRepository<Receiver, Long> {
    List<Receiver> findByUserIdAndActiveTrue(Long userId);

}
