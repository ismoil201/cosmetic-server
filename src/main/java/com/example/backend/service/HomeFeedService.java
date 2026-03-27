package com.example.backend.service;

import com.example.backend.dto.ProductCardResponse;
import com.example.backend.entity.*;
import com.example.backend.repository.EventLogRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.UserInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeFeedService {

    private final EventTrackingService eventTrackingService;
    private final UserService userService;
    private final UserInterestRepository interestRepo;
    private final ProductRepository productRepo;
    private final ProductService productService;

    private final EventLogRepository eventRepo;
    private final NegativeFeedbackService negativeFeedbackService;

    // ✅ NEW: Use cached user interests service
    private final UserInterestCacheService userInterestCacheService;

    @Transactional(readOnly = true)
    public List<ProductCardResponse> buildFeed(int limit) {

        if (limit <= 0) return List.of();
        if (limit > 120) limit = 120;

        User user = userService.getCurrentUserOrNull();

        // ✅ STABLE seed: kuniga 1 marta o‘zgaradi (scroll/pagination sakrashini kamaytiradi)
        String seed = (user != null)
                ? ("u" + user.getId() + ":" + LocalDate.now())
                : ("anon:" + LocalDate.now());
        Random rnd = new Random(seed.hashCode());

        // ✅ Fetch size: limitga proporsional (underfill + perf muammosini kamaytiradi)
        int fetch = Math.max(limit * 3, 60);

        // 0) Negative feedback
        if (user != null) {
            negativeFeedbackService.applyIfNeeded(user);
        }

        // 1) seen (oxirgi 3 kunda ko‘rilganlar)
        Set<Long> seenSet = new HashSet<>();
        if (user != null) {
            seenSet.addAll(eventRepo.findProductIdsAfter(
                    user, EventType.VIEW, LocalDateTime.now().minusDays(3)
            ));
        }

        List<Long> excludeSoft = new ArrayList<>(seenSet);
        boolean excludeSoftEmpty = excludeSoft.isEmpty();

        // 2) ✅ CACHED: interest map (30min TTL)
        Map<String, Double> catScore = (user != null)
            ? userInterestCacheService.getCategoryScores(user)
            : new HashMap<>();

        Map<String, Double> brandScore = (user != null)
            ? userInterestCacheService.getBrandScores(user)
            : new HashMap<>();

        // 3) candidates: PERSONAL (interest) + EXPLORE (global)
        List<Product> personalCandidates = new ArrayList<>();
        List<Product> exploreCandidates = new ArrayList<>();

        if (user != null) {
            // ✅ CACHED: top categories (30min TTL)
            List<String> catKeys = userInterestCacheService.getTopCategoryKeys(user);
            List<Category> cats = new ArrayList<>();
            for (String k : catKeys) {
                if (k == null) continue;
                try { cats.add(Category.valueOf(k.trim().toUpperCase())); }
                catch (IllegalArgumentException ignore) {}
            }
            if (!cats.isEmpty()) {
                personalCandidates.addAll(productRepo.candidatesByCategories(
                        cats, excludeSoft, excludeSoftEmpty, PageRequest.of(0, Math.min(fetch, 200))
                ));
            }

            // ✅ CACHED: top brands (30min TTL)
            List<String> brands = userInterestCacheService.getTopBrandKeys(user)
                    .stream()
                    .filter(s -> !s.isBlank())
                    .toList();

            if (!brands.isEmpty()) {
                personalCandidates.addAll(productRepo.candidatesByBrands(
                        brands, excludeSoft, excludeSoftEmpty, PageRequest.of(0, Math.min(fetch, 200))
                ));
            }
        }

        // EXPLORE: discounted + popular + new (seen exclude bilan)
        exploreCandidates.addAll(productRepo.discountedCandidates(
                excludeSoft, excludeSoftEmpty, PageRequest.of(0, Math.min(fetch, 200))
        ));
        exploreCandidates.addAll(productRepo.findByActiveTrueOrderBySoldCountDesc(
                PageRequest.of(0, Math.min(fetch, 200))
        ).getContent());
        exploreCandidates.addAll(productRepo.findByActiveTrueOrderByCreatedAtDesc(
                PageRequest.of(0, Math.min(fetch, 120))
        ).getContent());

        // ✅ maxPerBrand: count() yo‘q — pool size bo‘yicha taxmin
        int poolSizeGuess = personalCandidates.size() + exploreCandidates.size();
        int maxPerBrand = (poolSizeGuess <= 80) ? 6 : 3;

        // 4) scoring
        double seenPenalty = calcSeenPenalty(poolSizeGuess); // kichik katalogda yumshoq
        List<ScoredProduct> personalScored = scoreAndSort(personalCandidates, catScore, brandScore, seenSet, seenPenalty);
        List<ScoredProduct> exploreScored = scoreAndSort(exploreCandidates, catScore, brandScore, seenSet, seenPenalty);

        // 5) blend 70/30 (agar personal bo‘sh bo‘lsa, hammasi explore)
        int personalCount = (user == null || personalScored.isEmpty())
                ? 0
                : (int) Math.round(limit * 0.7);

        int exploreCount = limit - personalCount;

        List<Product> blended = blendDiversifiedGlobalBrand(
                personalScored, exploreScored,
                personalCount, exploreCount,
                maxPerBrand
        );

        // 6) Fallback: seen exclude sabab yetmasa -> seen ni qaytaramiz (exclude yo‘q)
        if (blended.size() < limit) {
            List<Product> more = productRepo.findByActiveTrueOrderBySoldCountDesc(
                    PageRequest.of(0, Math.max(fetch * 2, 120))
            ).getContent();

            List<ScoredProduct> moreScored = scoreAndSort(
                    more, catScore, brandScore, Collections.emptySet(), 0.0
            );

            blended = mergeFill(blended, moreScored, limit, maxPerBrand);
        }

        // 7) Top pool shuffle
        blended = shuffleTopPool(blended, limit, rnd);

        // 8) impressions log


        // 9) toCard (✅ batch: N+1 yo‘q)
        List<Product> picked = blended.stream().limit(limit).toList();
        return productService.toCardsPublic(picked, user);
    }

    private double calcSeenPenalty(int poolSizeGuess) {
        // kichik katalogda yumshoq
        if (poolSizeGuess <= 80) return 10.0;
        return 20.0;
    }

    private List<ScoredProduct> scoreAndSort(List<Product> list,
                                             Map<String, Double> catScore,
                                             Map<String, Double> brandScore,
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
            scored.add(new ScoredProduct(p, score(p, catScore, brandScore, seenSet, seenPenaltyValue)));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored;
    }

    private double score(Product p,
                         Map<String, Double> catScore,
                         Map<String, Double> brandScore,
                         Set<Long> seenSet,
                         double seenPenaltyValue) {

        double c = 0.0;
        if (p.getCategory() != null) {
            c = catScore.getOrDefault(p.getCategory().name(), 0.0);
        }

        double b = 0.0;
        if (p.getBrand() != null) {
            String key = p.getBrand().trim().toLowerCase();
            b = brandScore.getOrDefault(key, 0.0);
        }

        double popularity = (p.getSoldCount() * 3.0) + p.getViewCount();

        double discountBoost = 0.0;
        BigDecimal price = p.getPrice();
        BigDecimal discount = p.getDiscountPrice();

        if (price != null
                && discount != null
                && price.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(price) < 0) {

            BigDecimal pct = price.subtract(discount)
                    .divide(price, 6, RoundingMode.HALF_UP);
            discountBoost = pct.doubleValue() * 10.0;
        }

        double stockPenalty = (p.getStock() <= 0) ? -50.0 : 0.0;
        double todayDealBoost = p.isTodayDeal() ? 5.0 : 0.0;

        double seenPenalty = (seenSet != null && p.getId() != null && seenSet.contains(p.getId()))
                ? seenPenaltyValue
                : 0.0;

        return 3.0 * c
                + 2.0 * b
                + 0.02 * popularity
                + discountBoost
                + todayDealBoost
                + stockPenalty
                - seenPenalty;
    }

    /**
     * personal + explore aralash:
     * - brand diversity GLOBAL (reset bo‘lmaydi)
     * - dedupe GLOBAL
     */
    private List<Product> blendDiversifiedGlobalBrand(List<ScoredProduct> personal,
                                                      List<ScoredProduct> explore,
                                                      int personalCount,
                                                      int exploreCount,
                                                      int maxPerBrand) {

        int target = personalCount + exploreCount;

        List<Product> out = new ArrayList<>(target);
        Set<Long> used = new HashSet<>(target * 2);
        Map<String, Integer> brandCount = new HashMap<>();

        addWithBrandDiversity(out, used, brandCount, personal, personalCount, maxPerBrand);
        addWithBrandDiversity(out, used, brandCount, explore, exploreCount, maxPerBrand);

        // agar hali ham kam bo‘lsa, explore’dan yana to‘ldiramiz
        if (out.size() < target) {
            addWithBrandDiversity(out, used, brandCount, explore, target - out.size(), maxPerBrand);
        }

        return out;
    }

    private void addWithBrandDiversity(List<Product> out,
                                       Set<Long> used,
                                       Map<String, Integer> brandCount,
                                       List<ScoredProduct> scored,
                                       int need,
                                       int maxPerBrand) {

        if (need <= 0 || scored == null || scored.isEmpty()) return;

        for (ScoredProduct sp : scored) {
            if (need <= 0) break;
            Product p = sp.p;
            if (p == null || p.getId() == null) continue;
            if (!used.add(p.getId())) continue;

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            int cnt = brandCount.getOrDefault(brand, 0);

            if (cnt >= maxPerBrand) {
                // revert used.add
                used.remove(p.getId());
                continue;
            }

            out.add(p);
            brandCount.put(brand, cnt + 1);
            need--;
        }
    }

    /**
     * Fallback merge: mavjud list + yangi scoreddan limitgacha to‘ldirish
     */
    private List<Product> mergeFill(List<Product> base,
                                    List<ScoredProduct> more,
                                    int limit,
                                    int maxPerBrand) {

        LinkedHashMap<Long, Product> uniq = new LinkedHashMap<>(limit * 2);
        Map<String, Integer> brandCount = new HashMap<>();

        for (Product p : base) {
            if (p == null || p.getId() == null) continue;
            uniq.putIfAbsent(p.getId(), p);

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            brandCount.put(brand, brandCount.getOrDefault(brand, 0) + 1);
        }

        for (ScoredProduct sp : more) {
            if (uniq.size() >= limit) break;
            Product p = sp.p;
            if (p == null || p.getId() == null) continue;
            if (uniq.containsKey(p.getId())) continue;

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            int cnt = brandCount.getOrDefault(brand, 0);
            if (cnt >= maxPerBrand) continue;

            uniq.put(p.getId(), p);
            brandCount.put(brand, cnt + 1);
        }

        // agar brand limit sabab to‘lmasa, brand limitni ignore qilib to‘ldiramiz
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