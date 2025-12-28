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

    // ================= ADMIN LIST =================
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
                p.getSoldCount()
        ));

    }

    // ================= CREATE =================
    @Transactional
    public void create(ProductCreateRequest req) {

        Product product = new Product();
        map(req, product);
        productRepo.save(product);

        saveImages(product, req);
    }

    // ================= UPDATE =================
    @Transactional
    public void update(Long id, ProductCreateRequest req) {

        Product product = productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        map(req, product);
        productRepo.save(product);

        productImageRepo.deleteByProductId(product.getId());
        saveImages(product, req);
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

    private void map(ProductCreateRequest req, Product p) {
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setBrand(req.getBrand());
        p.setPrice(req.getPrice());
        p.setDiscountPrice(req.getDiscountPrice());
        p.setStock(req.getStock());

        p.setCategory(Category.valueOf(req.getCategory().toUpperCase()));
    }
}
