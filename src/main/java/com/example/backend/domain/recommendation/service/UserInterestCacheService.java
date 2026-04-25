package com.example.backend.domain.recommendation.service;

import com.example.backend.domain.recommendation.entity.InterestType;
import com.example.backend.domain.recommendation.entity.UserInterest;
import com.example.backend.domain.recommendation.repository.UserInterestRepository;
import com.example.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User Interest Cache Service
 *
 * Purpose:
 * - Cache user category/brand interests (changes slowly)
 * - Reduce DB queries in feed/recommendation flows
 *
 * Cache Strategy:
 * - Per-user cache (user-specific data)
 * - 30 minute TTL (interests change slowly)
 * - Invalidate on explicit interest update (rare)
 *
 * Safety:
 * - User isolation (separate cache per user ID)
 * - Acceptable staleness (30min lag OK for interests)
 */
@Service
@RequiredArgsConstructor
public class UserInterestCacheService {

    private final UserInterestRepository interestRepo;

    /**
     * ⚠️ CACHE REMOVED: Category Interest Scores
     *
     * WHY NO CACHE:
     * - Map<String, Double> with @Cacheable causes Redis type metadata in values
     * - When returned to REST endpoints, typed Double values leak to JSON
     * - Potential Android deserialization errors with typed primitives
     *
     * SOLUTION:
     * - Query database directly (user-specific, indexed, <5ms)
     * - Clean Map serialization via @Primary ObjectMapper
     * - Acceptable performance impact (interest queries are infrequent)
     *
     * Returns: Map<categoryName, score>
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getCategoryScores(User user) {
        if (user == null) return Map.of();

        List<UserInterest> interests = interestRepo
            .findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.CATEGORY);

        return interests.stream()
            .filter(i -> i.getKey() != null)
            .collect(Collectors.toMap(
                UserInterest::getKey,
                UserInterest::getScore,
                (a, b) -> a  // Keep first if duplicate
            ));
    }

    /**
     * ⚠️ CACHE REMOVED: Brand Interest Scores
     *
     * WHY NO CACHE:
     * - Redis type metadata contaminates Map values
     * - Prevents Jackson deserialization errors in Android clients
     *
     * Returns: Map<brandName, score> (lowercase)
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getBrandScores(User user) {
        if (user == null) return Map.of();

        List<UserInterest> interests = interestRepo
            .findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.BRAND);

        return interests.stream()
            .filter(i -> i.getKey() != null)
            .collect(Collectors.toMap(
                i -> i.getKey().trim().toLowerCase(),
                UserInterest::getScore,
                (a, b) -> a
            ));
    }

    /**
     * ⚠️ CACHE REMOVED: Top Category Keys (for candidate selection)
     *
     * WHY NO CACHE:
     * - List<String> with @Cacheable causes Redis ArrayList type wrapper
     * - Potential type metadata leakage to REST responses
     *
     * Returns: List of top 4 category names
     */
    @Transactional(readOnly = true)
    public List<String> getTopCategoryKeys(User user) {
        if (user == null) return List.of();

        return interestRepo.topKeys(
            user,
            InterestType.CATEGORY,
            PageRequest.of(0, 4)
        );
    }

    /**
     * ⚠️ CACHE REMOVED: Top Brand Keys (for candidate selection)
     *
     * WHY NO CACHE:
     * - List<String> with Redis type metadata causes Android JSON errors
     *
     * Returns: List of top 4 brand names (lowercase)
     */
    @Transactional(readOnly = true)
    public List<String> getTopBrandKeys(User user) {
        if (user == null) return List.of();

        return interestRepo.topKeys(
            user,
            InterestType.BRAND,
            PageRequest.of(0, 4)
        ).stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> s.trim().toLowerCase())
            .toList();
    }

    /**
     * ✅ STEP 10: Query Interest Scores
     *
     * Returns: Map<queryText, score> (lowercase, trimmed)
     *
     * Used for matching product names/descriptions against user's search queries
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getQueryScores(User user) {
        if (user == null) return Map.of();

        List<UserInterest> interests = interestRepo
            .findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.QUERY);

        return interests.stream()
            .filter(i -> i.getKey() != null && !i.getKey().isBlank())
            .collect(Collectors.toMap(
                i -> i.getKey().trim().toLowerCase(),
                UserInterest::getScore,
                (a, b) -> a  // Keep first if duplicate
            ));
    }

    /**
     * ⚠️ CACHE INVALIDATION: Clear all interest caches for user
     *
     * When to call:
     * - Explicit user interest reset (rare)
     * - Admin override (rare)
     *
     * NOT called on normal view/click (let TTL handle it)
     */
    @CacheEvict(
        value = "user:interests",
        key = "#userId + '*'",  // Clear all user interest caches
        allEntries = false
    )
    public void invalidateUserInterests(Long userId) {
        // Explicit cache clear
        // Note: This only clears exact keys, not wildcard
        // For wildcard support, need custom Redis logic
    }

    /**
     * ⚠️ CACHE INVALIDATION: Clear specific interest type
     */
    @CacheEvict(
        value = "user:interests",
        key = "#userId + ':' + #type.name()"
    )
    public void invalidateUserInterestType(Long userId, InterestType type) {
        // Clear specific type (CATEGORY or BRAND)
    }
}
