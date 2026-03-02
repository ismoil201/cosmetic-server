package com.example.backend.service;

import com.example.backend.dto.ProductCardResponse;
import com.example.backend.dto.ProductRecommendResponse;
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
public class ProductRecommendService {

    private final UserService userService;
    private final ProductRepository productRepo;
    private final ProductService productService;

    private final EventLogRepository eventRepo;
    private final UserInterestRepository interestRepo;
    private final NegativeFeedbackService negativeFeedbackService;

    @Transactional(readOnly = true)
    public ProductRecommendResponse recommendForDetail(Long productId,
                                                       int similarLimit,
                                                       int othersLimit,
                                                       String seed) {

        User user = userService.getCurrentUserOrNull();

        // Negative feedback (6 soatda 1 marta)
        if (user != null) negativeFeedbackService.applyIfNeeded(user);

        Product base = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // seed fallback
        if (seed == null || seed.isBlank()) seed = String.valueOf(System.currentTimeMillis());

        // seen (oxirgi 7 kun ko‘rilganlar) -> "others"da takrorni kesadi
        Set<Long> seen = new HashSet<>();
        if (user != null) {
            seen.addAll(eventRepo.findProductIdsAfter(
                    user, EventType.VIEW, LocalDateTime.now().minusDays(7)
            ));
        }

        // SIMILAR
        List<Product> similar = buildSimilar(base, similarLimit);

        // OTHERS
        List<Product> others = buildOthers(base, user, othersLimit, similar, seen, seed);

        // to card
        List<ProductCardResponse> simCards = similar.stream()
                .limit(similarLimit)
                .map(p -> productService.toCardPublic(p, user))
                .toList();

        List<ProductCardResponse> otherCards = others.stream()
                .limit(othersLimit)
                .map(p -> productService.toCardPublic(p, user))
                .toList();

        return new ProductRecommendResponse(simCards, otherCards);
    }

    /**
     * Similar: category + brand + price band yaqin + popularity/discount bilan rerank
     */
    private List<Product> buildSimilar(Product base, int limit) {

        Set<Long> exclude = new HashSet<>();
        exclude.add(base.getId());

        // candidates yig‘amiz
        List<Product> candidates = new ArrayList<>();

        // 1) category + price band (±15%)
        if (base.getCategory() != null && base.getPrice() != null && base.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal price = base.getPrice();
            BigDecimal min = price.multiply(BigDecimal.valueOf(0.85)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal max = price.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.HALF_UP);

            candidates.addAll(productRepo.activeByCategoryAndPriceBand(
                    base.getCategory(), min, max,
                    new ArrayList<>(exclude), exclude.isEmpty(),
                    PageRequest.of(0, 160)
            ));
        }

        // 2) category top (ko‘proq olish)
        if (base.getCategory() != null) {
            candidates.addAll(productRepo.activeByCategories(
                    List.of(base.getCategory()),
                    new ArrayList<>(exclude), exclude.isEmpty(),
                    PageRequest.of(0, 200)
            ));
        }

        // 3) brand top
        if (base.getBrand() != null && !base.getBrand().isBlank()) {
            candidates.addAll(productRepo.activeByBrands(
                    List.of(base.getBrand().trim().toLowerCase()),
                    new ArrayList<>(exclude), exclude.isEmpty(),
                    PageRequest.of(0, 200)
            ));
        }

        // uniq + score + top
        return scoreSimilarAndPick(base, candidates, limit);
    }

    private List<Product> scoreSimilarAndPick(Product base, List<Product> candidates, int limit) {

        Map<Long, Product> uniq = new LinkedHashMap<>();
        for (Product p : candidates) {
            if (p == null || p.getId() == null) continue;
            if (p.getId().equals(base.getId())) continue;
            if (!p.isActive()) continue;
            uniq.putIfAbsent(p.getId(), p);
        }

        BigDecimal basePrice = base.getPrice();

        record SP(Product p, double s) {}

        List<SP> scored = new ArrayList<>();
        for (Product p : uniq.values()) {

            double s = 0.0;

            // category match
            if (base.getCategory() != null && p.getCategory() == base.getCategory()) s += 3.0;

            // brand match
            if (base.getBrand() != null && p.getBrand() != null) {
                if (base.getBrand().trim().equalsIgnoreCase(p.getBrand().trim())) s += 2.0;
            }

            // price closeness
            if (basePrice != null && p.getPrice() != null && basePrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal diff = p.getPrice().subtract(basePrice).abs();
                BigDecimal ratio = diff.divide(basePrice, 6, RoundingMode.HALF_UP); // 0..?
                double closeness = Math.max(0, 1.0 - ratio.doubleValue()); // 1 best
                s += closeness * 2.0;
            }

            // discount boost
            s += discountBoost(p);

            // popularity
            double popularity = (p.getSoldCount() * 3.0) + p.getViewCount();
            s += 0.02 * popularity;

            // today deal
            if (p.isTodayDeal()) s += 2.5;

            // stock penalty
            if (p.getStock() <= 0) s -= 50.0;

            scored.add(new SP(p, s));
        }

        scored.sort((a, b) -> Double.compare(b.s, a.s));

        List<Product> out = new ArrayList<>();
        for (SP sp : scored) {
            if (out.size() >= limit) break;
            out.add(sp.p);
        }
        return out;
    }

    /**
     * Others: user interest (category/brand) + trending/discount/new/hit mix + seen+dedupe
     */
    private List<Product> buildOthers(Product base,
                                      User user,
                                      int limit,
                                      List<Product> similar,
                                      Set<Long> seen,
                                      String seed) {

        // 0) hard exclude (doim): base + similar
        Set<Long> excludeHard = new HashSet<>();
        excludeHard.add(base.getId());
        for (Product p : similar) {
            if (p != null && p.getId() != null) excludeHard.add(p.getId());
        }

        // 1) soft exclude: hard + seen (agar seen bo‘lsa)
        Set<Long> excludeSoft = new HashSet<>(excludeHard);
        if (seen != null) excludeSoft.addAll(seen);

        // 2) 1-urinish: excludeSoft
        List<Product> first = buildOthersInternal(user, limit, seed, excludeSoft);

        // 3) agar yetmasa -> 2-urinish: excludeHard (seen ni qaytarib yuboramiz)
        if (first.size() < limit) {
            List<Product> second = buildOthersInternal(user, limit, seed, excludeHard);

            // merge qilib dedupe qilib qaytaramiz (avval first, keyin second)
            LinkedHashMap<Long, Product> merged = new LinkedHashMap<>();
            for (Product p : first) {
                if (p != null && p.getId() != null) merged.putIfAbsent(p.getId(), p);
            }
            for (Product p : second) {
                if (p != null && p.getId() != null) merged.putIfAbsent(p.getId(), p);
            }
            return merged.values().stream().limit(limit).toList();
        }

        return first;
    }



    private double discountBoost(Product p) {
        BigDecimal price = p.getPrice();
        BigDecimal discount = p.getDiscountPrice();

        if (price == null || discount == null) return 0.0;
        if (price.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        if (discount.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        if (discount.compareTo(price) >= 0) return 0.0;

        BigDecimal pct = price.subtract(discount).divide(price, 6, RoundingMode.HALF_UP);
        return pct.doubleValue() * 10.0;
    }

    private record SP(Product p, double score) {}

    private List<SP> scoreByInterest(List<Product> list,
                                     Map<String, Double> catScore,
                                     Map<String, Double> brandScore) {

        Map<Long, Product> uniq = new LinkedHashMap<>();
        for (Product p : list) {
            if (p == null || p.getId() == null) continue;
            if (!p.isActive()) continue;
            uniq.putIfAbsent(p.getId(), p);
        }

        List<SP> scored = new ArrayList<>();
        for (Product p : uniq.values()) {

            double c = 0.0;
            if (p.getCategory() != null) c = catScore.getOrDefault(p.getCategory().name(), 0.0);

            double b = 0.0;
            if (p.getBrand() != null) b = brandScore.getOrDefault(p.getBrand().trim().toLowerCase(), 0.0);

            double popularity = (p.getSoldCount() * 3.0) + p.getViewCount();
            double todayDealBoost = p.isTodayDeal() ? 5.0 : 0.0;
            double stockPenalty = (p.getStock() <= 0) ? -50.0 : 0.0;

            double s = 3.0 * c
                    + 2.0 * b
                    + 0.02 * popularity
                    + discountBoost(p)
                    + todayDealBoost
                    + stockPenalty;

            scored.add(new SP(p, s));
        }

        scored.sort((a, bb) -> Double.compare(bb.score, a.score));
        return scored;
    }

    /**
     * 70/30 + seed shuffle + brand diversity (max 3 per brand)
     */
    private List<Product> blendWithBrandDiversity(List<SP> personal,
                                                  List<SP> explore,
                                                  int limit,
                                                  String seed,
                                                  int maxPerBrand) {

        int personalCount = (int) Math.round(limit * 0.7);
        int exploreCount = limit - personalCount;

        List<Product> merged = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        addTop(merged, used, personal, personalCount);
        addTop(merged, used, explore, exploreCount);

        long s = seed == null ? System.currentTimeMillis() : seed.hashCode();
        Collections.shuffle(merged, new Random(s));

        List<Product> out = new ArrayList<>();
        Map<String, Integer> brandCount = new HashMap<>();

        for (Product p : merged) {
            if (out.size() >= limit) break;
            if (p == null || p.getId() == null) continue;

            String brand = (p.getBrand() == null) ? "" : p.getBrand().trim().toLowerCase();
            int cnt = brandCount.getOrDefault(brand, 0);
            if (cnt >= maxPerBrand) continue;

            out.add(p);
            brandCount.put(brand, cnt + 1);
        }

        // fill if needed
        if (out.size() < limit) {
            for (Product p : merged) {
                if (out.size() >= limit) break;
                if (p == null || p.getId() == null) continue;
                boolean exists = out.stream().anyMatch(x -> x.getId().equals(p.getId()));
                if (!exists) out.add(p);
            }
        }

        return out;
    }
    private List<Product> buildOthersInternal(User user,
                                              int limit,
                                              String seed,
                                              Set<Long> excludeSet) {

        List<Long> excludeIds = new ArrayList<>(excludeSet);
        int excludeEmpty = excludeIds.isEmpty() ? 1 : 0;

        // interest maps
        Map<String, Double> catScore = new HashMap<>();
        Map<String, Double> brandScore = new HashMap<>();

        List<Product> personal = new ArrayList<>();
        List<Product> explore = new ArrayList<>();

        if (user != null) {
            interestRepo.findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.CATEGORY)
                    .forEach(x -> { if (x.getKey() != null) catScore.put(x.getKey(), x.getScore()); });

            interestRepo.findTop20ByUserAndTypeOrderByScoreDesc(user, InterestType.BRAND)
                    .forEach(x -> {
                        if (x.getKey() != null) brandScore.put(x.getKey().trim().toLowerCase(), x.getScore());
                    });

            // top categories
            List<String> catKeys = interestRepo.topKeys(user, InterestType.CATEGORY, PageRequest.of(0, 4));
            List<Category> cats = new ArrayList<>();
            for (String k : catKeys) {
                if (k == null) continue;
                try { cats.add(Category.valueOf(k.trim().toUpperCase())); }
                catch (IllegalArgumentException ignore) {}
            }
            if (!cats.isEmpty()) {
                personal.addAll(productRepo.candidatesByCategories(
                        cats, excludeIds, excludeIds.isEmpty(), PageRequest.of(0, 200)
                ));
            }

            // top brands
            List<String> brands = interestRepo.topKeys(user, InterestType.BRAND, PageRequest.of(0, 4))
                    .stream().filter(Objects::nonNull)
                    .map(s -> s.trim().toLowerCase())
                    .filter(s -> !s.isBlank())
                    .toList();

            if (!brands.isEmpty()) {
                personal.addAll(productRepo.candidatesByBrands(
                        brands, excludeIds, excludeIds.isEmpty(), PageRequest.of(0, 200)
                ));
            }
        }

        // Explore: Temu-style mix
        explore.addAll(productRepo.hitsShuffledExclude(seed, excludeIds, excludeEmpty, Math.min(8, limit)));
        explore.addAll(productRepo.discountsShuffledExclude(seed, excludeIds, excludeEmpty, Math.min(10, limit)));
        explore.addAll(productRepo.newArrivalsExclude(excludeIds, excludeEmpty, Math.min(10, limit)));
        explore.addAll(productRepo.popularExclude(excludeIds, excludeEmpty, PageRequest.of(0, 4)).getContent());

        // score and blend 70/30
        List<SP> personalScored = scoreByInterest(personal, catScore, brandScore);
        List<SP> exploreScored = scoreByInterest(explore, catScore, brandScore);

        // ✅ kichik catalog uchun brand limit yumshoq bo‘lsin
        long total = productRepo.count(); // 50 ta bo‘lsa bu arzon
        int maxPerBrand = (total <= 80) ? 6 : 3;

        return blendWithBrandDiversity(personalScored, exploreScored, limit, seed, maxPerBrand);
    }

    private void addTop(List<Product> out, Set<Long> used, List<SP> scored, int need) {
        for (SP sp : scored) {
            if (need <= 0) break;
            Product p = sp.p;
            if (p == null || p.getId() == null) continue;
            if (used.contains(p.getId())) continue;
            out.add(p);
            used.add(p.getId());
            need--;
        }
    }
}