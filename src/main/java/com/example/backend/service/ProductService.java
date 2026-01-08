package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.repository.FavoriteRepository;
import com.example.backend.repository.ProductDetailImageRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final FavoriteRepository favRepo;
    private final UserService userService;
    private final ProductImageRepository productImageRepo;
    private final ProductDetailImageRepository detailImageRepo;


    /* ================= CREATE ================= */

    @Transactional
    public void create(ProductCreateRequest req) {

        Product product = new Product();
        map(req, product);
        productRepo.save(product);

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages()); // ✅ qo‘shildi

    }

    /* ================= DELETE ================= */

    @Transactional
    public void delete(Long id) {
        productImageRepo.deleteByProductId(id);
        detailImageRepo.deleteByProductId(id); // ✅ qo‘shildi

        productRepo.deleteById(id);
    }

    /* ================= HOME ================= */

    public Page<ProductResponse> getHomeProducts(Pageable pageable) {

        User user = userService.getCurrentUserOrNull();
        Page<Product> page = productRepo.findByActiveTrue(pageable);

        return page.map(product -> {

            boolean favorite = false;
            if (user != null) {
                favorite = favRepo.existsByUserAndProduct(user, product);
            }

            List<ProductImageResponse> images =
                    productImageRepo.findByProductId(product.getId())
                            .stream()
                            .map(img -> new ProductImageResponse(
                                    img.getImageUrl(),
                                    img.isMain()
                            ))
                            .toList();

            return new ProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getBrand(),
                    product.getPrice(),
                    product.getDiscountPrice(),
                    product.getCategory(),
                    product.getRatingAvg(),
                    product.getReviewCount(),
                    product.getSoldCount(),      // 🔥
                    product.isTodayDeal(),       // 🔥
                    favorite,
                    product.getStock(),
                    images
            );


        });
    }

    /* ================= DETAIL ================= */

    @Transactional
    public ProductDetailResponse getDetail(Long productId) {

        User user = userService.getCurrentUserOrNull();

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setViewCount(p.getViewCount() + 1);

        boolean favorite = false;
        if (user != null) {
            favorite = favRepo.existsByUserAndProduct(user, p);
        }

        List<ProductImageResponse> images =
                productImageRepo.findByProductId(p.getId())
                        .stream()
                        .map(img -> new ProductImageResponse(
                                img.getImageUrl(),
                                img.isMain()
                        ))
                        .toList();

        List<ProductDetailImageResponse> detailImages =
                detailImageRepo.findByProductIdOrderBySortOrderAsc(p.getId())
                        .stream()
                        .map(img -> new ProductDetailImageResponse(
                                img.getImageUrl(),
                                img.getSortOrder()
                        ))
                        .toList();

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
                p.getSoldCount(),      // 🔥
                p.isTodayDeal(),       // 🔥
                favorite,
                images,
                detailImages
        );


    }

    // TODAY DEAL BANNER
    public List<ProductResponse> getTodayDeals() {

        User user = userService.getCurrentUserOrNull();

        return productRepo.findByIsTodayDealTrueAndActiveTrue()
                .stream()
                .map(p -> {

                    boolean favorite = false;
                    if (user != null) {
                        favorite = favRepo.existsByUserAndProduct(user, p);
                    }

                    List<ProductImageResponse> images =
                            productImageRepo.findByProductId(p.getId())
                                    .stream()
                                    .map(img -> new ProductImageResponse(
                                            img.getImageUrl(),
                                            img.isMain()
                                    ))
                                    .toList();

                    return new ProductResponse(
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
                            images
                    );
                })
                .toList();
    }


    /* ================= UPDATE ================= */

    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        map(req, product);
        productRepo.save(product);

        productImageRepo.deleteByProductId(product.getId());
        detailImageRepo.deleteByProductId(product.getId()); // ✅ qo‘shildi

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());   // ✅ qo‘shildi
    }

    public Page<ProductCardResponse> getByCategoryCards(Category category, Pageable pageable) {
        User user = userService.getCurrentUserOrNull();
        return productRepo.findByCategoryAndActiveTrueOrderByCreatedAtDesc(category, pageable)
                .map(p -> toCardPublic(p, user));   // shu yerda mainImageUrl chiqadi
    }


    public List<ProductResponse> getProductsByIds(List<Long> ids) {

        User user = userService.getCurrentUserOrNull();

        return productRepo.findByIdInAndActiveTrue(ids)
                .stream()
                .map(p -> {

                    boolean favorite = false;
                    if (user != null) {
                        favorite = favRepo.existsByUserAndProduct(user, p);
                    }

                    List<ProductImageResponse> images =
                            productImageRepo.findByProductId(p.getId())
                                    .stream()
                                    .map(img -> new ProductImageResponse(
                                            img.getImageUrl(),
                                            img.isMain()
                                    ))
                                    .toList();

                    return new ProductResponse(
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
                            images
                    );
                })
                .toList();
    }

    /* ================= HELPERS ================= */

    private void saveImages(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl(imageUrls.get(i));
            img.setMain(i == 0);
            productImageRepo.save(img);
        }
    }

    private ProductCardResponse toCard(Product p, User user) {
        boolean favorite = (user != null) && favRepo.existsByUserAndProduct(user, p);

        // ✅ list (slider uchun)
        List<ProductImageResponse> images = productImageRepo
                .findByProductIdOrderByMainDescIdAsc(p.getId())
                .stream()
                .map(img -> new ProductImageResponse(img.getImageUrl(), img.isMain()))
                .toList();

        // ✅ main (fallback bilan)
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


    public Page<ProductCardResponse> getPopular(Pageable pageable) {
        User user = userService.getCurrentUserOrNull();
        return productRepo.findByActiveTrueOrderBySoldCountDesc(pageable)
                .map(p -> toCard(p, user));
    }

    public List<ProductCardResponse> getHits(int limit) {
        User user = userService.getCurrentUserOrNull();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findByIsTodayDealTrueAndActiveTrue(pageable)
                .map(p -> toCard(p, user))
                .getContent();
    }

    public List<ProductCardResponse> getDiscounts(int limit) {
        User user = userService.getCurrentUserOrNull();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findDiscounted(pageable)
                .map(p -> toCard(p, user))
                .getContent();
    }

    public List<ProductCardResponse> getNewArrivals(int limit) {
        User user = userService.getCurrentUserOrNull();
        Pageable pageable = PageRequest.of(0, limit);
        return productRepo.findByActiveTrueOrderByCreatedAtDesc(pageable)
                .map(p -> toCard(p, user))
                .getContent();
    }


    private void saveDetailImages(
            Product product,
            List<ProductDetailImageRequest> detailImages
    ) {
        if (detailImages == null || detailImages.isEmpty()) return;

        for (ProductDetailImageRequest req : detailImages) {
            ProductDetailImage img = new ProductDetailImage();
            img.setProduct(product);
            img.setImageUrl(req.getImageUrl());
            img.setSortOrder(req.getSortOrder());
            detailImageRepo.save(img);
        }
    }
    // ProductService ichida

    public ProductCardResponse toCardPublic(Product p, User user) {
        return toCard(p, user);
    }



    public Page<ProductCardResponse> search(String q, Pageable pageable) {

        User user = userService.getCurrentUserOrNull();
        String query = (q == null) ? "" : q.trim().toLowerCase();

        Page<Product> page = productRepo.fuzzySearch(query, pageable);

        List<Product> sortedProducts = page.getContent().stream()
                .sorted((a, b) -> {
                    int d1 = levenshtein(nullSafe(a.getSearchText()), query);
                    int d2 = levenshtein(nullSafe(b.getSearchText()), query);
                    if (d1 != d2) return Integer.compare(d1, d2);

                    int s1 = a.getSoldCount() * 3 + a.getViewCount();
                    int s2 = b.getSoldCount() * 3 + b.getViewCount();
                    return Integer.compare(s2, s1);
                })
                .toList();

        List<ProductCardResponse> cards = sortedProducts.stream()
                .map(p -> toCardPublic(p, user))
                .toList();

        return new PageImpl<>(cards, pageable, page.getTotalElements());
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

        // 🔥 MUSINSA STYLE SEARCH TEXT
        p.setSearchText(
                (req.getName() + " " +
                        req.getBrand() + " " +
                        cat.name())
                        .toLowerCase()
        );
    }

}
