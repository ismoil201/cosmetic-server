package com.example.backend.service;

import com.example.backend.entity.InterestType;
import com.example.backend.entity.User;
import com.example.backend.entity.UserInterest;
import com.example.backend.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
     * ✅ CACHED: Category Interest Scores
     *
     * Cache: user:interests:{userId}:CATEGORY
     * TTL: 30 minutes
     * Why: Changes slowly (updated on view/click), queried frequently
     *
     * Returns: Map<categoryName, score>
     */
    @Cacheable(
        value = "user:interests",
        key = "#user.id + ':CATEGORY'",
        unless = "#result == null || #result.isEmpty()"
    )
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
     * ✅ CACHED: Brand Interest Scores
     *
     * Cache: user:interests:{userId}:BRAND
     * TTL: 30 minutes
     * Why: Changes slowly, queried frequently in feed
     *
     * Returns: Map<brandName, score> (lowercase)
     */
    @Cacheable(
        value = "user:interests",
        key = "#user.id + ':BRAND'",
        unless = "#result == null || #result.isEmpty()"
    )
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
     * ✅ CACHED: Top Category Keys (for candidate selection)
     *
     * Cache: user:interests:{userId}:top4cats
     * TTL: 30 minutes
     * Why: Used to select product candidates by category
     *
     * Returns: List of top 4 category names
     */
    @Cacheable(
        value = "user:interests",
        key = "#user.id + ':top4cats'",
        unless = "#result == null || #result.isEmpty()"
    )
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
     * ✅ CACHED: Top Brand Keys (for candidate selection)
     *
     * Cache: user:interests:{userId}:top4brands
     * TTL: 30 minutes
     * Why: Used to select product candidates by brand
     *
     * Returns: List of top 4 brand names (lowercase)
     */
    @Cacheable(
        value = "user:interests",
        key = "#user.id + ':top4brands'",
        unless = "#result == null || #result.isEmpty()"
    )
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
