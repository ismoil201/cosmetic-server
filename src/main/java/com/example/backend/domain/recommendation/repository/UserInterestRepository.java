package com.example.backend.domain.recommendation.repository;

import com.example.backend.domain.recommendation.entity.InterestType;
import com.example.backend.domain.recommendation.entity.UserInterest;
import com.example.backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    Optional<UserInterest> findByUserAndTypeAndKey(User user, InterestType type, String key);

    List<UserInterest> findTop20ByUserAndTypeOrderByScoreDesc(User user, InterestType type);

    @Query("""
       select ui.key from UserInterest ui
       where ui.user = :user and ui.type = :type
       order by ui.score desc
    """)
    List<String> topKeys(User user, InterestType type, Pageable pageable);
}