package com.example.backend.service;

import com.example.backend.dto.AdminProductResponse;
import com.example.backend.dto.ProductCreateRequest;
import com.example.backend.entity.Category;
import com.example.backend.entity.Product;
import com.example.backend.entity.ProductImage;
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

    // =========================
    // ✅ ADMIN PRODUCT LIST
    // =========================
    public Page<AdminProductResponse> list(Boolean active, Pageable pageable) {

        Page<Product> page = (active == null)
                ? productRepo.findAll(pageable)
                : productRepo.findByActive(active, pageable);

        return page.map(p -> new AdminProductResponse(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getStock(),
                p.isActive(),
                p.getCategory(),
                p.isTodayDeal(),
                p.getSoldCount(),
                p.getCreatedAt()
        ));
    }

    // =========================
    // ✅ ADMIN CREATE PRODUCT
    // =========================
    @Transactional
    public void create(ProductCreateRequest req) {

        Product product = new Product();
        map(req, product);

        productRepo.save(product);

        // 🔥 RASMLARNI SAQLASH
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setMain(i == 0); // 1-rasm MAIN
                productImageRepo.save(img);
            }
        }
    }

    // =========================
    // ✅ ADMIN UPDATE PRODUCT
    // =========================
    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        map(req, product);
        productRepo.save(product);

        // ❌ ESKI RASMLARNI O‘CHIRAMIZ
        productImageRepo.deleteByProductId(product.getId());

        // ✅ YANGI RASMLAR
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product);
                img.setImageUrl(req.getImageUrls().get(i));
                img.setMain(i == 0);
                productImageRepo.save(img);
            }
        }
    }

    // =========================
    // ✅ ADMIN SOFT DELETE
    // =========================
    public void softDelete(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(false);
        productRepo.save(p);
    }

    // =========================
    // ✅ ADMIN RESTORE
    // =========================
    public void restore(Long id) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        p.setActive(true);
        productRepo.save(p);
    }

    // =========================
    // 🔥 ADMIN – TODAY DEAL
    // =========================
    @Transactional
    public void setTodayDeal(Long productId, boolean value) {

        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setTodayDeal(value);
        productRepo.save(p);
    }

    // =========================
    // 🔥 FAQAT BITTA TODAY DEAL (OPTIONAL)
    // =========================
    @Transactional
    public void setSingleTodayDeal(Long productId) {

        // Avval hammasini o‘chiramiz
        productRepo.clearTodayDeals();

        // Bitta product’ni yoqamiz
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        p.setTodayDeal(true);
        productRepo.save(p);
    }

    // =========================
    // 🔧 MAP REQUEST → ENTITY
    // =========================
    private void map(ProductCreateRequest req, Product p) {

        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        try {
            p.setCategory(Category.valueOf(req.getCategory().toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid category");
        }
    }
}
