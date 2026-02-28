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
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeFeedService {

    private final UserService userService;
    private final UserInterestRepository interestRepo;
    private final ProductRepository productRepo;
    private final ProductService productService;

    // ✅ NEW
    private final EventLogRepository eventRepo;
    private final NegativeFeedbackService negativeFeedbackService;

    @Transactional(readOnly = true)
    public List<ProductCardResponse> buildFeed(int limit) {

        User user = userService.getCurrentUserOrNull();

        // ✅ 0) Negative feedback (6 soatda 1 marta) - user bo‘lsa
        if (user != null) {
            negativeFeedbackService.applyIfNeeded(user);
        }

        // ✅ 1) seenRecently (oxirgi 3 kunda ko‘rilganlar)
        Set<Long> seenSet = new HashSet<>();
        if (user != null) {
            List<Long> seenIds = eventRepo.findProductIdsAfter(
                    user,
                    EventType.VIEW,
                    LocalDateTime.now().minusDays(3)
            );
            seenSet.addAll(seenIds);
        }

        // ✅ 2) interest map (fast lookup)
        Map<String, Double> catScore = new HashMap<>();
        Map<String, Double> brandScore = new HashMap<>();
        if (user != null) {
            interestRepo.findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.CATEGORY)
                    .forEach(x -> {
                        if (x.getKey() != null) catScore.put(x.getKey(), x.getScore());
                    });

            interestRepo.findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.BRAND)
                    .forEach(x -> {
                        if (x.getKey() != null) brandScore.put(x.getKey().trim().toLowerCase(), x.getScore());
                    });
        }

        // ✅ 3) Candidate lists: PERSONAL vs EXPLORE
        List<Product> personalCandidates = new ArrayList<>();
        List<Product> exploreCandidates = new ArrayList<>();

        List<Long> usedPersonal = new ArrayList<>();
        List<Long> usedExplore = new ArrayList<>();

        if (user != null) {
            // top categories
            List<String> catKeys = interestRepo.topKeys(user, InterestType.CATEGORY, PageRequest.of(0, 4));

            List<Category> cats = new ArrayList<>();
            for (String k : catKeys) {
                if (k == null) continue;
                try {
                    cats.add(Category.valueOf(k.trim().toUpperCase()));
                } catch (IllegalArgumentException ignore) {}
            }

            if (!cats.isEmpty()) {
                personalCandidates.addAll(productRepo.candidatesByCategories(
                        cats, usedPersonal, usedPersonal.isEmpty(), PageRequest.of(0, 120)
                ));
            }

            // top brands
            List<String> brands = interestRepo.topKeys(user, InterestType.BRAND, PageRequest.of(0, 4))
                    .stream()
                    .filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isBlank())
                    .toList();

            if (!brands.isEmpty()) {
                personalCandidates.addAll(productRepo.candidatesByBrands(
                        brands, usedPersonal, usedPersonal.isEmpty(), PageRequest.of(0, 120)
                ));
            }
        }

        // Explore/trending/discounts (global)
        exploreCandidates.addAll(productRepo.discountedCandidates(usedExplore, usedExplore.isEmpty(), PageRequest.of(0, 120)));
        exploreCandidates.addAll(productRepo.findByActiveTrueOrderBySoldCountDesc(PageRequest.of(0, 160)).getContent());

        // ✅ 4) Score + sort
        List<ScoredProduct> personalScored = scoreAndSort(personalCandidates, catScore, brandScore, seenSet);
        List<ScoredProduct> exploreScored = scoreAndSort(exploreCandidates, catScore, brandScore, seenSet);

        // ✅ 5) Blend 70/30
        int personalCount = (int) Math.round(limit * 0.7);
        int exploreCount = limit - personalCount;

        List<Product> blended = blendDiversified(personalScored, exploreScored, personalCount, exploreCount);

        // ✅ 6) toCard
        return blended.stream()
                .limit(limit)
                .map(p -> productService.toCardPublic(p, user))
                .toList();
    }

    private List<ScoredProduct> scoreAndSort(List<Product> list,
                                             Map<String, Double> catScore,
                                             Map<String, Double> brandScore,
                                             Set<Long> seenSet) {

        // dedupe
        Map<Long, Product> uniq = new LinkedHashMap<>();
        for (Product p : list) {
            if (p == null || p.getId() == null) continue;
            uniq.putIfAbsent(p.getId(), p);
        }

        return uniq.values().stream()
                .map(p -> new ScoredProduct(p, score(p, catScore, brandScore, seenSet)))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .toList();
    }

    private double score(Product p,
                         Map<String, Double> catScore,
                         Map<String, Double> brandScore,
                         Set<Long> seenSet) {

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
                    .divide(price, 6, RoundingMode.HALF_UP); // 0..1
            discountBoost = pct.doubleValue() * 10.0;
        }

        double stockPenalty = (p.getStock() <= 0) ? -50.0 : 0.0;
        double todayDealBoost = p.isTodayDeal() ? 5.0 : 0.0;

        // ✅ seen penalty (oxirgi 3 kunda ko‘rilgan bo‘lsa pastga tushadi)
        double seenPenalty = (seenSet != null && p.getId() != null && seenSet.contains(p.getId())) ? 20.0 : 0.0;

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
     * - avval personalCount
     * - keyin exploreCount
     * - finalda brand diversity + dedupe
     */
    private List<Product> blendDiversified(List<ScoredProduct> personal,
                                           List<ScoredProduct> explore,
                                           int personalCount,
                                           int exploreCount) {

        List<Product> out = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        // 1) personal
        addWithBrandDiversity(out, used, personal, personalCount);

        // 2) explore
        addWithBrandDiversity(out, used, explore, exploreCount);

        return out;
    }

    private void addWithBrandDiversity(List<Product> out,
                                       Set<Long> used,
                                       List<ScoredProduct> scored,
                                       int need) {

        Map<String, Integer> brandCount = new HashMap<>();

        for (ScoredProduct sp : scored) {
            if (need <= 0) break;
            Product p = sp.p;
            if (p == null || p.getId() == null) continue;
            if (used.contains(p.getId())) continue;

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            int cnt = brandCount.getOrDefault(brand, 0);

            if (cnt >= 3) continue;

            out.add(p);
            used.add(p.getId());
            brandCount.put(brand, cnt + 1);
            need--;
        }
    }

    private record ScoredProduct(Product p, double score) {}
}