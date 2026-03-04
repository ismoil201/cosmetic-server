package com.example.backend.service;

import com.example.backend.SearchNormalizer;
import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
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

    /* ================= SEARCH ================= */

    @Transactional
    public Page<ProductCardResponse> search(String q, Pageable pageable) {

        User user = userService.getCurrentUserOrNull();
        String normalizedQuery = SearchNormalizer.normalize(q);

        Page<Product> page = productRepo.fuzzySearch(normalizedQuery, pageable);

        List<Product> sortedProducts = page.getContent().stream()
                .sorted((a, b) -> {

                    String aText = nullSafe(a.getSearchText());
                    String bText = nullSafe(b.getSearchText());

                    int d1 = distance(aText, normalizedQuery);
                    int d2 = distance(bText, normalizedQuery);
                    if (d1 != d2) return Integer.compare(d1, d2);

                    boolean aBrand = a.getBrand() != null &&
                            (normalizedQuery.contains(a.getBrand().toLowerCase()) || aText.contains(a.getBrand().toLowerCase()));
                    boolean bBrand = b.getBrand() != null &&
                            (normalizedQuery.contains(b.getBrand().toLowerCase()) || bText.contains(b.getBrand().toLowerCase()));

                    if (aBrand != bBrand) return aBrand ? -1 : 1;

                    int scoreA = a.getSoldCount() * 3 + a.getViewCount();
                    int scoreB = b.getSoldCount() * 3 + b.getViewCount();
                    return Integer.compare(scoreB, scoreA);
                })
                .toList();

        List<ProductCardResponse> cards = toCardsPublic(sortedProducts, user);

        SearchLog log = new SearchLog();
        log.setKeyword(q);
        log.setNormalizedKeyword(normalizedQuery);
        log.setResultCount(cards.size());
        log.setUser(user);
        searchLogRepo.save(log);

        return new PageImpl<>(cards, pageable, page.getTotalElements());
    }

    private int distance(String text, String query) {
        if (text == null) text = "";
        if (query == null) query = "";
        text = text.trim().toLowerCase();
        query = query.trim().toLowerCase();

        if (text.isBlank() || query.isBlank()) return Integer.MAX_VALUE;

        String[] textTokens = text.split("\\s+");
        String[] qTokens = query.split("\\s+");

        int sum = 0;
        for (String qTok : qTokens) {
            int min = Integer.MAX_VALUE;
            for (String tTok : textTokens) {
                min = Math.min(min, levenshtein(tTok, qTok));
                if (min == 0) break;
            }
            sum += min;
        }
        return sum;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private int levenshtein(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(
                        1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1
                );
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setBrand(req.getBrand());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        Category cat = Category.valueOf(req.getCategory().toUpperCase());
        p.setCategory(cat);

        String base = req.getName() + " " + req.getBrand() + " " + cat.name();
        String normalized = SearchNormalizer.normalize(base);
        p.setSearchText((base + " " + normalized).toLowerCase());
    }
}