package com.example.backend.domain.product.service;

import com.example.backend.global.util.SearchNormalizer;
import com.example.backend.domain.event.service.EventTrackingService;
import com.example.backend.domain.favorite.repository.FavoriteRepository;
import com.example.backend.domain.product.dto.*;
import com.example.backend.domain.product.entity.*;
import com.example.backend.domain.product.repository.*;
import com.example.backend.domain.search.entity.SearchLog;
import com.example.backend.domain.search.repository.SearchLogRepository;
import com.example.backend.domain.seller.entity.Seller;
import com.example.backend.domain.user.entity.User;
import com.example.backend.global.exception.NotFoundException;
import com.example.backend.domain.recommendation.service.InterestService;
import com.example.backend.domain.seller.service.SellerService;
import com.example.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final FavoriteRepository favRepo;
    private final UserService userService;
    private final ProductImageRepository productImageRepo;
    private final ProductDetailImageRepository detailImageRepo;
    private final InterestService interestService;
    private final EventTrackingService eventTrackingService;
    private final SellerService sellerService;
    private final SearchLogRepository searchLogRepo;

    private final ProductVariantRepository variantRepo;
    private final VariantTierPriceRepository tierRepo;

    /* ================= CREATE ================= */

    @Transactional
    public void create(ProductCreateRequest req) {
        Seller seller = sellerService.requireCurrentSeller();

        Product product = new Product();
        product.setSeller(seller);

        map(req, product);
        productRepo.save(product);

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());
    }

    /* ================= DELETE ================= */

    @Transactional
    public void delete(Long id) {
        productImageRepo.deleteByProductId(id);
        detailImageRepo.deleteByProductId(id);
        productRepo.deleteById(id);
    }

    /* ================= HOME (FIXED N+1) ================= */

    @Transactional(readOnly = true)
    public Page<ProductResponse> getHomeProducts(Pageable pageable) {

        User user = userService.getCurrentUserOrNull();
        Page<Product> page = productRepo.findByActiveTrue(pageable);

        List<Product> products = page.getContent();
        if (products.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }

        List<Long> ids = products.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .toList();

        // ✅ favorites in 1 query
        Set<Long> favIds = new HashSet<>();
        if (user != null && !ids.isEmpty()) {
            favIds.addAll(favRepo.findFavoriteProductIds(user, ids));
        }

        // ✅ images in 1 query
        List<ProductImage> allImages = productImageRepo.findByProductIdInOrderByMainDescIdAsc(ids);
        Map<Long, List<ProductImageResponse>> imagesMap = new HashMap<>();

        for (ProductImage img : allImages) {
            Long pid = img.getProduct() != null ? img.getProduct().getId() : null;
            if (pid == null) continue;

            imagesMap.computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(new ProductImageResponse(img.getImageUrl(), img.isMain()));
        }

        // build ProductResponse list (no extra DB queries)
        List<ProductResponse> out = products.stream()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getBrand(),
                        p.getPrice(),
                        p.getDiscountPrice(),
                        p.getCategory(),
                        p.getRatingAvg(),
                        p.getReviewCount(),
                        p.getSoldCount(),
                        p.isTodayDeal(),
                        user != null && favIds.contains(p.getId()),
                        p.getStock(),
                        imagesMap.getOrDefault(p.getId(), List.of())
                ))
                .toList();

        return new PageImpl<>(out, page.getPageable(), page.getTotalElements());
    }

    /* ================= DETAIL ================= */

    @Transactional
    public ProductDetailResponse getDetail(Long productId) {

        User user = userService.getCurrentUserOrNull();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        // view count increment
        p.setViewCount(p.getViewCount() + 1);

        if (user != null) {
            eventTrackingService.logView(user, p);
            interestService.onView(user, p);
        }

        boolean favorite = false;
        if (user != null) {
            favorite = favRepo.existsByUserAndProduct(user, p);
        }

        List<ProductImageResponse> images =
                productImageRepo.findByProductId(p.getId())
                        .stream()
                        .map(img -> new ProductImageResponse(img.getImageUrl(), img.isMain()))
                        .toList();

        List<ProductDetailImageResponse> detailImages =
                detailImageRepo.findByProductIdOrderBySortOrderAsc(p.getId())
                        .stream()
                        .map(img -> new ProductDetailImageResponse(img.getImageUrl(), img.getSortOrder()))
                        .toList();

        // ================== VARIANTS (NO N+1) ==================

        List<ProductVariant> variantEntities =
                variantRepo.findByProductIdAndActiveTrueOrderBySortOrderAscIdAsc(p.getId());

        List<Long> variantIds = variantEntities.stream()
                .map(ProductVariant::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<VariantTierResponse>> tiersMap;

        if (!variantIds.isEmpty()) {
            List<VariantTierPrice> allTiers =
                    tierRepo.findAllByVariantIdInOrderByVariantIdAscMinQtyAsc(variantIds);

            tiersMap = allTiers.stream()
                    .collect(Collectors.groupingBy(
                            t -> t.getVariant().getId(),
                            HashMap::new, // optional: map turi aniq bo‘lsin
                            Collectors.mapping(
                                    t -> new VariantTierResponse(t.getMinQty(), t.getTotalPrice()),
                                    Collectors.toList()
                            )
                    ));
        } else {
            tiersMap = new HashMap<>();
        }
        List<ProductVariantResponse> variants = variantEntities.stream()
                .map(v -> new ProductVariantResponse(
                        v.getId(),
                        v.getLabel(),
                        v.getPrice(),
                        v.getDiscountPrice(),
                        v.getStock(),
                        tiersMap.getOrDefault(v.getId(), List.of())
                ))
                .toList();

        // persist viewCount
        productRepo.save(p);

        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getCategory(),
                p.getStock(),
                p.getRatingAvg(),
                p.getReviewCount(),
                p.getSoldCount(),
                p.isTodayDeal(),
                favorite,
                images,
                detailImages,
                variants
        );
    }
    public ProductCardResponse toCardPublic(Product p, User user) {
        // eski private toCard(...) ni ishlatadi
        return toCard(p, user);
    }
    /* ================= TODAY DEAL BANNER ================= */

    @Transactional(readOnly = true)
    public List<ProductResponse> getTodayDeals() {

        User user = userService.getCurrentUserOrNull();

        List<Product> products = productRepo.findByIsTodayDealTrueAndActiveTrue();
        if (products.isEmpty()) return List.of();

        // (optional) N+1 ni kamaytirish: batch images + fav
        List<Long> ids = products.stream().map(Product::getId).filter(Objects::nonNull).toList();

        Set<Long> favIds = new HashSet<>();
        if (user != null && !ids.isEmpty()) {
            favIds.addAll(favRepo.findFavoriteProductIds(user, ids));
        }

        List<ProductImage> allImages = productImageRepo.findByProductIdInOrderByMainDescIdAsc(ids);
        Map<Long, List<ProductImageResponse>> imagesMap = new HashMap<>();
        for (ProductImage img : allImages) {
            Long pid = img.getProduct() != null ? img.getProduct().getId() : null;
            if (pid == null) continue;
            imagesMap.computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(new ProductImageResponse(img.getImageUrl(), img.isMain()));
        }

        return products.stream()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getBrand(),
                        p.getPrice(),
                        p.getDiscountPrice(),
                        p.getCategory(),
                        p.getRatingAvg(),
                        p.getReviewCount(),
                        p.getSoldCount(),
                        p.isTodayDeal(),
                        user != null && favIds.contains(p.getId()),
                        p.getStock(),
                        imagesMap.getOrDefault(p.getId(), List.of())
                ))
                .toList();
    }

    /* ================= UPDATE ================= */

    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        map(req, product);
        productRepo.save(product);

        productImageRepo.deleteByProductId(product.getId());
        detailImageRepo.deleteByProductId(product.getId());

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());
    }

    /* ================= CATEGORY CARDS (ALREADY BATCH) ================= */

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getByCategoryCards(Category category, Pageable pageable) {
        User user = userService.getCurrentUserOrNull();

        Page<Product> page = productRepo.findByCategoryAndActiveTrueOrderByCreatedAtDesc(category, pageable);
        List<ProductCardResponse> cards = toCardsPublic(page.getContent(), user);

        return new PageImpl<>(cards, page.getPageable(), page.getTotalElements());
    }

    /* ================= GET PRODUCTS BY IDS (OPTIONAL BATCH) ================= */

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids) {

        User user = userService.getCurrentUserOrNull();
        if (ids == null || ids.isEmpty()) return List.of();

        List<Product> products = productRepo.findByIdInAndActiveTrue(ids);
        if (products.isEmpty()) return List.of();

        List<Long> pids = products.stream().map(Product::getId).filter(Objects::nonNull).toList();

        Set<Long> favIds = new HashSet<>();
        if (user != null && !pids.isEmpty()) {
            favIds.addAll(favRepo.findFavoriteProductIds(user, pids));
        }

        List<ProductImage> allImages = productImageRepo.findByProductIdInOrderByMainDescIdAsc(pids);
        Map<Long, List<ProductImageResponse>> imagesMap = new HashMap<>();
        for (ProductImage img : allImages) {
            Long pid = img.getProduct() != null ? img.getProduct().getId() : null;
            if (pid == null) continue;
            imagesMap.computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(new ProductImageResponse(img.getImageUrl(), img.isMain()));
        }

        return products.stream()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getBrand(),
                        p.getPrice(),
                        p.getDiscountPrice(),
                        p.getCategory(),
                        p.getRatingAvg(),
                        p.getReviewCount(),
                        p.getSoldCount(),
                        p.isTodayDeal(),
                        user != null && favIds.contains(p.getId()),
                        p.getStock(),
                        imagesMap.getOrDefault(p.getId(), List.of())
                ))
                .toList();
    }

    /* ================= HELPERS ================= */

    private void saveImages(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        for (int i = 0; i < imageUrls.size(); i++) {
            String url = imageUrls.get(i);
            if (url == null || url.isBlank()) continue;

            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl(url);
            img.setMain(i == 0);
            productImageRepo.save(img);
        }
    }

    private ProductCardResponse toCard(Product p, User user) {
        boolean favorite = (user != null) && favRepo.existsByUserAndProduct(user, p);

        List<ProductImageResponse> images = productImageRepo
                .findByProductIdOrderByMainDescIdAsc(p.getId())
                .stream()
                .map(img -> new ProductImageResponse(img.getImageUrl(), img.isMain()))
                .toList();

        ProductImageResponse mainImage = productImageRepo
                .findFirstByProductIdAndMainTrue(p.getId())
                .or(() -> productImageRepo.findFirstByProductIdOrderByIdAsc(p.getId()))
                .map(img -> new ProductImageResponse(img.getImageUrl(), img.isMain()))
                .orElse(null);

        return new ProductCardResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getCategory(),
                p.getRatingAvg(),
                p.getReviewCount(),
                p.getSoldCount(),
                p.isTodayDeal(),
                favorite,
                p.getStock(),
                mainImage,
                images
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getPopular(Pageable pageable) {
        User user = userService.getCurrentUserOrNull();
        return productRepo.findByActiveTrueOrderBySoldCountDesc(pageable)
                .map(p -> toCard(p, user));
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> getHits(int limit) {
        User user = userService.getCurrentUserOrNull();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findByIsTodayDealTrueAndActiveTrue(pageable)
                .map(p -> toCard(p, user))
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> getDiscounts(int limit) {
        User user = userService.getCurrentUserOrNull();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findDiscounted(pageable)
                .map(p -> toCard(p, user))
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<ProductCardResponse> getNewArrivals(int limit) {
        User user = userService.getCurrentUserOrNull();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findByActiveTrueOrderByCreatedAtDesc(pageable)
                .map(p -> toCard(p, user))
                .getContent();
    }

    private void saveDetailImages(Product product, List<ProductDetailImageRequest> detailImages) {
        if (detailImages == null || detailImages.isEmpty()) return;

        for (ProductDetailImageRequest req : detailImages) {
            if (req == null) continue;
            if (req.getImageUrl() == null || req.getImageUrl().isBlank()) continue;

            ProductDetailImage img = new ProductDetailImage();
            img.setProduct(product);
            img.setImageUrl(req.getImageUrl());
            img.setSortOrder(req.getSortOrder());
            detailImageRepo.save(img);
        }
    }

    // ============== BATCH CARDS (NO N+1) ==============

    @Transactional(readOnly = true)
    public List<ProductCardResponse> toCardsPublic(List<Product> products, User user) {
        if (products == null || products.isEmpty()) return List.of();

        List<Long> ids = products.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .toList();

        // favorites set (1 query)
        Set<Long> favIds = new HashSet<>();
        if (user != null && !ids.isEmpty()) {
            favIds.addAll(favRepo.findFavoriteProductIds(user, ids));
        }

        // images (1 query)
        List<ProductImage> allImages = productImageRepo.findByProductIdInOrderByMainDescIdAsc(ids);
        Map<Long, List<ProductImageResponse>> imagesMap = new HashMap<>();
        Map<Long, ProductImageResponse> mainMap = new HashMap<>();

        for (ProductImage img : allImages) {
            Long pid = img.getProduct() != null ? img.getProduct().getId() : null;
            if (pid == null) continue;

            imagesMap.computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(new ProductImageResponse(img.getImageUrl(), img.isMain()));

            // first image is main due to ORDER BY main desc
            mainMap.putIfAbsent(pid, new ProductImageResponse(img.getImageUrl(), img.isMain()));
        }

        return products.stream()
                .map(p -> new ProductCardResponse(
                        p.getId(),
                        p.getName(),
                        p.getBrand(),
                        p.getPrice(),
                        p.getDiscountPrice(),
                        p.getCategory(),
                        p.getRatingAvg(),
                        p.getReviewCount(),
                        p.getSoldCount(),
                        p.isTodayDeal(),
                        user != null && favIds.contains(p.getId()),
                        p.getStock(),
                        mainMap.get(p.getId()),
                        imagesMap.getOrDefault(p.getId(), List.of())
                ))
                .toList();
    }

    /* ================= PROFESSIONAL MARKETPLACE SEARCH ================= */

    /**
     * ✅ PROFESSIONAL MARKETPLACE SEARCH
     *
     * Features:
     * - Multi-field search (name, description, search_text)
     * - Intelligent tier-based relevance ranking
     * - Stock-aware prioritization (in-stock first)
     * - Capped popularity boost (prevents dominance)
     * - Multi-language support (UZ/RU/EN via SearchNormalizer)
     * - Efficient pagination
     * - Search analytics logging
     *
     * Ranking Hierarchy:
     * 1. Exact name match (score ~2000)
     * 2. Name prefix match (score ~1000)
     * 3. Partial name match (score ~400)
     * 4. Description match (score ~100)
     * 5. search_text match (score ~50)
     *
     * @param q Raw user query (may contain typos, synonyms, UZ/RU/EN)
     * @param pageable Pagination parameters
     * @return Paginated search results sorted by relevance
     */
    @Transactional(readOnly = true)
    public Page<ProductCardResponse> search(String q, Pageable pageable) {

        User user = userService.getCurrentUserOrNull();

        // Normalize query for multi-language support
        // Handles: "kallagen" → "collagen", "атир" → "parfum", etc.
        String normalizedQuery = SearchNormalizer.normalize(q);

        // Use simple safe search (emergency fix - replaces risky marketplaceSearch)
        Page<Product> page = productRepo.simpleSearch(normalizedQuery, pageable);

        List<Product> products = page.getContent();

        // Batch convert to cards (no N+1 queries)
        List<ProductCardResponse> cards = toCardsPublic(products, user);

        // Log search analytics for business intelligence
        logSearchAnalytics(q, normalizedQuery, page.getTotalElements(), user);

        return new PageImpl<>(cards, pageable, page.getTotalElements());
    }

    /**
     * Log search analytics asynchronously
     * TODO: Move to @Async method in production for better performance
     */
    private void logSearchAnalytics(String originalQuery, String normalizedQuery, long resultCount, User user) {
        try {
            SearchLog log = new SearchLog();
            log.setKeyword(originalQuery);
            log.setNormalizedKeyword(normalizedQuery);
            log.setResultCount((int) resultCount);
            log.setUser(user);
            searchLogRepo.save(log);
        } catch (Exception e) {
            // Don't fail search if logging fails
            // In production: use proper async logging with error monitoring
            System.err.println("Search analytics logging failed: " + e.getMessage());
        }
    }

    /**
     * Map request data to Product entity
     * Includes professional search_text generation for multi-language search
     */
    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setBrand(req.getBrand());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        Category cat = Category.valueOf(req.getCategory().toUpperCase());
        p.setCategory(cat);

        // ✅ IMPROVED: Professional search_text generation
        // Uses SearchNormalizer.buildSearchText() for comprehensive coverage
        // Combines: name + brand + category + description
        // Result: raw text + normalized text (synonyms, transliterations)
        String searchText = SearchNormalizer.buildSearchText(
            req.getName(),           // e.g., "Collagen Serum"
            req.getBrand(),          // e.g., "AXIS-Y"
            cat.name(),              // e.g., "SERUM"
            req.getDescription(),    // e.g., "Anti-aging serum with marine collagen"
            null                     // Variant labels (can add if available)
        );

        p.setSearchText(searchText);

        // Example result:
        // "Collagen Serum AXIS-Y SERUM Anti-aging serum with marine collagen
        //  collagen serum axis y serum anti aging serum with marine collagen"
        // ↑ Original text preserves exact matches
        // ↑ Normalized text handles synonyms: "kallagen" → "collagen"
    }
}