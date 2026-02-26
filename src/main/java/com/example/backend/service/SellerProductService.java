package com.example.backend.service;

import com.example.backend.SearchNormalizer;
import com.example.backend.dto.*;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductDetailImage;
import com.example.backend.entity.ProductImage;
import com.example.backend.entity.Seller;
import com.example.backend.repository.ProductDetailImageRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository productImageRepo;
    private final ProductDetailImageRepository detailImageRepo;

    private final SellerService sellerService;

    // =============== LIST (MY PRODUCTS) ===============
    @Transactional(readOnly = true)
    public Page<Product> myProducts(Pageable pageable) {
        Long sellerId = sellerService.requireCurrentSellerId();
        return productRepo.findBySellerIdAndActiveTrueOrderByCreatedAtDesc(sellerId, pageable);
    }

    // =============== DETAIL (MY PRODUCT) ===============
    @Transactional(readOnly = true)
    public Product myProductDetail(Long productId) {
        Long sellerId = sellerService.requireCurrentSellerId();
        return productRepo.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new RuntimeException("Product not found (or not yours)"));
    }

    // =============== CREATE ===============
    @Transactional
    public Long create(ProductCreateRequest req) {
        Seller seller = sellerService.requireCurrentSeller();

        Product product = new Product();
        map(req, product);

        // 🔥 marketplace ownership
        product.setSeller(seller);

        productRepo.save(product);

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());

        return product.getId();
    }

    // =============== UPDATE (ONLY MY PRODUCT) ===============
    @Transactional
    public void update(Long productId, ProductCreateRequest req) {
        Long sellerId = sellerService.requireCurrentSellerId();

        Product product = productRepo.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new AccessDeniedException("Bu product sizga tegishli emas"));

        map(req, product);
        productRepo.save(product);

        productImageRepo.deleteByProductId(product.getId());
        detailImageRepo.deleteByProductId(product.getId());

        saveImages(product, req.getImageUrls());
        saveDetailImages(product, req.getDetailImages());
    }

    // =============== DELETE (SOFT DELETE TAVSIYA) ===============
    @Transactional
    public void delete(Long productId) {
        Long sellerId = sellerService.requireCurrentSellerId();

        Product product = productRepo.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new AccessDeniedException("Bu product sizga tegishli emas"));

        // ✅ marketplace uchun soft delete yaxshi
        product.setActive(false);
        productRepo.save(product);

        // Agar siz hard delete qilmoqchi bo'lsangiz:
        // productImageRepo.deleteByProductId(productId);
        // detailImageRepo.deleteByProductId(productId);
        // productRepo.delete(product);
    }

    /* ================= HELPERS (ProductService’dan ko‘chirildi) ================= */

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

    private void saveDetailImages(Product product, List<ProductDetailImageRequest> detailImages) {
        if (detailImages == null || detailImages.isEmpty()) return;

        for (ProductDetailImageRequest req : detailImages) {
            ProductDetailImage img = new ProductDetailImage();
            img.setProduct(product);
            img.setImageUrl(req.getImageUrl());
            img.setSortOrder(req.getSortOrder());
            detailImageRepo.save(img);
        }
    }


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


    public SellerProductDetailResponse toDetailResponse(Product p) {

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


    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setBrand(req.getBrand());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        Category cat = Category.valueOf(req.getCategory().toUpperCase());
        p.setCategory(cat);

        // searchText (sizda SearchNormalizer bo'lsa)
        String base = req.getName() + " " + req.getBrand() + " " + cat.name();
        String normalized = SearchNormalizer.normalize(base);
        p.setSearchText((base + " " + normalized).toLowerCase());
    }
}