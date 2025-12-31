package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductDetailImage;
import com.example.backend.entity.ProductImage;
import com.example.backend.repository.ProductDetailImageRepository;
import com.example.backend.repository.ProductImageRepository;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepo;
    private final ProductImageRepository productImageRepo;
    private final ProductDetailImageRepository detailImageRepo;


    // ================= ADMIN LIST =================
    public Page<AdminProductResponse> list(
            Boolean active,
            String category,
            String brand,
            Boolean todayDeal,
            String keyword,
            Pageable pageable
    ) {

        Category cat = null;
        if (category != null) {
            try {
                cat = Category.valueOf(category.toUpperCase());
            } catch (Exception e) {
                throw new RuntimeException("Invalid category");
            }
        }

        return productRepo.adminSearch(
                active, cat, brand, todayDeal, keyword, pageable
        ).map(p -> new AdminProductResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getStock(),
                p.isActive(),
                p.getCategory(),
                p.isTodayDeal(),
                p.getSoldCount()
        ));
    }

    public AdminProductDetailResponse detail(Long id) {

        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        var imageUrls = productImageRepo.findByProductId(p.getId())
                .stream()
                .map(ProductImage::getImageUrl)
                .toList();

        var detailImages = detailImageRepo
                .findByProductIdOrderBySortOrderAsc(p.getId())
                .stream()
                .map(d -> new ProductDetailImageResponse(
                        d.getImageUrl(),
                        d.getSortOrder()
                ))
                .toList();

        return new AdminProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getCategory(),
                p.getStock(),
                p.isActive(),
                p.isTodayDeal(),
                imageUrls,
                detailImages
        );
    }


    // ================= CREATE =================
    @Transactional
    public void create(ProductCreateRequest req) {
        validate(req);

        Product product = new Product();
        map(req, product);
        productRepo.save(product);

        saveImages(product, req);
        saveDetailImages(product, req); // 🔥 YANGI

    }

    // ================= UPDATE =================
    @Transactional
    public void update(Long id, ProductCreateRequest req) {
        validate(req);

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        map(req, product);
        productRepo.save(product);

        productImageRepo.deleteByProductId(product.getId());
        detailImageRepo.deleteByProductId(product.getId()); // 🔥

        saveImages(product, req);
        saveDetailImages(product, req); // 🔥

    }

    // ================= SOFT DELETE =================
    public void softDelete(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(false);
        productRepo.save(p);
    }

    // ================= RESTORE =================
    public void restore(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(true);
        productRepo.save(p);
    }

    // ================= TODAY DEAL (BITTA) =================
    @Transactional
    public void setSingleTodayDeal(Long productId) {

        // 🔥 Avval eski today deal’larni o‘chir
        productRepo.clearTodayDeals();

        // 🔥 Yangi today deal
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setTodayDeal(true);
        productRepo.save(product);
    }


    // ================= HELPERS =================
    private void saveImages(Product product, ProductCreateRequest req) {
        if (req.getImageUrls() == null) return;

        for (int i = 0; i < req.getImageUrls().size(); i++) {
            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setImageUrl(req.getImageUrls().get(i));
            img.setMain(i == 0);
            productImageRepo.save(img);
        }
    }
    private void saveDetailImages(Product product, ProductCreateRequest req) {

        if (req.getDetailImages() == null) return;

        for (int i = 0; i < req.getDetailImages().size(); i++) {

            ProductDetailImageRequest d = req.getDetailImages().get(i);

            ProductDetailImage img = new ProductDetailImage();
            img.setProduct(product);
            img.setImageUrl(d.getImageUrl());

            int order = d.getSortOrder() > 0 ? d.getSortOrder() : i + 1;
            img.setSortOrder(order);

            detailImageRepo.save(img);
        }
    }



    @Transactional
    public void setActive(Long id, boolean active) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setActive(active);
        productRepo.save(product);
    }


    @Transactional
    public void toggleActive(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(!p.isActive());
        productRepo.save(p);
    }


    private void validate(ProductCreateRequest req) {

        if (req.getName() == null || req.getName().isBlank())
            throw new RuntimeException("Product name is required");

        if (req.getPrice() == null || req.getPrice().signum() <= 0)
            throw new RuntimeException("Price must be greater than 0");

        if (req.getDiscountPrice() != null && req.getDiscountPrice().signum() < 0)
            throw new RuntimeException("Discount price must be >= 0");

        if (req.getDiscountPrice() != null
                && req.getPrice() != null
                && req.getDiscountPrice().compareTo(req.getPrice()) > 0)
            throw new RuntimeException("Discount price cannot be greater than price");

        if (req.getStock() < 0)
            throw new RuntimeException("Stock must be >= 0");

        if (req.getCategory() == null)
            throw new RuntimeException("Category is required");
    }



    private void map(ProductCreateRequest req, Product p) {

        validate(req);

        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        try {
            p.setCategory(Category.valueOf(req.getCategory().toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid category: " + req.getCategory());
        }
    }

}
