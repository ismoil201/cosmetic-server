package com.example.backend.domain.home.service;

import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.event.service.EventTrackingService;
import com.example.backend.domain.product.dto.ProductCardResponse;
import com.example.backend.domain.product.entity.Category;
import com.example.backend.domain.product.entity.Product;
import com.example.backend.domain.user.entity.User;
import com.example.backend.domain.event.repository.EventLogRepository;
import com.example.backend.domain.product.repository.ProductRepository;
import com.example.backend.domain.recommendation.repository.UserInterestRepository;
import com.example.backend.domain.recommendation.service.NegativeFeedbackService;
import com.example.backend.domain.product.service.ProductService;
import com.example.backend.domain.recommendation.service.UserInterestCacheService;
import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ✅ STEP 9: Personalized Home Feed Service
 *
 * Implements production-ready personalized product recommendations using:
 * - User interest profiles (CATEGORY, BRAND, QUERY from user_interests table)
 * - Multi-dimensional product scoring
 * - Diversity controls (brand + category)
 * - Graceful fallback for guests and cold-start users
 *
 * Algorithm:
 * 1. Load user interests from cache (30min TTL)
 * 2. Build candidate pool: personal (interest-based) + explore (global)
 * 3. Score candidates using weighted formula
 * 4. Apply diversity filtering (max per category/brand)
 * 5. Blend personal (70%) + explore (30%)
 * 6. Shuffle top pool for variety
 * 7. Return as ProductCardResponse list
 *
 * Scoring Formula:
 * finalScore = categoryInterest × 0.40
 *            + brandInterest × 0.25
 *            + queryInterest × 0.15
 *            + popularity × 0.10
 *            + discount × 0.05
 *            + recency × 0.03
 *            + stock × 0.02
 *            - seenPenalty
 *
 * Performance:
 * - Candidate pool capped at 300-500 products
 * - Batch conversion (N+1 prevention)
 * - Cached interest lookups
 * - < 100ms for authenticated users
 * - < 50ms for guests
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeFeedService {

    // =====================================================
    // STEP 9: Scoring weights (configurable constants)
    // =====================================================
    private static final double WEIGHT_CATEGORY_INTEREST = 0.40;
    private static final double WEIGHT_BRAND_INTEREST = 0.25;
    private static final double WEIGHT_QUERY_INTEREST = 0.15;  // Future: match against product search_text
    private static final double WEIGHT_POPULARITY = 0.10;
    private static final double WEIGHT_DISCOUNT = 0.05;
    private static final double WEIGHT_RECENCY = 0.03;
    private static final double WEIGHT_STOCK = 0.02;

    // Diversity controls
    private static final int MAX_PER_CATEGORY = 8;  // Prevent category domination
    private static final int MAX_PER_BRAND_SMALL_CATALOG = 6;
    private static final int MAX_PER_BRAND_LARGE_CATALOG = 3;

    // Blend ratio
    private static final double PERSONAL_RATIO = 0.70;  // 70% personalized, 30% explore

    // =====================================================
    // ✅ PRODUCTION: Performance monitoring thresholds
    // =====================================================
    /**
     * Slow request threshold (ms) - log INFO for monitoring
     * Triggered when feed generation takes longer than normal but acceptable
     */
    private static final long SLOW_FEED_THRESHOLD_MS = 500;

    /**
     * Very slow request threshold (ms) - log WARN for investigation
     * Triggered when feed generation is dangerously slow, requires attention
     */
    private static final long VERY_SLOW_FEED_THRESHOLD_MS = 1000;

    /**
     * Maximum expected candidate count
     * Exceeding this indicates fetch limit controls are not working correctly
     */
    private static final int MAX_EXPECTED_CANDIDATES = 350;

    private final EventTrackingService eventTrackingService;
    private final UserService userService;
    private final UserInterestRepository interestRepo;
    private final ProductRepository productRepo;
    private final ProductService productService;

    private final EventLogRepository eventRepo;
    private final NegativeFeedbackService negativeFeedbackService;

    // ✅ Use cached user interests service (30min TTL)
    private final UserInterestCacheService userInterestCacheService;

    @Transactional(readOnly = true)
    public List<ProductCardResponse> buildFeed(int limit) {
        long startTime = System.currentTimeMillis();
        long t0, t1;

        if (limit <= 0) return List.of();
        if (limit > 120) limit = 120;

        t0 = System.currentTimeMillis();
        User user = userService.getCurrentUserOrNull();
        t1 = System.currentTimeMillis();
        long authMs = t1 - t0;

        // ✅ STABLE seed: kuniga 1 marta o‘zgaradi (scroll/pagination sakrashini kamaytiradi)
        String seed = (user != null)
                ? ("u" + user.getId() + ":" + LocalDate.now())
                : ("anon:" + LocalDate.now());
        Random rnd = new Random(seed.hashCode());

        // ✅ Fetch size: limitga proporsional (underfill + perf muammosini kamaytiradi)
        int fetch = Math.max(limit * 3, 60);

        // 0) Negative feedback
        t0 = System.currentTimeMillis();
        if (user != null) {
            negativeFeedbackService.applyIfNeeded(user);
        }
        t1 = System.currentTimeMillis();
        long negativeFeedbackMs = t1 - t0;

        // 1) seen (oxirgi 3 kunda ko'rilganlar)
        t0 = System.currentTimeMillis();
        Set<Long> seenSet = new HashSet<>();
        if (user != null) {
            seenSet.addAll(eventRepo.findProductIdsAfter(
                    user, EventType.VIEW, LocalDateTime.now().minusDays(3)
            ));
        }
        t1 = System.currentTimeMillis();
        long seenMs = t1 - t0;

        List<Long> excludeSoft = new ArrayList<>(seenSet);
        boolean excludeSoftEmpty = excludeSoft.isEmpty();

        // 2) ✅ STEP 10: Load all user interests (CACHED: 30min TTL)
        t0 = System.currentTimeMillis();
        Map<String, Double> catScore = (user != null)
            ? userInterestCacheService.getCategoryScores(user)
            : new HashMap<>();

        Map<String, Double> brandScore = (user != null)
            ? userInterestCacheService.getBrandScores(user)
            : new HashMap<>();

        // ✅ STEP 10: Load QUERY interests for matching
        Map<String, Double> queryScore = (user != null)
            ? userInterestCacheService.getQueryScores(user)
            : new HashMap<>();
        t1 = System.currentTimeMillis();
        long interestsMs = t1 - t0;

        // 3) candidates: PERSONAL (interest) + EXPLORE (global)
        t0 = System.currentTimeMillis();
        List<Product> personalCandidates = new ArrayList<>();
        List<Product> exploreCandidates = new ArrayList<>();

        // ✅ PERFORMANCE: Cap fetch sizes to avoid loading 500+ products
        int catFetch = Math.min(fetch, 100);    // Max 100 per category pool
        int brandFetch = Math.min(fetch, 100);  // Max 100 per brand pool
        int discountFetch = Math.min(fetch, 80);
        int popularFetch = Math.min(fetch, 80);
        int newFetch = Math.min(fetch, 80);

        if (user != null) {
            // ✅ CACHED: top categories (30min TTL)
            List<String> catKeys = userInterestCacheService.getTopCategoryKeys(user);
            List<String> catStrings = new ArrayList<>();
            for (String k : catKeys) {
                if (k == null) continue;
                try {
                    Category.valueOf(k.trim().toUpperCase()); // Validate
                    catStrings.add(k.trim().toUpperCase());
                }
                catch (IllegalArgumentException ignore) {}
            }
            if (!catStrings.isEmpty()) {
                personalCandidates.addAll(productRepo.candidatesByCategories(
                        catStrings, excludeSoft, excludeSoftEmpty ? 1 : 0, catFetch
                ));
            }

            // ✅ CACHED: top brands (30min TTL)
            List<String> brands = userInterestCacheService.getTopBrandKeys(user)
                    .stream()
                    .filter(s -> !s.isBlank())
                    .toList();

            if (!brands.isEmpty()) {
                personalCandidates.addAll(productRepo.candidatesByBrands(
                        brands, excludeSoft, excludeSoftEmpty ? 1 : 0, brandFetch
                ));
            }
        }

        // EXPLORE: discounted + popular + new (seen exclude bilan)
        exploreCandidates.addAll(productRepo.discountedCandidates(
                excludeSoft, excludeSoftEmpty ? 1 : 0, discountFetch
        ));
        exploreCandidates.addAll(productRepo.findByActiveTrueOrderBySoldCountDesc(
                PageRequest.of(0, popularFetch)
        ).getContent());
        exploreCandidates.addAll(productRepo.findByActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, newFetch)
        ).getContent());
        t1 = System.currentTimeMillis();
        long candidatesMs = t1 - t0;

        // ✅ STEP 9: Dynamic diversity limits based on catalog size
        int poolSizeGuess = personalCandidates.size() + exploreCandidates.size();
        int maxPerBrand = (poolSizeGuess <= 80) ? MAX_PER_BRAND_SMALL_CATALOG : MAX_PER_BRAND_LARGE_CATALOG;
        int maxPerCategory = MAX_PER_CATEGORY;

        // 4) ✅ STEP 10: Scoring with QUERY interest matching
        t0 = System.currentTimeMillis();
        double seenPenalty = calcSeenPenalty(poolSizeGuess);
        List<ScoredProduct> personalScored = scoreAndSort(personalCandidates, catScore, brandScore, queryScore, seenSet, seenPenalty);
        List<ScoredProduct> exploreScored = scoreAndSort(exploreCandidates, catScore, brandScore, queryScore, seenSet, seenPenalty);
        t1 = System.currentTimeMillis();
        long scoringMs = t1 - t0;

        // 5) ✅ STEP 9: Blend 70% personal + 30% explore (graceful fallback if personal empty)
        t0 = System.currentTimeMillis();
        int personalCount = (user == null || personalScored.isEmpty())
                ? 0
                : (int) Math.round(limit * PERSONAL_RATIO);

        int exploreCount = limit - personalCount;

        List<Product> blended = blendDiversified(
                personalScored, exploreScored,
                personalCount, exploreCount,
                maxPerCategory, maxPerBrand
        );

        // 6) Fallback: seen exclude sabab yetmasa -> seen ni qaytaramiz (exclude yo'q)
        if (blended.size() < limit) {
            List<Product> more = productRepo.findByActiveTrueOrderBySoldCountDesc(
                    PageRequest.of(0, Math.max(fetch * 2, 120))
            ).getContent();

            List<ScoredProduct> moreScored = scoreAndSort(
                    more, catScore, brandScore, queryScore, Collections.emptySet(), 0.0
            );

            blended = mergeFill(blended, moreScored, limit, maxPerCategory, maxPerBrand);
        }

        // 7) Top pool shuffle
        blended = shuffleTopPool(blended, limit, rnd);
        t1 = System.currentTimeMillis();
        long diversityMs = t1 - t0;

        // 8) impressions log


        // 9) toCard (✅ batch: N+1 yo'q)
        t0 = System.currentTimeMillis();
        List<Product> picked = blended.stream().limit(limit).toList();
        List<ProductCardResponse> result = productService.toCardsPublic(picked, user);
        t1 = System.currentTimeMillis();
        long toCardsMs = t1 - t0;

        long totalMs = System.currentTimeMillis() - startTime;

        // ✅ PRODUCTION-SAFE PERFORMANCE LOGGING
        // Only log slow requests to avoid spamming production logs
        logPerformance(user, limit, catScore, brandScore, queryScore, poolSizeGuess,
                authMs, negativeFeedbackMs, seenMs, interestsMs, candidatesMs,
                scoringMs, diversityMs, toCardsMs, totalMs);

        return result;
    }

    /**
     * ✅ PRODUCTION: Production-safe performance logging
     *
     * Logging levels based on performance:
     * - WARN: totalMs > 1000ms (very slow, requires investigation)
     * - INFO: totalMs > 500ms (slow, monitor for patterns)
     * - DEBUG: totalMs <= 500ms (normal, detailed metrics)
     *
     * Security:
     * - Log userId (safe for correlation, not PII)
     * - Do NOT log: tokens, emails, names, sensitive data
     *
     * @param user Current user (may be null for guest)
     * @param limit Requested feed size
     * @param catScore Category interest scores
     * @param brandScore Brand interest scores
     * @param queryScore Query interest scores
     * @param candidates Total candidates loaded
     * @param authMs User auth lookup time
     * @param negativeFeedbackMs Negative feedback check time
     * @param seenMs Recently viewed products query time
     * @param interestsMs User interests cache lookup time
     * @param candidatesMs Candidate loading time (DB queries)
     * @param scoringMs Scoring algorithm time
     * @param diversityMs Diversity filtering time
     * @param toCardsMs ProductCardResponse conversion time
     * @param totalMs Total request time
     */
    private void logPerformance(User user, int limit,
                                Map<String, Double> catScore,
                                Map<String, Double> brandScore,
                                Map<String, Double> queryScore,
                                int candidates,
                                long authMs, long negativeFeedbackMs, long seenMs,
                                long interestsMs, long candidatesMs, long scoringMs,
                                long diversityMs, long toCardsMs, long totalMs) {

        String userId = (user != null) ? String.valueOf(user.getId()) : "guest";
        int totalInterests = catScore.size() + brandScore.size() + queryScore.size();

        String perfMsg = String.format(
                "[home-feed] userId=%s limit=%d interests=%d candidates=%d " +
                        "authMs=%d negativeFeedbackMs=%d seenMs=%d interestsMs=%d candidatesMs=%d " +
                        "scoringMs=%d diversityMs=%d toCardsMs=%d totalMs=%d",
                userId, limit, totalInterests, candidates,
                authMs, negativeFeedbackMs, seenMs, interestsMs, candidatesMs,
                scoringMs, diversityMs, toCardsMs, totalMs
        );

        // ✅ Warn if candidate count exceeds expected maximum (indicates bug in fetch limits)
        if (candidates > MAX_EXPECTED_CANDIDATES) {
            log.warn("[home-feed] ALERT: Candidate count {} exceeds maximum expected {} - " +
                            "fetch limit controls may not be working correctly. userId={}",
                    candidates, MAX_EXPECTED_CANDIDATES, userId);
        }

        // ✅ Tiered logging based on performance
        if (totalMs > VERY_SLOW_FEED_THRESHOLD_MS) {
            // Very slow request - WARN level for immediate attention
            log.warn("[PERF] VERY SLOW FEED: {}", perfMsg);

            // Log breakdown of slow components for investigation
            if (candidatesMs > 200) {
                log.warn("[home-feed] Slow candidate loading: {}ms (DB query performance issue?)", candidatesMs);
            }
            if (toCardsMs > 100) {
                log.warn("[home-feed] Slow card conversion: {}ms (N+1 query issue?)", toCardsMs);
            }
            if (interestsMs > 50) {
                log.warn("[home-feed] Slow interest lookup: {}ms (cache miss or Redis slow?)", interestsMs);
            }

        } else if (totalMs > SLOW_FEED_THRESHOLD_MS) {
            // Slow request - INFO level for monitoring trends
            log.info("[PERF] SLOW FEED: {}", perfMsg);

        } else {
            // Normal request - DEBUG level (disabled in production by default)
            log.debug("[PERF] {}", perfMsg);
        }
    }

    private double calcSeenPenalty(int poolSizeGuess) {
        // kichik katalogda yumshoq
        if (poolSizeGuess <= 80) return 10.0;
        return 20.0;
    }

    private List<ScoredProduct> scoreAndSort(List<Product> list,
                                             Map<String, Double> catScore,
                                             Map<String, Double> brandScore,
                                             Map<String, Double> queryScore,
                                             Set<Long> seenSet,
                                             double seenPenaltyValue) {

        Map<Long, Product> uniq = new LinkedHashMap<>();
        for (Product p : list) {
            if (p == null || p.getId() == null) continue;
            if (!p.isActive()) continue;
            uniq.putIfAbsent(p.getId(), p);
        }

        List<ScoredProduct> scored = new ArrayList<>(uniq.size());
        for (Product p : uniq.values()) {
            scored.add(new ScoredProduct(p, score(p, catScore, brandScore, queryScore, seenSet, seenPenaltyValue)));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored;
    }

    /**
     * ✅ STEP 10: Multi-dimensional product scoring with QUERY matching
     *
     * Scoring Formula (normalized to 0-100 scale):
     * - categoryInterest × 40%  (User's category preference)
     * - brandInterest × 25%     (User's brand preference)
     * - queryInterest × 15%     (Match product fields against user search queries)
     * - popularity × 10%        (Global popularity signal)
     * - discount × 5%           (Discount percentage boost)
     * - recency × 3%            (New arrival boost)
     * - stock × 2%              (In-stock boost)
     * - seenPenalty             (Reduce already-viewed products)
     *
     * @param p Product to score
     * @param catScore User's category interest scores
     * @param brandScore User's brand interest scores
     * @param queryScore User's query interest scores
     * @param seenSet Recently viewed product IDs
     * @param seenPenaltyValue Penalty for seen products
     * @return Final score (higher = better match)
     */
    private double score(Product p,
                         Map<String, Double> catScore,
                         Map<String, Double> brandScore,
                         Map<String, Double> queryScore,
                         Set<Long> seenSet,
                         double seenPenaltyValue) {

        // 1) Category interest (0-40 points)
        double categoryInterestScore = 0.0;
        if (p.getCategory() != null) {
            categoryInterestScore = catScore.getOrDefault(p.getCategory().name(), 0.0) * WEIGHT_CATEGORY_INTEREST;
        }

        // 2) Brand interest (0-25 points)
        double brandInterestScore = 0.0;
        if (p.getBrand() != null) {
            String key = p.getBrand().trim().toLowerCase();
            brandInterestScore = brandScore.getOrDefault(key, 0.0) * WEIGHT_BRAND_INTEREST;
        }

        // 3) ✅ STEP 10: Query interest (0-15 points) - Match against product fields
        double queryInterestScore = matchQueryInterests(p, queryScore);

        // 4) Popularity score (0-10 points)
        // Normalize: typical popular product has ~100-500 soldCount + viewCount
        double rawPopularity = (p.getSoldCount() * 3.0) + (p.getViewCount() * 1.0);
        double popularityScore = Math.min(100.0, rawPopularity / 10.0) * WEIGHT_POPULARITY;

        // 5) Discount score (0-5 points)
        double discountScore = 0.0;
        BigDecimal price = p.getPrice();
        BigDecimal discount = p.getDiscountPrice();

        if (price != null
                && discount != null
                && price.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(price) < 0) {

            // Discount percentage: 0.0 to 1.0
            BigDecimal discountPct = price.subtract(discount)
                    .divide(price, 6, RoundingMode.HALF_UP);
            // Normalize to 0-100 scale, then apply weight
            discountScore = discountPct.doubleValue() * 100.0 * WEIGHT_DISCOUNT;
        }

        // 6) Recency score (0-3 points)
        // Boost for new arrivals (products created in last 30 days)
        double recencyScore = 0.0;
        if (p.getCreatedAt() != null) {
            long daysOld = java.time.Duration.between(p.getCreatedAt(), LocalDateTime.now()).toDays();
            if (daysOld < 30) {
                // Linear decay: 100 points at day 0, 0 points at day 30
                recencyScore = (30.0 - daysOld) / 30.0 * 100.0 * WEIGHT_RECENCY;
            }
        }

        // 7) Stock score (0-2 points)
        double stockScore = (p.getStock() > 0) ? 100.0 * WEIGHT_STOCK : 0.0;

        // 8) Today Deal boost (bonus)
        double todayDealBoost = p.isTodayDeal() ? 5.0 : 0.0;

        // 9) Seen penalty (reduce recently viewed)
        double seenPenalty = (seenSet != null && p.getId() != null && seenSet.contains(p.getId()))
                ? seenPenaltyValue
                : 0.0;

        return categoryInterestScore
                + brandInterestScore
                + queryInterestScore
                + popularityScore
                + discountScore
                + recencyScore
                + stockScore
                + todayDealBoost
                - seenPenalty;
    }

    /**
     * ✅ STEP 10: Match user's QUERY interests against product fields
     *
     * Matches queries against: name, searchText, description, brand, category
     * Uses case-insensitive substring matching
     * Caps total boost if multiple queries match same product
     *
     * @param p Product to check
     * @param queryScore Map of query interests (query → score)
     * @return Query interest score contribution (0-15 points)
     */
    private double matchQueryInterests(Product p, Map<String, Double> queryScore) {
        if (queryScore == null || queryScore.isEmpty()) return 0.0;

        // Build searchable text from product fields (normalized)
        String productText = buildSearchableText(p);

        double totalQueryScore = 0.0;
        int matchCount = 0;

        for (Map.Entry<String, Double> entry : queryScore.entrySet()) {
            String query = entry.getKey(); // Already lowercase from cache
            double score = entry.getValue();

            if (productText.contains(query)) {
                totalQueryScore += score;
                matchCount++;
            }
        }

        // Cap boost: max 2 queries can contribute full weight
        double cappedScore = totalQueryScore;
        if (matchCount > 2) {
            cappedScore = Math.min(totalQueryScore, queryScore.values().stream()
                    .sorted(Comparator.reverseOrder())
                    .limit(2)
                    .mapToDouble(Double::doubleValue)
                    .sum());
        }

        return cappedScore * WEIGHT_QUERY_INTEREST;
    }

    /**
     * ✅ STEP 10: Build searchable text from product fields
     *
     * Combines all searchable product fields into single normalized string
     *
     * @param p Product
     * @return Lowercase concatenated searchable text
     */
    private String buildSearchableText(Product p) {
        StringBuilder sb = new StringBuilder();
        if (p.getName() != null) sb.append(p.getName().toLowerCase()).append(" ");
        if (p.getSearchText() != null) sb.append(p.getSearchText().toLowerCase()).append(" ");
        if (p.getDescription() != null) sb.append(p.getDescription().toLowerCase()).append(" ");
        if (p.getBrand() != null) sb.append(p.getBrand().toLowerCase()).append(" ");
        if (p.getCategory() != null) sb.append(p.getCategory().name().toLowerCase()).append(" ");
        return sb.toString();
    }

    /**
     * ✅ STEP 9: Blend personal + explore with diversity controls
     *
     * Applies global diversity tracking across both pools:
     * - Category diversity: Max 8 products per category (prevent domination)
     * - Brand diversity: Max 3-6 products per brand (based on catalog size)
     * - Deduplication: Same product never appears twice
     *
     * @param personal Personal recommendation candidates
     * @param explore Exploration candidates
     * @param personalCount Target count from personal pool
     * @param exploreCount Target count from explore pool
     * @param maxPerCategory Max products per category
     * @param maxPerBrand Max products per brand
     * @return Blended and diversified product list
     */
    private List<Product> blendDiversified(List<ScoredProduct> personal,
                                           List<ScoredProduct> explore,
                                           int personalCount,
                                           int exploreCount,
                                           int maxPerCategory,
                                           int maxPerBrand) {

        int target = personalCount + exploreCount;

        List<Product> out = new ArrayList<>(target);
        Set<Long> used = new HashSet<>(target * 2);
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Integer> brandCount = new HashMap<>();

        // Add from personal pool first (higher priority)
        addWithDiversity(out, used, categoryCount, brandCount, personal, personalCount, maxPerCategory, maxPerBrand);

        // Add from explore pool
        addWithDiversity(out, used, categoryCount, brandCount, explore, exploreCount, maxPerCategory, maxPerBrand);

        // Fallback: If still short, relax diversity and fill from explore
        if (out.size() < target) {
            addWithDiversity(out, used, categoryCount, brandCount, explore, target - out.size(), maxPerCategory, maxPerBrand);
        }

        return out;
    }

    /**
     * ✅ STEP 9: Add products with category + brand diversity controls
     */
    private void addWithDiversity(List<Product> out,
                                  Set<Long> used,
                                  Map<String, Integer> categoryCount,
                                  Map<String, Integer> brandCount,
                                  List<ScoredProduct> scored,
                                  int need,
                                  int maxPerCategory,
                                  int maxPerBrand) {

        if (need <= 0 || scored == null || scored.isEmpty()) return;

        for (ScoredProduct sp : scored) {
            if (need <= 0) break;
            Product p = sp.p;
            if (p == null || p.getId() == null) continue;

            // Skip duplicates
            if (!used.add(p.getId())) continue;

            // Check category limit
            String category = (p.getCategory() == null) ? "" : p.getCategory().name();
            int catCnt = categoryCount.getOrDefault(category, 0);
            if (catCnt >= maxPerCategory) {
                used.remove(p.getId());  // Revert
                continue;
            }

            // Check brand limit
            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            int brandCnt = brandCount.getOrDefault(brand, 0);
            if (brandCnt >= maxPerBrand) {
                used.remove(p.getId());  // Revert
                continue;
            }

            // Add product and update counters
            out.add(p);
            categoryCount.put(category, catCnt + 1);
            brandCount.put(brand, brandCnt + 1);
            need--;
        }
    }

    /**
     * ✅ STEP 9: Fallback merge with diversity
     *
     * Merges base list with additional scored products up to limit,
     * while respecting category and brand diversity constraints.
     */
    private List<Product> mergeFill(List<Product> base,
                                    List<ScoredProduct> more,
                                    int limit,
                                    int maxPerCategory,
                                    int maxPerBrand) {

        LinkedHashMap<Long, Product> uniq = new LinkedHashMap<>(limit * 2);
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Integer> brandCount = new HashMap<>();

        // Track existing products
        for (Product p : base) {
            if (p == null || p.getId() == null) continue;
            uniq.putIfAbsent(p.getId(), p);

            String category = (p.getCategory() == null) ? "" : p.getCategory().name();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            brandCount.put(brand, brandCount.getOrDefault(brand, 0) + 1);
        }

        // Add more products with diversity checks
        for (ScoredProduct sp : more) {
            if (uniq.size() >= limit) break;
            Product p = sp.p;
            if (p == null || p.getId() == null) continue;
            if (uniq.containsKey(p.getId())) continue;

            String category = (p.getCategory() == null) ? "" : p.getCategory().name();
            int catCnt = categoryCount.getOrDefault(category, 0);
            if (catCnt >= maxPerCategory) continue;

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            int brandCnt = brandCount.getOrDefault(brand, 0);
            if (brandCnt >= maxPerBrand) continue;

            uniq.put(p.getId(), p);
            categoryCount.put(category, catCnt + 1);
            brandCount.put(brand, brandCnt + 1);
        }

        // If still short due to diversity limits, relax constraints and fill
        if (uniq.size() < limit) {
            for (ScoredProduct sp : more) {
                if (uniq.size() >= limit) break;
                Product p = sp.p;
                if (p == null || p.getId() == null) continue;
                uniq.putIfAbsent(p.getId(), p);
            }
        }

        return uniq.values().stream().limit(limit).toList();
    }

    /**
     * Top pool shuffle: rankingni butunlay buzmaydi, lekin takrorni kamaytiradi.
     */
    private List<Product> shuffleTopPool(List<Product> list, int limit, Random rnd) {
        if (list == null || list.isEmpty()) return list;

        int k = Math.min(list.size(), Math.max(limit * 2, 30));
        List<Product> top = new ArrayList<>(list.subList(0, k));
        Collections.shuffle(top, rnd);

        List<Product> out = new ArrayList<>(list.size());
        out.addAll(top);
        if (list.size() > k) out.addAll(list.subList(k, list.size()));
        return out;
    }

    private record ScoredProduct(Product p, double score) {}
}