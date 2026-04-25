package com.example.backend.domain.seller.service;

import com.example.backend.global.util.SearchNormalizer;
import com.example.backend.domain.product.dto.*;
import com.example.backend.domain.product.entity.*;
import com.example.backend.domain.product.repository.*;
import com.example.backend.domain.seller.dto.SellerProductDetailResponse;
import com.example.backend.domain.seller.dto.SellerProductListResponse;
import com.example.backend.domain.seller.entity.Seller;
import com.example.backend.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SellerProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository productImageRepo;
    private final ProductDetailImageRepository detailImageRepo;
    private final ProductVariantRepository variantRepo;
    private final VariantTierPriceRepository tierRepo;
    private final SellerService sellerService;

    // ================= LIST (MY PRODUCTS - BATCH OPTIMIZED) =================
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<SellerProductListResponse> myProducts(Pageable pageable) {
        Long sellerId = sellerService.requireCurrentSellerId();
        Page<Product> productPage = productRepo.findBySellerIdAndActiveTrueOrderByCreatedAtDesc(sellerId, pageable);
        
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(
                    List.of(), 
                    productPage.getPageable(), 
                    productPage.getTotalElements()
            );
        }
        
        // ✅ Batch fetch main images in 1 query
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        
        List<ProductImage> allImages = productImageRepo.findByProductIdInOrderByMainDescIdAsc(productIds);
        java.util.Map<Long, String> mainImageMap = new java.util.HashMap<>();
        
        for (ProductImage img : allImages) {
            Long pid = img.getProduct() != null ? img.getProduct().getId() : null;
            if (pid != null && !mainImageMap.containsKey(pid)) {
                mainImageMap.put(pid, img.getImageUrl());
            }
        }
        
        // ✅ Build responses without additional queries
        List<SellerProductListResponse> responses = products.stream()
                .map(p -> new SellerProductListResponse(
                        p.getId(),
                        p.getName(),
                        p.getBrand(),
                        p.getPrice(),
                        p.getDiscountPrice(),
                        p.getCategory(),
                        p.getStock(),
                        p.getSoldCount(),
                        p.getRatingAvg(),
                        p.getReviewCount(),
                        p.isActive(),
                        mainImageMap.get(p.getId()),
                        p.getCreatedAt()
                ))
                .toList();
        
        return new org.springframework.data.domain.PageImpl<>(
                responses,
                productPage.getPageable(),
                productPage.getTotalElements()
        );
    }

    // ================= DETAIL (MY PRODUCT) =================
    @Transactional(readOnly = true)
    public Product myProductDetail(Long productId) {
        Long sellerId = sellerService.requireCurrentSellerId();

        // ✅ IDOR FIX: other seller producti "topilmadi" bo‘lib ko‘rinadi
        return productRepo.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    // ================= CREATE =================
    @Transactional
    public Long create(ProductCreateRequest req) {
        Seller seller = sellerService.requireCurrentSeller();

        Product product = new Product();
        map(req, product);

        // ✅ ownership
        product.setSeller(seller);

        product.setActive(false);
        productRepo.save(product);

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());
        saveVariantsFresh(product, req.getVariants()); // fresh save

        return product.getId();
    }

    // ================= UPDATE (ONLY MY PRODUCT) =================
    @Transactional
    public void update(Long productId, ProductCreateRequest req) {
        Seller seller = sellerService.requireCurrentSeller();

        // ✅ IDOR FIX: only owner seller can update
        Product product = productRepo.findByIdAndSellerId(productId, seller.getId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        map(req, product);

        // ownership must stay correct
        product.setSeller(seller);

        productRepo.save(product);

        // ✅ refresh images
        productImageRepo.deleteByProductId(product.getId());
        detailImageRepo.deleteByProductId(product.getId());

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());

        // ✅ refresh variants (IMPORTANT)
        replaceVariants(product, req.getVariants());
    }

    // ================= DELETE (SOFT DELETE) =================
    @Transactional
    public void delete(Long productId) {
        Long sellerId = sellerService.requireCurrentSellerId();

        Product product = productRepo.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        product.setActive(false); // ✅ soft delete
        productRepo.save(product);
    }

    /* ======================== MAPPERS ======================== */

    @Transactional(readOnly = true)
    public SellerProductListResponse toListResponse(Product p) {
        String mainImageUrl = productImageRepo
                .findFirstByProductIdAndMainTrue(p.getId())
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return new SellerProductListResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getCategory(),
                p.getStock(),
                p.getSoldCount(),
                p.getRatingAvg(),
                p.getReviewCount(),
                p.isActive(),
                mainImageUrl,
                p.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public SellerProductDetailResponse toDetailResponse(Product p) {

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

        // Seller panelda variantlarni ham ko‘rsatmoqchi bo‘lsangiz shu yerga qo‘shasiz.
        // Hozir sizning DTO'ingizda variants yo‘q, shuning uchun qo‘shmadim.

        return new SellerProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getCategory(),
                p.getStock(),
                p.getSoldCount(),
                p.getRatingAvg(),
                p.getReviewCount(),
                p.isActive(),
                images,
                detailImages,
                p.getCreatedAt()
        );
    }

    /* ======================== HELPERS ======================== */

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

    /**
     * Create uchun: fresh save.
     */
    private void saveVariantsFresh(Product product, List<ProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) return;

        for (ProductVariantRequest vReq : variants) {
            if (vReq == null) continue;

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setLabel(vReq.getLabel());
            variant.setPrice(vReq.getPrice());
            variant.setDiscountPrice(vReq.getDiscountPrice());
            variant.setStock(vReq.getStock());
            variant.setSortOrder(vReq.getSortOrder());
            variant.setActive(true);

            variantRepo.save(variant);

            if (vReq.getTiers() != null && !vReq.getTiers().isEmpty()) {
                for (VariantTierRequest tReq : vReq.getTiers()) {
                    if (tReq == null) continue;

                    VariantTierPrice tier = new VariantTierPrice();
                    tier.setVariant(variant);
                    tier.setMinQty(tReq.getMinQty());
                    tier.setTotalPrice(tReq.getTotalPrice());
                    tierRepo.save(tier);
                }
            }
        }
    }

    /**
     * Update uchun: old variants + tiers ni tozalab, yangisini qo‘yadi.
     * ✅ Bu bo‘lmasa update qilganda variantlar/tierlar ko‘payib ketadi.
     */
    private void replaceVariants(Product product, List<ProductVariantRequest> variants) {
        Long productId = product.getId();

        // 1) existing variantsni topamiz
        List<ProductVariant> existing = variantRepo.findByProductId(productId);
        if (existing != null && !existing.isEmpty()) {
            List<Long> variantIds = existing.stream()
                    .map(ProductVariant::getId)
                    .filter(Objects::nonNull)
                    .toList();

            // 2) tiersni o‘chiramiz
            if (!variantIds.isEmpty()) {
                tierRepo.deleteByVariantIdIn(variantIds);
            }

            // 3) variantsni o‘chiramiz
            variantRepo.deleteByProductId(productId);
        }

        // 4) yangidan saqlaymiz
        saveVariantsFresh(product, variants);
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